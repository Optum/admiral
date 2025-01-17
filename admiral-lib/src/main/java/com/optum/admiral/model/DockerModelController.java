package com.optum.admiral.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.core.command.CreateContainerCmdImpl;
import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.config.ActionMonitor;
import com.optum.admiral.config.AdmiralContainerConfig;
import com.optum.admiral.config.AdmiralNetworkConfig;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.event.AdmiralEventListener;
import com.optum.admiral.event.AdmiralEventPublisher;
import com.optum.admiral.event.ExecuteHookListener;
import com.optum.admiral.event.HealthCheckListener;
import com.optum.admiral.event.StartWaitListener;
import com.optum.admiral.exception.AdmiralContainerNotFoundException;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.exception.AdmiralImageNotFoundException;
import com.optum.admiral.exception.AdmiralInvalidReferenceFormatException;
import com.optum.admiral.exception.AdmiralMountsDeniedException;
import com.optum.admiral.exception.AdmiralNetworkNotFoundException;
import com.optum.admiral.exception.AdmiralNetworkHasActiveEndpointsException;
import com.optum.admiral.io.ContainerLogStreamer;
import com.optum.admiral.io.FileLogStreamer;
import com.optum.admiral.key.LogStreamerKey;
import com.optum.admiral.model.dockerjava.DockerJavaDockerModelImpl;
import com.optum.admiral.type.CopyHook;
import com.optum.admiral.type.Dependant;
import com.optum.admiral.type.Duration;
import com.optum.admiral.type.LogMonitor;
import com.optum.admiral.type.PortMap;
import com.optum.admiral.type.Volume;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * =-- DMC --=
 *
 * This class is supposed to contain all the mechanism for controlling
 * everything in the Admiral Docker Model.  It expressly doesn't know anything about
 * policy or why something happens, it just knows how to make something happen.
 *
 * It also expressly doesn't know about any data structures outside the model: it
 * can't parse YAML, it can't decode docker-compose short format syntax, it
 * has no external dependencies except the Docker-Java API.  Conversely, it
 * is expected (but not enforced) that probably nothing other than things in
 * this com.optum.admiral.model package should know about Docker-Java.
 *
 * To further isolate Admiral code from library dependencies, the DockerJavaDockerModelImpl creates the bullet-proof
 * firewall layer around the library.  It is super-strict mechanism only, and basically only does type conversions.  In
 * that way, Docker-Java nested objects get unraveled and Docker-Java exceptions get caught, parsed, and rethrown to
 * Admiral's liking.
 *
 * There is a fine semantic line between Container and Service.
 * DMC controls containers but doesn't know about services.
 * The thinking is that the service-to-container boundary is governed by
 * a little bit of trivial mechanism (which is easy to push up) and a bunch
 * of policy (which doesn't belong here).
 *
 * So don't look here for stopService or rollingBounceService, all DMC does is
 * control containers.  Our caller (Admiral) needs to implement the service methods.
 */
public class DockerModelController implements AdmiralEventPublisher, Debugger, ExecuteHookListener, HealthCheckListener, StartWaitListener {
    /**
     * Services have "replicas"-many containers.  Admiral does not currently support up/down scaling of replicas.
     * So the creation of the containerSet need only be done once.  If up/down scaling is later implemented, the
     * containerSet will need to be rebuilt with each scaling event.
     */
    private final Map<String, ContainerController> containerSet =  new HashMap<>();

    private final AdmiralEventPublisher admiralEventPublisher;

    private final AdmiralOptions admiralOptions;

    private final ComposeConfig composeConfig;

    private final ThreadGroup logStreamerThreadGroup;

    public DockerModelController(AdmiralEventPublisher admiralEventPublisher, ComposeConfig composeConfig, AdmiralOptions admiralOptions) {
        this.admiralEventPublisher = admiralEventPublisher;
        this.composeConfig = composeConfig;
        this.admiralOptions = admiralOptions;
        this.logStreamerThreadGroup = new ThreadGroup("LogStreamers");
        rebuildContainerSet(composeConfig);
    }

    /**
     * Convenience delegate method.
     */
    @Override
    public void publish(Consumer<AdmiralEventListener> event) {
        admiralEventPublisher.publish(event);
    }

    private DockerJavaDockerModelImpl dockerImpl = new DockerJavaDockerModelImpl(this, this);

    public void setProgressHandler(ProgressHandler progressHandler) {
        dockerImpl.setProgressHandler(progressHandler);
    }

    public boolean isConnected() {
        return dockerImpl.isConnected();
    }

    public void createNetworks(Collection<AdmiralNetworkConfig> networkConfigs) throws AdmiralDockerException {
        for(AdmiralNetworkConfig admiralNetworkConfig : networkConfigs) {
            if (!admiralNetworkConfig.isExternal()) {
                final String dockerNetworkName = admiralNetworkConfig.getDockerNetworkName();

                List<Network> existingNetworks = dockerImpl.listNetworks(dockerNetworkName);
                if (existingNetworks.isEmpty()) {
                    publish(l -> l.networkCreating(dockerNetworkName));
                    CreateNetworkResponse createNetworkResponse = dockerImpl.createNetwork(dockerNetworkName, admiralNetworkConfig.getLabels());
                    publish(l -> l.networkCreated(dockerNetworkName, createNetworkResponse, false));
                } else {
                    publish(l -> l.networkCreated(dockerNetworkName, null, true));
                }
            }
        }
    }

    public void removeNetworks(Collection<AdmiralNetworkConfig> networkConfigs) throws AdmiralDockerException {
        for(AdmiralNetworkConfig admiralNetworkConfig : networkConfigs) {
            if (!admiralNetworkConfig.isExternal()) {
                boolean skipped = false;
                boolean failed = false;
                final String dockerNetworkName = admiralNetworkConfig.getDockerNetworkName();
                publish(l -> l.networkRemoving(dockerNetworkName));
                try {
                    dockerImpl.removeNetwork(dockerNetworkName);
                } catch (AdmiralNetworkNotFoundException e) {
                    skipped = true;
                } catch (AdmiralNetworkHasActiveEndpointsException e) {
                    failed = true;
                    publish(l -> l.networkRemoveFailed(dockerNetworkName, e));
                }
                if (!failed) {
                    final boolean finalSkipped = skipped;
                    publish(l -> l.networkRemoved(dockerNetworkName, finalSkipped));
                }
            }
        }
    }

    public void connectIfNecessary() throws AdmiralDockerException {
        if (dockerImpl.connectIfNecessary()) {
            publish(l -> l.dockerEngineConnected());
        }
    }

    public void disconnectFromDockerEngine() throws AdmiralDockerException {
        dockerImpl.disconnect();
        publish(l -> l.dockerEngineDisconnected());
    }

    private static final String STDOUT = "stdout";
    private static final String STDERR = "stderr";
    public void attachToContainer(AdmiralServiceConfig admiralServiceConfig, String containerName)
            throws AdmiralDockerException {
        publish(l -> l.debugAttachToContainerBegin(containerName, STDOUT));
        attachToContainer(containerName, "A", STDOUT, admiralServiceConfig.getActionMonitors(), false);
        publish(l -> l.debugAttachToContainerEnd(containerName, STDOUT));
        publish(l -> l.debugAttachToContainerBegin(containerName, STDERR));
        attachToContainer(containerName, "B", STDERR, admiralServiceConfig.getActionMonitors(), true);
        publish(l -> l.debugAttachToContainerEnd(containerName, STDERR));
    }

    private void attachToContainer(String containerName, String groupName, String streamName, List<ActionMonitor> actionMonitors, boolean stderr)
            throws AdmiralDockerException {
        // Gather
        final ContainerLogStreamer containerLogStreamer = new ContainerLogStreamer(containerName, groupName, streamName, actionMonitors);
        final LogStreamerKey logStreamerKey = containerLogStreamer.getPrimaryKey();

        final ContainerController containerController = getContainer(containerName);
        boolean alreadyHaveOne = containerController.addStreamer(logStreamerKey, containerLogStreamer);
        // Guard.  If we are already attached, exit.
        if (alreadyHaveOne)
            return;

        dockerImpl.attachToContainer(containerName, stderr, containerLogStreamer);

        // Notify
        publish(l -> l.containerAttachedToStream(containerLogStreamer));
    }

    public void deleteFile(File file) {
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                // Notify
                publish(l -> l.deletedFile(file));
            } else {
                publish(l -> l.warning("File " + file + " was not deleted."));
            }
        }
    }

    public void attachToLogs(AdmiralServiceConfig admiralServiceConfig, String containerName, boolean reconnecting) {
        for (LogMonitor logMonitor : admiralServiceConfig.getLogMonitors()) {
            // Gather
            publish(l -> l.debugAttachToLogBegin(containerName, logMonitor.filename));
            FileLogStreamer fileLogStreamer = new FileLogStreamer(containerName, logMonitor);
            final LogStreamerKey logStreamerKey = fileLogStreamer.getPrimaryKey();

            ContainerController containerController = getContainer(containerName);
            boolean alreadyHaveOne = containerController.addStreamer(logStreamerKey, fileLogStreamer);
            // Guard.  If we are already attached, exit.
            if (alreadyHaveOne)
                return;

            // Go
            fileLogStreamer.start(logStreamerThreadGroup, reconnecting, this);
            publish(l -> l.debugAttachToLogEnd(containerName, logMonitor.filename));

            // Notify
            publish(l -> l.containerAttachedToStream(fileLogStreamer));
        }
    }

    public void detatchFromContainer(String containerName) {
        ContainerController containerController = containerSet.get(containerName);
        containerController.detatch();
    }

    public boolean isContainerRunning(String containerName) throws AdmiralDockerException {
        try {
            final InspectContainerResponse inspectContainerResponse = dockerImpl.inspectContainer(containerName);
            return inspectContainerResponse.getState().getRunning();
        } catch (AdmiralContainerNotFoundException e) {
            return false;
        }
    }

    /**
     * Loops through all the possible container names (based on replicas) and if ANY container is
     * running for this service, returns true; otherwise (no container running) returns false.
     */
    public boolean isServiceRunningOrAssumed(String serviceName) throws AdmiralDockerException {
        AdmiralServiceConfig admiralServiceConfig = composeConfig.getServiceConfig(serviceName);

        if (admiralServiceConfig.shouldAssume()) {
            if (admiralServiceConfig.shouldAssumeRunning()) {
                return true;
            } else {
                return false;
            }
        }

        final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
        for (int i = 1; i <= replicas; i++) {
            final String containerName = composeConfig.calculateContainerName(serviceName, i);
            final boolean running = isContainerRunning(containerName);
            if (running) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loops through all the possible container names (based on replicas) and if ANY container is
     * running for this service, returns true; otherwise (no container running) returns false.
     */
    public boolean isServiceCreated(String serviceName) throws AdmiralDockerException {
        AdmiralServiceConfig admiralServiceConfig = composeConfig.getServiceConfig(serviceName);
        final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
        for (int i = 1; i <= replicas; i++) {
            final String containerName = composeConfig.calculateContainerName(serviceName, i);
            try {
                dockerImpl.inspectContainer(containerName);
                return true;
            } catch (AdmiralContainerNotFoundException e) {
                // Do Nothing
            }
        }
        return false;
    }

    public ContainerController getContainer(String containerName) {
        return containerSet.get(containerName);
    }

    public void shutdownLogStreamers() {
        for(ContainerController containerController : containerSet.values()) {
            containerController.shutdown();
        }
    }

    private void rebuildContainerSet(ComposeConfig composeConfig) {
        containerSet.clear();
        for(AdmiralServiceConfig admiralServiceConfig : composeConfig.getServicesOrEmpty()) {
            final String name = admiralServiceConfig.getName();
            final Service service = new Service(name, admiralServiceConfig);
            final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
            for (int i = 1; i <= replicas; i++) {
                final String containerName = composeConfig.calculateContainerName(name, i);
                ContainerController containerController = new ContainerController(containerName, service);
                containerSet.put(containerName, containerController);
            }
        }
    }

    public void startServicesInDependencyOrder(Collection<AdmiralServiceConfig> services)
            throws AdmiralDockerException, InterruptedException {
        Set<String> startedServiceNames = new HashSet<>();
        Set<AdmiralServiceConfig> toStart = new HashSet<>(services);

        for(AdmiralServiceConfig admiralServiceConfig : composeConfig.getServicesOrEmpty()) {
            if (isServiceRunningOrAssumed(admiralServiceConfig.getName())) {
                startedServiceNames.add(admiralServiceConfig.getName());
            }
        }

        boolean keepTrying = true;
        while (keepTrying && (!toStart.isEmpty())) {
            for (AdmiralServiceConfig admiralServiceConfig : services) {
                if (toStart.contains(admiralServiceConfig)) {
                    List<String> notIn = admiralServiceConfig.dependsOnAllIn(startedServiceNames);
                    if (notIn.isEmpty()) {
                        startService(admiralServiceConfig);
                        startedServiceNames.add(admiralServiceConfig.getName());
                        toStart.remove(admiralServiceConfig);
                    } else {
                        ArrayList<String> wontStart = new ArrayList<>();
                        wontStart.addAll(notIn);
                        for(AdmiralServiceConfig toDo : toStart) {
                            wontStart.remove(toDo.getName());
                        }
                        if (wontStart.isEmpty()) {
                            publish(l -> l.warning("Can't start " + admiralServiceConfig.getName() + " yet.  Waiting on: " + String.join(", ", notIn)));
                        } else {
                            publish(l -> l.error("Unable to start " + admiralServiceConfig.getName() + " because the following depends_on services are not running: " + String.join(", ", wontStart)));
                            keepTrying = false;
                        }
                    }
                }
            }
            try {
                // TODO - Sleeps are evil.  Static, no back-off sleeps are more evil.
                if (keepTrying)
                    Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("SS: " + e.getMessage());
                keepTrying = false;
            }
        }
    }


    public void startService(AdmiralServiceConfig admiralServiceConfig)
            throws AdmiralDockerException, InterruptedException {
        // Gather
        final String serviceName = admiralServiceConfig.getName();

        // Notify
        publish(l -> l.serviceStarting(serviceName));

        // Go
        final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
        for (int i = 1; i <= replicas; i++) {
            final String containerName = composeConfig.calculateContainerName(serviceName, i);
            final boolean running = isContainerRunning(containerName);
            if (!running) {
                attachToContainer(admiralServiceConfig, containerName);
                attachToLogs(admiralServiceConfig, containerName, false);
                startContainer(admiralServiceConfig, containerName);
            } else {
                // This is a skipped
                publish(l -> l.containerStarted(containerName, true));
            }
        }

        publish(l -> l.serviceStarted(serviceName));
    }

    // TODO: This implementation is blind to the actual state of containers.
    public void stopServices(Collection<AdmiralServiceConfig> services)
            throws AdmiralDockerException {
        Set<String> stopped = new HashSet<>();
        Set<String> toStop = new HashSet<>();
        for (AdmiralServiceConfig admiralServiceConfig : services) {
            toStop.add(admiralServiceConfig.getName());
        }

        boolean keepTrying = true;
        while (keepTrying && (!toStop.isEmpty())) {
            for (AdmiralServiceConfig admiralServiceConfig : services) {
                final String serviceName = admiralServiceConfig.getName();
                if (toStop.contains(serviceName) && nooneDependsOnMe(stopped, services, serviceName)) {
                    stopService(admiralServiceConfig);
                    stopped.add(serviceName);
                    toStop.remove(serviceName);
                } else {
                    publish(l -> l.warning("Can't stop " + serviceName + " yet."));
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                keepTrying = false;
            }
        }
    }

    private boolean nooneDependsOnMe(Set<String> stopped, Collection<AdmiralServiceConfig> services, String me) {
        for (AdmiralServiceConfig admiralServiceConfig : services) {
            final String serviceName = admiralServiceConfig.getName();
            for(Dependant dependant : admiralServiceConfig.getDependsOn()) {
                if (dependant.getServiceName().equals(me) && (!stopped.contains(serviceName))) {
                    // And that service isn't stopped.
                    // So I can't stop.
                    return false;
                }
            }
        }
        return true;
    }

    private void stopService(AdmiralServiceConfig admiralServiceConfig)
            throws AdmiralDockerException {
        // Gather
        final String serviceName = admiralServiceConfig.getName();

        publish(l -> l.serviceStopping(serviceName));

        final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();

        for (int i = 1; i <= replicas; i++) {
            final String containerName = composeConfig.calculateContainerName(serviceName, i);
            stopContainer(containerName, admiralServiceConfig.getStopGracePeriod());
        }
        publish(l -> l.serviceStopped(serviceName));
    }

    public void rmServices(Collection<AdmiralServiceConfig> services)
            throws AdmiralDockerException, InterruptedException {
        for (AdmiralServiceConfig admiralServiceConfig : services) {
            rmService(admiralServiceConfig);
        }
    }

    private void rmService(AdmiralServiceConfig admiralServiceConfig)
            throws AdmiralDockerException {
        final String serviceName = admiralServiceConfig.getName();

        publish(l -> l.serviceRemoving(serviceName));

        final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();

        for (int i = 1; i <= replicas; i++) {
            final String containerName = composeConfig.calculateContainerName(serviceName, i);
            removeContainer(containerName);
        }

        publish(l -> l.serviceRemoved(serviceName));
    }

    /**
     * Convenience Method
     */
    public String matchAdmiralContainerName(Container container) {
        for (String name : containerSet.keySet()) {
            for(String n : container.getNames()) {
                if (n.equals("/" + name)) {
                    return name;
                }
            }
        }
        return null;
    }

    /**
     * HealthChecks (the docker-supported method) must occur before PostStartWaits.
     * The reasoning is that PostStartWaits wait for things that are more difficult to check than HealthChecks, and
     * that the HealthChecks return true but the PostStartWaits still returns false.  So with that theory, you should
     * process all the HealthChecks first, then wait for the PostStartWaits that are intended to wait a little longer.
     *
     * If the user in the shell types "wait" we wait (userSpecificallyAskedToWait==true) even if the waitForContainersWhenStarting is false.
     */
    public boolean containerWait(AdmiralContainerConfig admiralContainerConfig, String containerName, boolean userSpecificallyAskedToWait) {
        // HealthChecks are mandatory since they are docker specifications.
        HealthCheck composeHealthCheck = admiralContainerConfig.getComposeHealthCheck();
        if (composeHealthCheck != null) {
            if (!composeHealthCheck.executeHealthCheck(this, this, containerName, userSpecificallyAskedToWait)) {
                publish(l -> l.warning("Healthcheck expired.  Container " + containerName + " has not responded within "
                        + composeHealthCheck.retries + " retries."));
                return false;
            }
        }

        // PostStartWaits are optional since they are Admiral specifications.  If you don't want to wait, we won't wait.
        if (userSpecificallyAskedToWait || admiralOptions.waitForContainersWhenStarting) {
            for(HealthCheck healthCheck : admiralContainerConfig.getAdmiralHealthChecks()) {
                if (!healthCheck.executeHealthCheck(this, this, containerName, userSpecificallyAskedToWait)) {
                    publish(l -> l.warning("Healthcheck expired.  Container " + containerName + " has not responded within "
                            + healthCheck.retries + " retries."));
                    return false;
                }
            }

            for (StartWait startWait : admiralContainerConfig.getPostStartWaits()) {
                if (!startWait.waitForIt(this)) {
                    publish(l -> l.warning("Wait expired.  Container " + containerName + " has not responded within "
                            + startWait.getSeconds() + " seconds."));
                    return false;
                }
            }
        }

        return true;
    }

    public boolean startContainer(AdmiralContainerConfig admiralContainerConfig, String containerName)
            throws AdmiralDockerException {
        try {
            publish(l -> l.containerStarting(this, containerName, admiralContainerConfig));

            dockerImpl.startContainer(containerName);
        } catch (AdmiralMountsDeniedException e) {
            publish(l -> l.containerStartingFailed(containerName));
            publish(l -> l.containerMountDenied(containerName, admiralContainerConfig.getName(), e.getMountName()));
            return false;
        } catch (AdmiralContainerNotFoundException e) {
            publish(l -> l.containerStartingFailed(containerName));
            publish(l -> l.containerNotFound(containerName, false));
            return false;
        }
        publish(l -> l.containerStarted(containerName, false));

        return containerWait(admiralContainerConfig, containerName, false);
    }

    public void publishUnhandledException(Throwable cause) {
        publish(l -> l.unhandledException(cause));
    }

    public void removeContainer(String containerName) throws AdmiralDockerException {
        try {
            publish(l -> l.containerRemoving(containerName));

            final boolean running = isContainerRunning(containerName);
            if (!running) {
                dockerImpl.removeContainer(containerName);
                publish(l -> l.containerRemoved(containerName, false));
            } else {
                publish(l -> l.containerRemoved(containerName, true));
            }
        } catch (AdmiralContainerNotFoundException e) {
            publish(l -> l.containerRemoved(containerName, true));
        }
    }

    private static String dockerJavaDetectImagePullNoSuchHost(String original) {
        Pattern pattern = Pattern.compile("Get \"https://(.+)/v2/\": dialing (.+):443 static system has no HTTPS proxy: resolving host (.+): lookup (.+): no such host");
        Matcher matcher = pattern.matcher(original);
        if (matcher.find()) {
            if (matcher.groupCount()==4) {
                String host1 = matcher.group(1);
                String host2 = matcher.group(2);
                String host3 = matcher.group(3);
                String host4 = matcher.group(4);
                if (host1.equals(host2) && host2.equals(host3) && host3.equals(host4) && host4.equals(host1)) {
                    return host1;
                }
            }
        }
        return null;
    }

    private static String dockerJavaDetectImageNotFound(String original) {
        Pattern pattern = Pattern.compile("^pull access denied for (.+), repository does not exist or may require 'docker login': denied: requested access to the resource is denied$");
        Matcher matcher = pattern.matcher(original);
        if (matcher.find()) {
            if (matcher.groupCount()==1) {
                String imageName = matcher.group(1);
                return imageName;
            }
        }
        return null;
    }

    private static String dockerJavaDetectManifestNotFound(String original) {
        Pattern pattern = Pattern.compile("^manifest for (.+) not found: manifest unknown: manifest unknown$");
        Matcher matcher = pattern.matcher(original);
        if (matcher.find()) {
            if (matcher.groupCount()==1) {
                String imageName = matcher.group(1);
                return imageName;
            }
        }
        return null;
    }

    private static boolean dockerJavaDetectAccessDenied(String original) {
        Pattern pattern = Pattern.compile("^pull access denied for invalid/image/host, repository does not exist or may require 'docker login': denied: requested access to the resource is denied$");
        Matcher matcher = pattern.matcher(original);
        return matcher.find();
    }

    public boolean containerExists(String containerName) throws AdmiralDockerException {
        try {
            dockerImpl.inspectContainer(containerName);
            return true;
        } catch (AdmiralContainerNotFoundException e) {
            return false;
        }
    }

    private HostConfig createContainer_HostConfig(AdmiralContainerConfig admiralContainerConfig) {
        HostConfig hostConfig = new HostConfig();

        List<Link> containList = new ArrayList<>();
        for (Dependant depService : admiralContainerConfig.getDependsOn()) {
            final String serviceName = depService.getServiceName();
            final String depContainer = composeConfig.calculateContainerName(serviceName, 1);
            Link link = new Link(depContainer, serviceName);
            containList.add(link);
        }

        hostConfig.withLinks(containList);
        hostConfig.withPublishAllPorts(false);

        List<PortBinding> ports = new ArrayList<>();
        for(PortMap portMap : admiralContainerConfig.getPortMaps()) {
            Ports.Binding binding = Ports.Binding.bindIpAndPort(portMap.getHost(), portMap.getPublished());
            ports.add(new PortBinding(binding, convert(portMap)));
        }
        hostConfig.withPortBindings(ports);

        hostConfig.withExtraHosts(admiralContainerConfig.getExtraHosts().toArray(new String[0]));
//        hostConfig.withVolumesFrom()
//        hostConfigBuilder.volumesFrom(new ArrayList(admiralContainerConfig.getVolumesFrom()));

        List<Bind> bindList = new ArrayList<>();
        for (Volume v : admiralContainerConfig.getVolumes()) {
            Bind bind = new Bind(v.getSource(), new com.github.dockerjava.api.model.Volume(v.getTarget()));
            bindList.add(bind);
        }
        hostConfig.withBinds(bindList);

        // This is part A of #BAD-CODE
        // We have to force a null value here so the built-in docker-java library code skips creating a network,
        // Because we create the network.  We set up the data to pass to docker, but docker-java will overwrite it
        // if this hostConfig network mode is set.
        hostConfig.withNetworkMode(null);

        return hostConfig;
    }

    private ExposedPort convert(PortMap portMap) {
        InternetProtocol internetProtocol;
        switch (portMap.getProtocol()) {
            case tcp:
                internetProtocol = InternetProtocol.TCP;
                break;
            case udp:
                internetProtocol = InternetProtocol.UDP;
                break;
            default:
                internetProtocol = InternetProtocol.DEFAULT;
        }
        return new ExposedPort(portMap.getAdmiralExposedPort().port, internetProtocol);
    }

    public boolean createContainer(ComposeConfig composeConfig, AdmiralContainerConfig admiralContainerConfig, String containerName) throws AdmiralDockerException, InterruptedException, IOException {
        publish(l -> l.containerCreating(containerName));

        final CreateContainerCmd ccc = dockerImpl.getCreateContainerCmd(admiralContainerConfig.getImage())
                .withName(containerName)
                .withEnv(admiralContainerConfig.getEnvironentVariablesAsStrings())
                .withExposedPorts(createContainer_ExposedPorts(admiralContainerConfig))
                .withHostConfig(createContainer_HostConfig(admiralContainerConfig));

        if (!admiralContainerConfig.getPlatform().isEmpty()) {
            ccc.withPlatform(admiralContainerConfig.getPlatform());
        }
        if (!admiralContainerConfig.getCommand().isEmpty()) {
            ccc.withCmd(admiralContainerConfig.getCommand());
        }
        if (!admiralContainerConfig.getEntrypoint().isEmpty()) {
            ccc.withEntrypoint(admiralContainerConfig.getEntrypoint());
        }

        // This is part B of #BAD-CODE
        // Here we manually build a CreateContainerCmdImpl.NetworkingConfig since the library
        // does not give us methods to do it for us.  We want to create one so that the DockerEngine
        // will set up all of our attached networks when we create the container.
        CreateContainerCmdImpl.NetworkingConfig networkingConfig = new CreateContainerCmdImpl.NetworkingConfig();
        networkingConfig.endpointsConfig = new HashMap<>();
        for(Map.Entry<String, NetworkRef> entry : admiralContainerConfig.getNetworks().entrySet()) {
            final String composeNetworkReferenceName = entry.getKey();
            final NetworkRef networkRef = entry.getValue();
            final AdmiralNetworkConfig admiralNetworkConfig = composeConfig.getNetwork(composeNetworkReferenceName);
            final String dockerNetworkName = admiralNetworkConfig.getDockerNetworkName();

            ContainerNetwork containerNetwork = new ContainerNetwork();
            containerNetwork.withNetworkID(dockerNetworkName);
            containerNetwork.withAliases(networkRef.getAliases());
            networkingConfig.endpointsConfig.put(dockerNetworkName, containerNetwork);
        }

        // The networkingConfig field is private with no setter method... but is needed...
        // So we have to go through the reflection API and brute-force it ourselves.
        try {
            Field field = ccc.getClass().getDeclaredField("networkingConfig");
            field.setAccessible(true);
            field.set(ccc, networkingConfig);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        // END OF - This is part B of #BAD-CODE

        if (createContainer_CreateContainer(admiralContainerConfig, ccc, true)) {
            createContainer_ProcessAnyPostCopyHooks(containerName, admiralContainerConfig);
            return createContainer_ProcessAnyPostExecuteHooks(containerName, admiralContainerConfig);
        } else {
            return false;
        }
    }

    private List<ExposedPort> createContainer_ExposedPorts(AdmiralContainerConfig admiralContainerConfig) {
        List<ExposedPort> ports = new ArrayList<>();
        for(PortMap portMap : admiralContainerConfig.getPortMaps()) {
            ports.add(convert(portMap));
        }
        return ports;
    }

    private boolean createContainer_CreateContainer(AdmiralContainerConfig admiralContainerConfig, CreateContainerCmd ccc, boolean tryAgain) {
        CreateContainerResponse createContainerResponse = null;
        boolean imageDownloaded = false;
        try {
            createContainerResponse = dockerImpl.createContainer(ccc);
        } catch (AdmiralImageNotFoundException e) {
            publish(l -> l.localImageNotFound(e.getImageName()));
            publish(l -> l.imagePulling(admiralContainerConfig.getImage()));
            dockerImpl.pullImage(admiralContainerConfig.getImage(), admiralContainerConfig.getPlatform());
            imageDownloaded = true;
        } catch (AdmiralInvalidReferenceFormatException e) {
            publish(l -> l.invalidReferenceFormat(admiralContainerConfig));
        } finally {
            if (imageDownloaded && tryAgain) {
                // It is important to put the tail recursion here in the finally block and not the catch block above
                // where imageDownloaded is set to true.
                // This allows the second [inner call] return code to be passed back through the first finally block.
                // If done above, the second [inner call] return code is OVERWRITTEN by this first finally block.
                return createContainer_CreateContainer(admiralContainerConfig, ccc, false);
            } else if (createContainerResponse==null) {
                publish(l -> l.containerCreatingFailed(ccc.getName()));
                return false;
            } else {
                final CreateContainerResponse response = createContainerResponse;
                publish(l -> l.containerCreated(ccc.getName(), response, admiralContainerConfig));
                return true;
            }
        }
    }

    public void parseCauseOfInternalServerErrorException(InternalServerErrorException e) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonString = extraJsonFromDockerJavaMessage(e);
            JsonNode json = mapper.readTree(jsonString);
            String message = json.get("message").asText();
            String hostName = dockerJavaDetectImagePullNoSuchHost(message);
            if (hostName!=null) {
                publish(l -> l.hostNotFoundTryingToPullImage(hostName));
            } else {
                publish(l -> l.unhandledException(e));
            }
        } catch (AdmiralDockerException | JsonProcessingException ex) {
            // CAUTION!  If the parsing fails, we don't want to print the parsing exception (ex), we want to print
            // the original exception, with a warning saying we don't know what it is.
            publish(l -> l.unhandledException(e));
        }
    }

    public void parseCauseOfNotFoundException(NotFoundException e) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonString = extraJsonFromDockerJavaMessage(e);
            JsonNode json = mapper.readTree(jsonString);
            String message = json.get("message").asText();

            final String imageName = dockerJavaDetectImageNotFound(message);
            if (imageName!=null) {
                publish(l -> l.pulledImageNotFound(imageName));
                return;
            }

            final String manifestName = dockerJavaDetectManifestNotFound(message);
            if (manifestName!=null) {
                publish(l -> l.pulledImageManifestNotFound(manifestName));
                return;
            }

            if (dockerJavaDetectAccessDenied(message)) {
                publish(l -> l.pulledImageAccessDenied());
                return;
            }

            publish(l -> l.unhandledException(e));
        } catch (AdmiralDockerException | JsonProcessingException ex) {
            // CAUTION!  If the parsing fails, we don't want to print the parsing exception (ex), we want to print
            // the original exception, with a warning saying we don't know what it is.
            publish(l -> l.unhandledException(e));
        }
    }

    private String extraJsonFromDockerJavaMessage(com.github.dockerjava.api.exception.DockerException e) throws AdmiralDockerException {
        Pattern pattern = Pattern.compile("^Status (?:\\d+): (\\{.+\\})\\n$");
        Matcher matcher = pattern.matcher(e.getMessage());
        if (matcher.find()) {
            if (matcher.groupCount()==1) {
                String json = matcher.group(1);
                return json;
            }
        }
        throw new AdmiralDockerException(null);
    }

    private void createContainer_ProcessAnyPostCopyHooks(String containerName, AdmiralContainerConfig admiralContainerConfig)
            throws AdmiralDockerException {
        if (!admiralContainerConfig.getPostCopyHooks().isEmpty()) {
            for(CopyHook copyHook : admiralContainerConfig.getPostCopyHooks()) {
                postCreateCopyHook(containerName, copyHook);
            }
        }
    }

    private boolean createContainer_ProcessAnyPostExecuteHooks(String containerName, AdmiralContainerConfig admiralContainerConfig)
            throws AdmiralDockerException, InterruptedException {
        if (!admiralContainerConfig.getPostExecuteHooks().isEmpty()) {
            boolean containerStarted = startContainer(admiralContainerConfig, containerName);
            boolean success;
            if (!containerStarted) {
                justShutItDown(containerName, admiralContainerConfig.getStopGracePeriod());
                return false;
            } else {
                if (admiralOptions.parallelizePostCreateExecutes) {
                    success = runHooksInParallel(containerName, admiralContainerConfig.getPostExecuteHooks());
                } else {
                    success = runHooksInSeries(containerName, admiralContainerConfig.getPostExecuteHooks());
                }
                if (!success) {
                    justShutItDown(containerName, admiralContainerConfig.getStopGracePeriod());
                    return false;
                }
            }
        }
        return true;
    }

    public void createServicesInDependencyOrder(Collection<AdmiralServiceConfig> services)
            throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException  {
        Set<String> processedServiceNames = new HashSet<>();
        Set<AdmiralServiceConfig> toProcess = new HashSet<>(services);

        for(AdmiralServiceConfig admiralServiceConfig : composeConfig.getServices()) {
            if (isServiceCreated(admiralServiceConfig.getName())) {
                processedServiceNames.add(admiralServiceConfig.getName());
            }
        }

        boolean keepTrying = true;
        while (keepTrying && (!toProcess.isEmpty())) {
            for (AdmiralServiceConfig admiralServiceConfig : services) {
                // Gather
                final String serviceName = admiralServiceConfig.getName();
                // Go
                if (toProcess.contains(admiralServiceConfig)) {
                    List<String> notIn = admiralServiceConfig.dependsOnAllIn(processedServiceNames);
                    if (notIn.isEmpty()) {
                        boolean success = createService(admiralServiceConfig);
                        if (success) {
                            processedServiceNames.add(serviceName);
                            toProcess.remove(admiralServiceConfig);
                        } else {
                            publish(l -> l.error("Unable to create service " + serviceName));
                            return;
                        }
                    } else {
                        ArrayList<String> wontCreate = new ArrayList<>();
                        wontCreate.addAll(notIn);
                        for(AdmiralServiceConfig toDo : toProcess) {
                            wontCreate.remove(toDo.getName());
                        }
                        if (wontCreate.isEmpty()) {
                            publish(l -> l.warning("Can't create " + admiralServiceConfig.getName() + " yet.  Waiting on: " + String.join(", ", notIn)));
                        } else {
                            publish(l -> l.error("Unable to create " + admiralServiceConfig.getName() + " because the following depends_on services are not created: " + String.join(", ", wontCreate)));
                            keepTrying = false;
                        }
                    }
                }
            }
            try {
                // TODO - Sleeps are evil.  Static, no back-off sleeps are more evil.
                if (keepTrying)
                    Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("CS: " + e.getMessage());
                keepTrying = false;
            }
        }
    }

    private boolean createService(AdmiralServiceConfig admiralServiceConfig)
            throws AdmiralDockerException, InterruptedException, IOException {
        final String serviceName = admiralServiceConfig.getName();

        publish(l -> l.serviceCreating(serviceName));

        final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();

        for (int i = 1; i <= replicas; i++) {
            final String containerName = composeConfig.calculateContainerName(serviceName, i);

            if (containerExists(containerName))
                continue;

            boolean success = createContainer(composeConfig, admiralServiceConfig, containerName);
            if (!success) {
                return false;
            }
        }

        publish(l -> l.serviceCreated(serviceName));
        return true;
    }

    public void joinServices(Collection<AdmiralServiceConfig> services) throws AdmiralDockerException {
        for (AdmiralServiceConfig admiralServiceConfig : services) {
            joinService(admiralServiceConfig);
        }
    }

    public void joinService(AdmiralServiceConfig admiralServiceConfig)
            throws AdmiralDockerException {
        // Gather
        final String serviceName = admiralServiceConfig.getName();

        // Notify
        publish(l -> l.serviceJoining(serviceName));

        // Go
        final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
        for (int i = 1; i <= replicas; i++) {
            final String containerName = composeConfig.calculateContainerName(serviceName, i);
            publish(l -> l.containerJoining(containerName));
            try {
                final boolean running = isContainerRunning(containerName);
                if (running) {
                    attachToContainer(admiralServiceConfig, containerName);
                    attachToLogs(admiralServiceConfig, containerName, true);
                }
            } catch (AdmiralContainerNotFoundException e) {
                publish(l -> l.containerNotFound(containerName, true));
            }
            publish(l -> l.containerJoined(containerName));
        }
    }

    public void unjoinServices(Collection<AdmiralServiceConfig> services) throws AdmiralDockerException {
        for (AdmiralServiceConfig admiralServiceConfig : services) {
            unjoinService(admiralServiceConfig);
        }
    }

    public void unjoinService(AdmiralServiceConfig admiralServiceConfig)
            throws AdmiralDockerException {
        // Gather
        final String serviceName = admiralServiceConfig.getName();

        // Notify
        publish(l -> l.serviceJoining(serviceName));

        // Go
        final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
        for (int i = 1; i <= replicas; i++) {
            final String containerName = composeConfig.calculateContainerName(serviceName, i);
            try {
                final boolean running = isContainerRunning(containerName);
                if (running) {
                    detatchFromContainer(containerName);
                }
            } catch (AdmiralContainerNotFoundException e) {
                publish(l -> l.containerNotFound(containerName, true));
            }
        }
    }

    private void justShutItDown(String containerName, Duration stopGracePeriod) throws AdmiralDockerException {
        publish(l -> l.warning("Container: " + containerName + " has post_create_execute hooks that did not complete successfully or were interrupted."));
        publish(l -> l.warning("The container must be stopped and removed."));
        publish(l -> l.warning("Trying to stop container " + containerName + "... Will kill after " + stopGracePeriod.prettyS()));
        stopContainer(containerName, stopGracePeriod);
        publish(l -> l.warning("Trying to remove container " + containerName + "..."));
        removeContainer(containerName);
        publish(l -> l.warning("Done with create hook fail cleanup."));
    }

    private void cancel(Collection<ForkJoinTask> tasks) {
        for(ForkJoinTask task : tasks) {
            if (!task.isCancelled()) {
                task.cancel(true);
            }
        }
    }

    private boolean runHooksInParallel(String containerName, List<ExecuteHook> executeHooks) {
        final Collection<ForkJoinTask> tasks = new ArrayList<>();
        for(ExecuteHook executeHook : executeHooks) {
            tasks.add(new ForkJoinTask() {
                private Boolean success;
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return super.cancel(mayInterruptIfRunning);
                }

                @Override
                public Object getRawResult() {
                    return success;
                }

                @Override
                protected void setRawResult(Object value) {
                }

                @Override
                protected boolean exec() {
                    try {
                        postCreateExecuteHook(containerName, executeHook);
                    } catch (InterruptedException e) {
                        DockerModelController.this.cancel(tasks);
                        return false;
                    } catch (Exception e) {
                        DockerModelController.this.cancel(tasks);
                        return false;
                    }
                    success = true;
                    return success;
                }
            });
        }
        try {
            ForkJoinTask.invokeAll(tasks);
        } catch (CancellationException e) {
            publish(l -> l.error("Post Create Execute Hooks were cancelled."));
            return false;
        }
        return true;
    }

    private boolean runHooksInSeries(String containerName, List<ExecuteHook> executeHooks) {
        try {
            for (ExecuteHook executeHook : executeHooks) {
                postCreateExecuteHook(containerName, executeHook);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void startLine(String cmdId, String s) {
        publish(l -> l.executeHookStart(cmdId, s));
    }

    @Override
    public void stdoutLine(String cmdId, String s) {
        publish(l -> l.executeHookStdoutLine(cmdId, s));
    }

    @Override
    public void stderrLine(String cmdId, String s) {
        publish(l -> l.executeHookStderrLine(cmdId, s));
    }

    @Override
    public void doneLine(String cmdId, String s) {
        publish(l -> l.executeHookDone(cmdId, s));
    }

    public void addLine(String containerName, String streamName, String s) {
        publish(l -> l.addLine(containerName, streamName, s));
    }

    private void postCreateExecuteHook(String containerName, ExecuteHook postExecuteHook) throws Exception {
        if (postExecuteHook!=null) {
            postExecuteHook.setExecuteHookListener(this);
            postExecuteHook.call();
        }
    }

    private void postCreateCopyHook(String containerName, CopyHook copyHook) throws AdmiralDockerException {
        Path path = FileSystems.getDefault().getPath(copyHook.getSource());
        publish(l -> l.containerCopingFileTo(containerName, path.toString(), copyHook.getTarget()));
        try {
            dockerImpl.copyToContainer(containerName, path.toString(), copyHook.getTarget());
            publish(l -> l.containerCopiedFileTo(containerName, path.toString(), copyHook.getTarget()));
        } catch (AdmiralDockerException e) {
            publish(l -> l.containerCopingFileToFailed(containerName, path.toString(), copyHook.getTarget()));
        }
    }

    public void stopContainer(String containerName, Duration stopGracePeriod) throws AdmiralDockerException {
        publish(l -> l.containerStopping(containerName));
        final boolean running = isContainerRunning(containerName);

        if (running) {
            dockerImpl.stopContainer(containerName, admiralOptions.killContainersWhenStopping ? 0 : stopGracePeriod.getSeconds());
        }
        ContainerController containerController = containerSet.get(containerName);
        containerController.shutdown();

        publish(l -> l.containerStopped(containerName, !running));
    }

    public String ping() throws AdmiralDockerException {
        return dockerImpl.ping();
    }

    public Version version() throws AdmiralDockerException {
        return dockerImpl.version();
    }

    public Info info() throws AdmiralDockerException {
        return dockerImpl.info();
    }

    public List<Container> listContainers() throws AdmiralDockerException {
        return dockerImpl.listContainers();
    }

    public List<Image> listImages() throws AdmiralDockerException {
        return dockerImpl.listImages();
    }

    public Collection<String> getContainerNames() {
        return containerSet.keySet();
    }

    public Collection<ContainerController> inspectContainers() throws AdmiralDockerException {
        for(ContainerController containerController : containerSet.values()) {
            final String containerName = containerController.getName();
            containerController.setInspectContainerResponse(dockerImpl.inspectContainer(containerName));
        }
        return containerSet.values();
    }

    public Map<String, String> getContainerEnvironmentVariables(String containerName) throws AdmiralDockerException {
        Map<String, String> vars = new HashMap<>();
        InspectContainerResponse inspectContainerResponse = dockerImpl.inspectContainer(containerName);
        String[] rawVars = inspectContainerResponse.getConfig().getEnv();
        for(String rawVar : rawVars) {
            final String[] pieces = rawVar.split("=",2);
            if (pieces.length==2) {
                final String key = pieces[0];
                final String value = pieces[1];
                vars.put(key, value);
            }
        }
        return vars;
    }


    @Override
    public void startWaitProgress(int current, int total, String url, String status, String progress) {
        publish(l -> l.waitProgressMessage(current, total, url, status, progress));
    }

    @Override
    public void healthCheckProgress(int current, int total, String url, String status, String progress) {
        publish(l -> l.waitProgressMessage(current, total, url, status, progress));
    }

    @Override
    public void healthCheckProgress(String url, String status, String progress) {
        publish(l -> l.waitProgressMessage(url, status, progress));
    }

    public ExecCreateCmdResponse execute(String containerName, ResultCallback<Frame> rc, String... cmd) throws AdmiralDockerException {
        return dockerImpl.execute(containerName, rc, cmd);
    }

    public long checkExecResponse(ExecCreateCmdResponse execCreateCmdResponse) throws AdmiralDockerException {
        return dockerImpl.checkExecResponse(execCreateCmdResponse);
    }

    @Override
    public void log(String s) {
        publish(l -> l.debug(s));
    }

    @Override
    public void log(String prompt, String value) {
        publish(l -> l.debug(prompt + "=" + value ));
    }
}
