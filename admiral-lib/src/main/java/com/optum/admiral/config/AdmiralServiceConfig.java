package com.optum.admiral.config;

import com.optum.admiral.ConfigVariableProcessor;
import com.optum.admiral.ContainerParameterProcessor;
import com.optum.admiral.model.ComposeHealthCheck;
import com.optum.admiral.model.ExecuteHook;
import com.optum.admiral.model.HealthCheck;
import com.optum.admiral.model.NetworkRef;
import com.optum.admiral.model.StartPortWait;
import com.optum.admiral.model.URLAdmiralHealthCheck;
import com.optum.admiral.model.StartWait;
import com.optum.admiral.type.Assume;
import com.optum.admiral.type.Commands;
import com.optum.admiral.type.CopyHook;
import com.optum.admiral.type.Dependant;
import com.optum.admiral.type.Duration;
import com.optum.admiral.type.LogMonitor;
import com.optum.admiral.type.PortMap;
import com.optum.admiral.type.SemanticVersion;
import com.optum.admiral.type.VariableSpec;
import com.optum.admiral.type.Volume;
import com.optum.admiral.type.exception.InvalidSemanticVersion;
import com.optum.admiral.type.exception.VariableSpecContraint;
import com.optum.admiral.util.FileService;
import com.optum.admiral.yaml.CommandsYaml;
import com.optum.admiral.yaml.ServiceYamlTweakableSupplier;
import com.optum.admiral.yaml.TweaksYaml;
import com.optum.admiral.yaml.CopyHookYaml;
import com.optum.admiral.yaml.EnvironmentSpecificationYaml;
import com.optum.admiral.yaml.ExecuteHookYaml;
import com.optum.admiral.yaml.HealthCheckYaml;
import com.optum.admiral.yaml.LogMonitorYaml;
import com.optum.admiral.yaml.NetworkRefYaml;
import com.optum.admiral.yaml.ServiceYaml;
import com.optum.admiral.yaml.VolumeYaml;
import com.optum.admiral.yaml.WaitHookYaml;
import com.optum.admiral.yaml.YamlParserHelper;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class AdmiralServiceConfig implements AdmiralContainerConfig, Comparable<AdmiralServiceConfig> {
    private final String name;
    private Assume assume = Assume.DONOT;
    private Commands commands = Commands.AUTO;
    private Duration stopGracePeriod = new Duration("10s");  // Default is implemented here, not in the YAML.  (YAML only overrides, it doesn't provide defaults.)
    private SemanticVersion codeVersion;
    private DeployConfig deployConfig;
    private HealthCheck composeHealthCheck;
    private final List<URLAdmiralHealthCheck> admiralHealthChecks = new ArrayList<>();
    private final List<String> command = new ArrayList<>();
    private final List<String> entrypoint = new ArrayList<>();
    private EnvironmentSpecification environmentSpecification;
    private final List<ActionMonitor> actionMonitors = new ArrayList<>();
    private final List<LogMonitor> logMonitors = new ArrayList<>();
    private final List<ServiceYaml> sources = new ArrayList<>();
    private final Set<String> showVariables = new HashSet<>();
    private final Set<String> serviceGroups = new HashSet<>();
    private final Map<String, NetworkRef> networks = new HashMap<>();

    // This is reset with each config load.
    private ContainerParameterProcessor containerParameterProcessor;

    public AdmiralServiceConfig(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<String> getCommand() {
        return command;
    }

    @Override
    public List<String> getEntrypoint() {
        return entrypoint;
    }

    @Override
    public Duration getStopGracePeriod() {
        return stopGracePeriod;
    }

    public Set<String> getServiceGroups() {
        return serviceGroups;
    }

    public boolean hasServiceGroups() {
        return !serviceGroups.isEmpty();
    }

    @Override
    public Set<String> getShowVariables() {
        return showVariables;
    }

    public boolean showVariable(String key) {
        return showVariables.contains(key);
    }

    public boolean shouldAssume() {
        return assume != Assume.DONOT;
    }

    public boolean shouldAssumeRunning() {
        return assume == Assume.RUNNING;
    }

    public Assume getAssume() {
        return assume;
    }

    public Commands getCommands() {
        return commands;
    }

    public ContainerParameterProcessor getContainerEnvironmentVariableProcessor() {
        return containerParameterProcessor;
    }

    public EnvironmentSpecification getEnvironmentSpecification() {
        return environmentSpecification;
    }

    public void validateEnvironmentSpecification() throws VariableSpecContraint {
        // No spec - so we're good.
        if (environmentSpecification==null)
            return;

        if (codeVersion==null && environmentSpecification.hasCodeVersionConstraints()) {
            throw new VariableSpecContraint("Environment Variable Constraints exist for " + name + " but no x-admiral_code_version for that container has been specified.");
        }

        StringBuilder errorMessage = null;
        // This makes sure each environment: entry is legit.  But it can't find MISSING ones.
        for(ContainerParameterProcessor.Entry entry : containerEnvironmentVariables.values()) {
            String error = environmentSpecification.validateUse(codeVersion, entry.key);
            if (error != null) {
                if (errorMessage==null) {
                    errorMessage = new StringBuilder();
                } else {
                    errorMessage.append("\n");
                }
                errorMessage.append(error);
            }
        }

        // This looks for missing ones.
        for(VariableSpec variableSpec : environmentSpecification.getVariables()) {
            if (variableSpec.isRequiredFor(codeVersion) && !containerEnvironmentVariables.containsKey(variableSpec.name)) {
                if (errorMessage==null) {
                    errorMessage = new StringBuilder();
                } else {
                    errorMessage.append("\n");
                }
                errorMessage.append("Environmment variable \"" + variableSpec.name + "\" is required for code version \"" +
                        codeVersion + "\" but that environment variable is missing.");
            }
        }

        if (errorMessage!=null) {
            throw new VariableSpecContraint("Service " + name + " has errors:\n" + errorMessage.toString());
        }
    }

    private boolean defined(String s) {
        if (s==null) return false;
        return (s.length()!=0);
    }

    public void applyTweaksYaml(TweaksYaml tweaksYaml, ConfigVariableProcessor configVariableProcessor) throws
            AdmiralConfigurationException {
        YamlParserHelper yph = new YamlParserHelper(configVariableProcessor);

        if (tweaksYaml.hasAssume()) {
            assume = tweaksYaml.getAssume();
        }

        if (tweaksYaml.hasCommands()) {
            CommandsYaml commandsYaml = tweaksYaml.getCommands();
            commands = new Commands(commandsYaml);
        }

        if (tweaksYaml.hasDigField()) {
            applyServiceYaml_Dig(tweaksYaml, true);
        }

        if (tweaksYaml.hasDependsOnField()) {
            applyServiceYaml_DependsOn(tweaksYaml, true);
        }

        if (tweaksYaml.hasExposedPortsField()) {
            applyServiceYaml_ExposedPorts(tweaksYaml, yph, true);
        }

        if (tweaksYaml.hasGroupsField()) {
            applyServiceYaml_ServiceGroups(tweaksYaml, true);
        }
    }

    public void applyServiceYaml(FileService fileService, ServiceYaml serviceYaml, ConfigVariableProcessor configVariableProcessor) throws
            AdmiralConfigurationException, InvalidSemanticVersion, VariableSpecContraint, InvalidBooleanException, PropertyNotFoundException, InvalidEnumException {
        sources.add(serviceYaml);

        YamlParserHelper yph = new YamlParserHelper(configVariableProcessor);

        applyServiceYaml_ActionMonitors(fileService, serviceYaml);
        applyServiceYaml_CodeVersion(serviceYaml, yph);
        applyServiceYaml_Command(serviceYaml);
        applyServiceYaml_DependsOn(serviceYaml, false);
        applyServiceYaml_DeployConfig(serviceYaml);
        applyServiceYaml_Entrypoint(serviceYaml);
        applyServiceYaml_EnvironmentSpecs(serviceYaml, yph);
        applyServiceYaml_ExposedPorts(serviceYaml, yph, false);
        applyServiceYaml_ExtraHosts(serviceYaml);
        applyServiceYaml_HealthCheck(serviceYaml, yph);
        applyServiceYaml_Image(serviceYaml, yph);
        applyServiceYaml_Platform(serviceYaml, yph);
        applyServiceYaml_LogMonitors(fileService, serviceYaml, yph);
        applyServiceYaml_Networks(serviceYaml);
        applyServiceYaml_PostCreateCopy(serviceYaml, yph);
        applyServiceYaml_PostCreateExecute(serviceYaml, yph);
        applyServiceYaml_PostStartWait(serviceYaml, yph);
        applyServiceYaml_ServiceGroups(serviceYaml, false);
        applyServiceYaml_Dig(serviceYaml, false);
        applyServiceYaml_StopGracePeriod(serviceYaml, yph);
        applyServiceYaml_Volumes(serviceYaml, yph, fileService);
        applyServiceYaml_VolumesFrom(serviceYaml);

        thenReprocessEnvironmentVariables(configVariableProcessor);
    }

    private void applyServiceYaml_EnvironmentSpecs(ServiceYaml serviceYaml, YamlParserHelper yph)
            throws AdmiralConfigurationException, InvalidSemanticVersion, VariableSpecContraint, InvalidBooleanException, PropertyNotFoundException, InvalidEnumException {
        if (!serviceYaml.x_admiral_environment_specs.isEmpty()) {
            if (environmentSpecification == null) {
                environmentSpecification = new EnvironmentSpecification();
            }
            for (String spec : serviceYaml.x_admiral_environment_specs) {
                final String environmentSpecString = yph.getS(spec);
                URL environmentSpecURL;
                try {
                    environmentSpecURL = new URL(environmentSpecString);
                } catch (MalformedURLException e) {
                    throw new AdmiralConfigurationException(name, "Bad URL: " + environmentSpecString);
                }
                EnvironmentSpecificationYaml environmentSpecificationYaml = EnvironmentSpecificationYaml.loadFromYamlURL(environmentSpecURL);
                environmentSpecification.addVariables(yph, environmentSpecificationYaml.getVariables());
            }
        }
    }

    private void applyServiceYaml_PostCreateExecute(ServiceYaml serviceYaml, YamlParserHelper yph) throws AdmiralConfigurationException {
        for(ExecuteHookYaml executeHookYaml : serviceYaml.x_admiral_post_create_execute) {
            postExecuteHooks.add(new ExecuteHook(yph.getS(executeHookYaml.id),
                    yph.getS(executeHookYaml.cmd),
                    yph.getS(executeHookYaml.working_dir)));
        }
    }

    private void applyServiceYaml_PostCreateCopy(ServiceYaml serviceYaml, YamlParserHelper yph) throws AdmiralConfigurationException {
        for(CopyHookYaml copyHookYaml : serviceYaml.x_admiral_post_create_copy) {
            postCopyHooks.add(new CopyHook(yph.getS(copyHookYaml.source), yph.getS(copyHookYaml.target)));
        }
    }

    private void applyServiceYaml_ExposedPorts(ServiceYamlTweakableSupplier serviceYamlPortsSupplier, YamlParserHelper yph, boolean replace) throws AdmiralConfigurationException {
        if (replace) {
            portMaps.clear();
        }
        for(String port: serviceYamlPortsSupplier.getExposedPorts()) {
            if (port==null || port.isEmpty())
                throw new AdmiralConfigurationException("", "Blank ports: lines are not allowed.");
            PortMap portMap = new PortMap(yph.getS(port));
            portMaps.add(portMap);
        }
    }

    private void applyServiceYaml_Volumes(ServiceYaml serviceYaml, YamlParserHelper yph, FileService fileService) throws AdmiralConfigurationException {
        for(VolumeYaml volumeYaml : serviceYaml.getVolumes()) {
            if (volumeYaml==null)
                throw new AdmiralConfigurationException("", "Blank volumes: lines are not allowed.");
            // Volumes *REPLACE* with the primary unique key being the container path.
            Volume volume = volumeYaml.createVolume(fileService, yph);
            volumes.put(volume.getSource(), volume);
        }
    }

    private void applyServiceYaml_DependsOn(ServiceYamlTweakableSupplier serviceYamlTweakableSupplier, boolean replace) throws AdmiralConfigurationException {
        if (replace) {
            dependsOn.clear();
        }
        for(String serviceName : serviceYamlTweakableSupplier.getDependsOn()) {
            if (serviceName==null || serviceName.isEmpty())
                throw new AdmiralConfigurationException("", "Blank depends_on: lines are not allowed.");
            dependsOn.put(serviceName, new Dependant(serviceName));
        }
    }

    private void applyServiceYaml_ActionMonitors(FileService fileService, ServiceYaml serviceYaml)
            throws AdmiralConfigurationException, InvalidBooleanException, PropertyNotFoundException, InvalidEnumException {
        for(String action_monitor : serviceYaml.x_admiral_action_monitors) {
            if (action_monitor==null || action_monitor.isEmpty())
                throw new AdmiralConfigurationException("", "Blank x-admiral_action_monitors: lines are not allowed.");
            actionMonitors.add(new ActionMonitor(fileService.relativeFile(action_monitor)));
        }
    }

    private void applyServiceYaml_CodeVersion(ServiceYaml serviceYaml, YamlParserHelper yph)
            throws AdmiralConfigurationException, InvalidSemanticVersion {
        SemanticVersion codeVersion = SemanticVersion.parse(yph.getS(serviceYaml.x_admiral_code_version));
        if (codeVersion!=null) {
            this.codeVersion = codeVersion;
        }
    }

    private void applyServiceYaml_Command(ServiceYaml serviceYaml) {
        command.addAll(serviceYaml.getCommandPieces());
    }

    private void applyServiceYaml_Entrypoint(ServiceYaml serviceYaml) {
        entrypoint.addAll(serviceYaml.getEntrypointPieces());
    }

    private void applyServiceYaml_StopGracePeriod(ServiceYaml serviceYaml, YamlParserHelper yph) throws AdmiralConfigurationException {
        if (defined(serviceYaml.stop_grace_period)) {
            stopGracePeriod = new Duration(yph.getS(serviceYaml.stop_grace_period));
        }
    }

    private void applyServiceYaml_Image(ServiceYaml serviceYaml, YamlParserHelper yph) throws AdmiralConfigurationException {
        if (defined(serviceYaml.image)) {
            this.image = yph.getS(serviceYaml.image);
        }
    }

    private void applyServiceYaml_Platform(ServiceYaml serviceYaml, YamlParserHelper yph) throws AdmiralConfigurationException {
        if (defined(serviceYaml.platform)) {
            this.platform = yph.getS(serviceYaml.platform);
        }
    }

    private void applyServiceYaml_ExtraHosts(ServiceYaml serviceYaml) {
        extraHosts.addAll(serviceYaml.extra_hosts);
    }

    private void applyServiceYaml_VolumesFrom(ServiceYaml serviceYaml) {
        volumesFrom.addAll(serviceYaml.volumes_from);
    }

    private void applyServiceYaml_LogMonitors(FileService fileService, ServiceYaml serviceYaml, YamlParserHelper yph)
            throws AdmiralConfigurationException, InvalidBooleanException, PropertyNotFoundException, InvalidEnumException {
        for(LogMonitorYaml logMonitorYaml : serviceYaml.x_admiral_log_monitors) {
            if (logMonitorYaml==null)
                throw new AdmiralConfigurationException("", "Blank x-admiral_log_monitors: lines are not allowed.");
            final String logName = yph.getS(logMonitorYaml.filename);
            final boolean deleteAtStart = yph.getB("delete_at_start", logMonitorYaml.delete_at_start);
            final List<File> actionMonitorFiles = new ArrayList<>();
            for(String filename : logMonitorYaml.action_monitors) {
                actionMonitorFiles.add(fileService.relativeFile(yph.getS(filename)));
            }
            logMonitors.add(new LogMonitor(logName, deleteAtStart, actionMonitorFiles));
        }
    }

    private void applyServiceYaml_HealthCheck(ServiceYaml serviceYaml, YamlParserHelper yph) throws AdmiralConfigurationException {
        HealthCheckYaml healthCheckYaml = serviceYaml.getHealthCheck();
        if (healthCheckYaml!=null) {
            final boolean isCmdShell = "CMD-SHELL".equals(healthCheckYaml.getArgs().get(0));
            final long start_period = new Duration(yph.getS(healthCheckYaml.start_period)).getMS();
            final long timeout = new Duration(yph.getS(healthCheckYaml.timeout)).getMS();
            final boolean disabled;
            if ("NONE".equals(healthCheckYaml.getArgs().get(0)) || yph.getB("disable", healthCheckYaml.disable)) {
                disabled = true;
            } else {
                disabled = false;
            }
            List<String> evaledArgs = new ArrayList<>();
            for(String arg : healthCheckYaml.getArgs().subList(1, healthCheckYaml.getArgs().size())) {
                evaledArgs.add(yph.getS(arg));
            }
            String[] args = evaledArgs.toArray(new String[0]);
            final long interval = new Duration(yph.getS(healthCheckYaml.interval)).getMS();
            final long minimum_interval;
            if (healthCheckYaml.x_admiral_minimum_interval !=null) {
                minimum_interval = new Duration(yph.getS(healthCheckYaml.x_admiral_minimum_interval)).getMS();
            } else {
                minimum_interval = interval;
            }
            final long rewait_period;
            if (healthCheckYaml.x_admiral_rewait_period != null) {
                rewait_period = new Duration(yph.getS(healthCheckYaml.x_admiral_rewait_period)).getMS();
            } else {
                rewait_period = start_period;
            }
            final long rewait_interval;
            if (healthCheckYaml.x_admiral_rewait_interval != null) {
                rewait_interval = new Duration(yph.getS(healthCheckYaml.x_admiral_rewait_interval)).getMS();
            } else {
                rewait_interval = interval;
            }

            composeHealthCheck = new ComposeHealthCheck(yph.getS(healthCheckYaml.x_admiral_id), isCmdShell, args,
                    start_period,
                    timeout,
                    yph.getI("retries", healthCheckYaml.retries),
                    interval,
                    minimum_interval,
                    rewait_period,
                    rewait_interval,
                    disabled);
        }
    }

    private void applyServiceYaml_PostStartWait(ServiceYaml serviceYaml, YamlParserHelper yph) throws AdmiralConfigurationException {
        for(WaitHookYaml waitHookYaml : serviceYaml.x_admiral_post_start_wait) {
            final String url = yph.getS(waitHookYaml.url);
            if (url!=null && (!url.isEmpty())) {
                final String search = yph.getS(waitHookYaml.search);
                final long start_period = new Duration(yph.getS(waitHookYaml.start_period)).getMS();
                final long timeout = new Duration(yph.getS(waitHookYaml.timeout)).getMS();
                final long interval = new Duration(yph.getS(waitHookYaml.interval)).getMS();
                final long minimum_interval;
                if (waitHookYaml.minimum_interval != null) {
                    minimum_interval = new Duration(yph.getS(waitHookYaml.minimum_interval)).getMS();
                } else {
                    minimum_interval = interval;
                }
                final long rewait_period;
                if (waitHookYaml.rewait_period != null) {
                    rewait_period = new Duration(yph.getS(waitHookYaml.rewait_period)).getMS();
                } else {
                    rewait_period = start_period;
                }
                final long rewait_interval;
                if (waitHookYaml.rewait_interval != null) {
                    rewait_interval = new Duration(yph.getS(waitHookYaml.rewait_interval)).getMS();
                } else {
                    rewait_interval = interval;
                }

                final int retries = yph.getI("retries", waitHookYaml.retries);
                final boolean disable = yph.getB("disable", waitHookYaml.disable);
                final boolean successWhenRedirected = yph.getB("success_when_redirected", waitHookYaml.success_when_redirected);

                admiralHealthChecks.add( new URLAdmiralHealthCheck(yph.getS(waitHookYaml.id),
                                url, search, successWhenRedirected,
                        start_period,
                        timeout,
                        retries,
                        interval,
                        minimum_interval,
                        rewait_period,
                        rewait_interval,
                        disable));
            } else {
                final int port = yph.getI("port", waitHookYaml.port);
                final String host = yph.getS(waitHookYaml.host);
                final int timeout = yph.getI("timeout", waitHookYaml.timeout);
                postStartWaits.add(
                        new StartPortWait(host, port, timeout));
            }
        }
    }

    private void applyServiceYaml_ServiceGroups(ServiceYamlTweakableSupplier serviceYaml, boolean replace) {
        if (replace) {
            serviceGroups.clear();
        }
        serviceGroups.addAll(serviceYaml.getGroups());
    }

    private void applyServiceYaml_Dig(ServiceYamlTweakableSupplier serviceYamlTweakableSupplier, boolean replace) {
        if (replace) {
            showVariables.clear();
        }
        showVariables.addAll(serviceYamlTweakableSupplier.getDig());
    }

    private void applyServiceYaml_DeployConfig(ServiceYaml serviceYaml) {
        this.deployConfig = new DeployConfig(serviceYaml.deploy.replicas);
    }

    private void thenReprocessEnvironmentVariables(ConfigVariableProcessor configVariableProcessor) throws AdmiralConfigurationException {
        // Each time a new serviceYaml is applied we have to rebuild, since all env_vars must process before any environment.
        containerEnvironmentVariables.clear();

        containerParameterProcessor = new ContainerParameterProcessor(configVariableProcessor);

        // env_file section processing is first
        for(ServiceYaml sy : sources) {
            for(String envFile : sy.getEnvFiles()) {
                File envFilePath = sy.getRelativePathForEnvFile(envFile);
                try {
                    containerParameterProcessor.addContainerEnvironmentVariablesFromFileNamed(envFilePath);
                } catch (IOException e) {
                    // TODO - rethrow intelligent yaml error.
                    System.out.println("Error loading properties from " + envFilePath);
                    e.printStackTrace();
                }
            }
        }

        // environment section processing is second, because these must override values set in env_file
        for(ServiceYaml sy : sources) {
            final String source = sy.getParent().getSourceFile().getPath();
            for(Map.Entry<String, String> entry : sy.getEnvironmentVariables().entrySet()) {
                final String key = entry.getKey();
                final String value = entry.getValue();
                containerParameterProcessor.addEnvironmentVariable(key, value, source);
            }
        }

        containerEnvironmentVariables.putAll(containerParameterProcessor.getData());
    }

    private void applyServiceYaml_Networks(ServiceYaml serviceYaml) {
        if (!serviceYaml.networks.isEmpty()) {
            // If there are networks listed, use them.
            for (Map.Entry<String, NetworkRefYaml> entry : serviceYaml.networks.entrySet()) {
                String networkName = entry.getKey();
                NetworkRefYaml networkRefYaml = entry.getValue();
                List<String> aliases = new ArrayList<>(networkRefYaml.aliases);
                // This is where the alias for the service name is added.
                if (!aliases.contains(name)) {
                    aliases.add(name);
                }
                NetworkRef networkRef = new NetworkRef(networkName, aliases);
                networks.put(networkName, networkRef);
            }
        } else {
            // Otherwise, we put the service in the default network.
            String networkName = "default";
            // This is where the alias for the service name is added.
            List<String> aliases = new ArrayList<>();
            aliases.add(name);
            NetworkRef networkRef = new NetworkRef(networkName, aliases);
            networks.put(networkName, networkRef);
        }
    }

    private String image;
    private String platform = "";
    private final List<CopyHook> postCopyHooks = new ArrayList<>();
    private final List<ExecuteHook> postExecuteHooks = new ArrayList<>();
    private final List<StartWait> postStartWaits = new ArrayList<>();
    private final List<PortMap> portMaps = new ArrayList<>();
    private final Map<String, Volume> volumes = new TreeMap<>();
    private final Map<String, ContainerParameterProcessor.Entry> containerEnvironmentVariables = new TreeMap<>();

    private final Map<String, Dependant> dependsOn = new TreeMap<>();
    private final Set<String> extraHosts = new HashSet<>();
    private final Set<String> volumesFrom = new HashSet<>();

    @Override
    public String getImage() {
        return image;
    }

    @Override
    public String getPlatform() {
        return platform;
    }

    public List<ActionMonitor> getActionMonitors() {
        return actionMonitors;
    }

    public List<LogMonitor> getLogMonitors() {
        return logMonitors;
    }

    @Override
    public HealthCheck getComposeHealthCheck() {
        return composeHealthCheck;
    }

    @Override
    public List<URLAdmiralHealthCheck> getAdmiralHealthChecks() {
        return admiralHealthChecks;
    }

    @Override
    public List<StartWait> getPostStartWaits() {
        return postStartWaits;
    }

    @Override
    public List<ExecuteHook> getPostExecuteHooks() {
        return postExecuteHooks;
    }

    @Override
    public List<CopyHook> getPostCopyHooks() {
        return postCopyHooks;
    }

    public int getDependsOnCount() {
        return dependsOn.size();
    }

    @Override
    public Collection<Dependant> getDependsOn() {
        return dependsOn.values();
    }

    @Override
    public Map<String, NetworkRef> getNetworks() {
        return networks;
    }

    public DeployConfig getDeployConfig() {
        return deployConfig;
    }

    public List<String> dependsOnAllIn(Collection<String> serviceNames) {
        List<String> notIn = new ArrayList<>();

        for (Dependant dependant : dependsOn.values()) {
            final String name = dependant.getServiceName();
            if (!serviceNames.contains(name)) {
                notIn.add(name);
            }
        }
        return notIn;
    }

    public Set<String> missingDependsOnIn(Collection<String> serviceNames) {
        Set<String> missing = new HashSet<>();
        for (Dependant dependant : dependsOn.values()) {
            if (!serviceNames.contains(dependant.getServiceName())) {
                missing.add(dependant.getServiceName());
            }
        }
        return missing;
    }


    public int getPortsCount() {
        return portMaps.size();
    }
    @Override
    public List<PortMap> getPortMaps() {
        return portMaps;
    }

    @Override
    public Collection<Volume> getVolumes() {
        return volumes.values();
    }

    public String getEnvironmentVariable(String key) {
        return containerEnvironmentVariables.get(key).getValue();
    }

    public int getEnvironmentVariableCount() {
        return containerEnvironmentVariables.size();
    }

    @Override
    public Map<String, ContainerParameterProcessor.Entry> getEnvironmentVariables() {
        return containerEnvironmentVariables;
    }

    @Override
    public List<String> getEnvironentVariablesAsStrings() {
        List<String> asList = new ArrayList<>();
        for (Map.Entry<String, ContainerParameterProcessor.Entry> entry : getEnvironmentVariables().entrySet()) {
            String key = entry.getKey();
            ContainerParameterProcessor.Entry entryvalue = entry.getValue();
            String value = entryvalue.getValue();
            if (value != null) {
                asList.add(key + "=" + value);
            } else {
                asList.add(key);
            }
        }
        return asList;
    }

    @Override
    public Set<String> getExtraHosts() {
        return extraHosts;
    }

    @Override
    public Set<String> getVolumesFrom() {
        return volumesFrom;
    }

    @Override
    public int compareTo(AdmiralServiceConfig o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (o==null)
            return false;

        if (!(o instanceof AdmiralServiceConfig))
            return false;

        AdmiralServiceConfig other = (AdmiralServiceConfig) o;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
