package com.optum.admiral;

import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Version;
import com.optum.admiral.action.CommandsACT;
import com.optum.admiral.action.ConfigACT;
import com.optum.admiral.action.EnvACT;
import com.optum.admiral.action.GroupsACT;
import com.optum.admiral.action.InspectACT;
import com.optum.admiral.action.ListACT;
import com.optum.admiral.action.PsACT;
import com.optum.admiral.action.ServicesACT;
import com.optum.admiral.action.SetACT;
import com.optum.admiral.action.DigACT;
import com.optum.admiral.action.ShowComposeACT;
import com.optum.admiral.action.ShowConfigACT;
import com.optum.admiral.action.ShowParametersACT;
import com.optum.admiral.action.ShowPreferencesACT;
import com.optum.admiral.action.TestACT;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.event.AdmiralEventListener;
import com.optum.admiral.event.AdmiralEventPublisher;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.logging.ActionHarness;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.Container;
import com.optum.admiral.model.ContainerController;
import com.optum.admiral.model.DockerModelController;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.model.Image;
import com.optum.admiral.model.ProgressHandler;
import com.optum.admiral.model.ProgressMessage;
import com.optum.admiral.preferences.OutputPreferences;
import com.optum.admiral.preferences.UXPreferences;
import com.optum.admiral.serviceaction.BounceAction;
import com.optum.admiral.serviceaction.CreateAction;
import com.optum.admiral.serviceaction.DownAction;
import com.optum.admiral.serviceaction.RestartAction;
import com.optum.admiral.serviceaction.RmAction;
import com.optum.admiral.serviceaction.StartAction;
import com.optum.admiral.serviceaction.StopAction;
import com.optum.admiral.serviceaction.UpAction;
import com.optum.admiral.type.Commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class Admiral
        implements AdmiralEventPublisher, ProgressHandler {
    private final DockerModelController dmc;
    private final ComposeConfig composeConfig;

    private final BounceAction bounceAction;
    private final CreateAction createAction;
    private final DownAction downAction;
    private final RestartAction restartAction;
    private final RmAction rmAction;
    private final StartAction startAction;
    private final StopAction stopAction;
    private final UpAction upAction;

    /**
     * Set up data structures, but don't DO anything in the constructor.
     */
    public Admiral(AdmiralOptions admiralOptions, DockerModelController dockerModelController, ComposeConfig composeConfig) {
        this.dmc = dockerModelController;
        this.dmc.setProgressHandler(this);
        this.composeConfig = composeConfig;
        this.bounceAction = new BounceAction(this, dmc, composeConfig);
        this.createAction = new CreateAction(this, dmc, composeConfig);
        this.downAction = new DownAction(this, dmc, composeConfig);
        this.restartAction = new RestartAction(this, dmc, composeConfig);
        this.rmAction = new RmAction(this, dmc, composeConfig);
        this.startAction = new StartAction(this, dmc, composeConfig);
        this.stopAction = new StopAction(this, dmc, composeConfig);
        this.upAction = new UpAction(this, dmc, composeConfig);
    }

    public String getProjectName() {
        return composeConfig.getProjectName();
    }

    public Collection<String> getContainerNames() {
        return dmc.getContainerNames();
    }

    public Collection<String> getServiceNames() {
        return composeConfig.getServiceNames();
    }

    private List<AdmiralServiceConfig> expandGroups(List<String> serviceOrGroupNames) throws AdmiralServiceConfigNotFoundException {
        return composeConfig.getGroupEngine().expandGroups(serviceOrGroupNames);
    }

    public Collection<String> getServiceAndServiceGroupNames() {
        Set<String> all = new HashSet<>();
        all.addAll(composeConfig.getServiceNames());
        all.addAll(composeConfig.getGroupEngine().getServiceGroupNames());
        return all;
    }

    public Collection<AdmiralServiceConfig> getAdmiralServiceConfigs() throws AdmiralServiceConfigNotFoundException {
        return composeConfig.getServices();
    }

    public String calculateContainerName(String serviceName, int i) {
        return composeConfig.calculateContainerName(serviceName, i);
    }

    public Collection<String> getEnvironmentVariableNames() {
        Collection<String> names = new HashSet<>();
        Map<String, ConfigVariableProcessor.Entry> data = composeConfig.getData();
        for (Map.Entry<String, ConfigVariableProcessor.Entry> e : data.entrySet()) {
            ConfigVariableProcessor.Entry entry = e.getValue();
            final String key = entry.getKey();
            names.add(key);
        }
        return names;
    }

    public Collection<ContainerParameterProcessor.Entry> getEnvironmentVariables(String serviceName) {
        AdmiralServiceConfig admiralServiceConfig = composeConfig.getServiceConfig(serviceName);
        if (admiralServiceConfig==null) {
            return Collections.emptyList();
        }

        return admiralServiceConfig.getEnvironmentVariables().values();
    }

    public void showInitialState() {
        publish(l -> l.showInitialState(composeConfig));
    }

    public void threadsShutdown() {
        dmc.shutdownLogStreamers();
    }

    @Override
    public void publish(Consumer<AdmiralEventListener> event) {
        dmc.publish(event);
    }

    public boolean isConnected() {
        return dmc.isConnected();
    }

    public void createACT(List<String> serviceNames)
        throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, IOException, InterruptedException
    {
        createAction.perform(serviceNames);
    }

    private void connectIfNecessary()
            throws AdmiralDockerException {
        dmc.connectIfNecessary();
    }

    public void networkCreateACT()
            throws AdmiralDockerException {
        final ActionHarness actionHarness = new ActionHarness("network", Collections.singletonList("create"));
        connectIfNecessary();
        dmc.createNetworks(composeConfig.getNetworks());
        actionDone(actionHarness);
    }

    public void networkRmACT()
            throws AdmiralDockerException {
        final ActionHarness actionHarness = new ActionHarness("network", Collections.singletonList("rm"));
        connectIfNecessary();
        dmc.removeNetworks(composeConfig.getNetworks());
        actionDone(actionHarness);
    }

    public void startACT(List<String> serviceNames)
        throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException
    {
        startAction.perform(serviceNames);
    }

    public void restartACT(List<String> serviceNames)
        throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException
    {
        restartAction.perform(serviceNames);
    }

    public void bounceACT(List<String> serviceNames)
        throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException
    {
        bounceAction.perform(serviceNames);
    }

    public void upACT(List<String> serviceNames)
        throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException
    {
        upAction.perform(serviceNames);
    }

    public void waitACT(List<String> serviceNames)
            throws AdmiralServiceConfigNotFoundException, AdmiralDockerException {
        final Collection<AdmiralServiceConfig> services;
        final ActionHarness actionHarness = new ActionHarness("wait", serviceNames);

        if (serviceNames.isEmpty()) {
            services = composeConfig.getServicesCopy();
            services.removeIf(c -> c.getCommands().wait != Commands.Binding.AUTO);
        } else {
            services = expandGroups(serviceNames);
            services.removeIf(c -> c.getCommands().wait == Commands.Binding.NEVER);
        }

        connectIfNecessary();
        waitServices(services);
        actionDone(actionHarness);
    }

    public void rmACT(List<String> serviceNames)
            throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException
    {
        rmAction.perform(serviceNames);
    }

    private void actionDone(ActionHarness actionHarness) {
        actionHarness.getTimer().stop();
        publish(l -> l.resetProgress(actionHarness));
    }

    private void waitServices(Collection<AdmiralServiceConfig> services)
            throws AdmiralDockerException {
        for (AdmiralServiceConfig admiralServiceConfig : services) {
            waitService(admiralServiceConfig);
        }
    }

    private void waitService(AdmiralServiceConfig admiralServiceConfig)
            throws AdmiralDockerException {
        // Gather
        final String serviceName = admiralServiceConfig.getName();

        final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
        for (int i = 1; i <= replicas; i++) {
            final String containerName = composeConfig.calculateContainerName(serviceName, i);
            final boolean running = dmc.isContainerRunning(containerName);
            if (running) {
                dmc.containerWait(admiralServiceConfig, containerName, true);
            }
        }
    }

    public void configACT(OutputWriter outputWriter) {
        ConfigACT configACT = new ConfigACT(composeConfig, outputWriter);
        configACT.perform();
    }

    public void groupsACT(OutputStyler outputStyler, OutputWriter outputWriter) {
        GroupsACT groupsACT = new GroupsACT(composeConfig, outputStyler, outputWriter);
        groupsACT.perform();
    }

    public void showPreferencesACT(UXPreferences uxPreferences, OutputWriter outputWriter) {
        ShowPreferencesACT showPreferencesACT = new ShowPreferencesACT(uxPreferences, outputWriter);
        showPreferencesACT.perform();
    }

    public void commandsACT(OutputStyler outputStyler, OutputWriter outputWriter) {
        CommandsACT commandsACT = new CommandsACT(composeConfig, outputStyler, outputWriter);
        commandsACT.perform();
    }

    public void envACT(OutputPreferences outputPreferences, OutputStyler outputStyler, OutputWriter outputWriter) {
        EnvACT envACT = new EnvACT(composeConfig,outputPreferences, outputStyler, outputWriter);
        envACT.perform();
    }

    public void envACT(OutputPreferences outputPreferences, OutputStyler outputStyler, OutputWriter outputWriter, String serviceName) {
        EnvACT envACT = new EnvACT(composeConfig,outputPreferences, outputStyler, outputWriter);
        envACT.perform(serviceName);
    }

    public void envACT(OutputPreferences outputPreferences, OutputStyler outputStyler, OutputWriter outputWriter, String serviceName, String[] vars) {
        EnvACT envACT = new EnvACT(composeConfig,outputPreferences, outputStyler, outputWriter);
        envACT.perform(serviceName, vars);
    }

    public void digACT(OutputPreferences outputPreferences, OutputStyler outputStyler, OutputWriter outputWriter, Collection<String> serviceNames, boolean showAll) throws AdmiralServiceConfigNotFoundException {
        DigACT digACT = new DigACT(this, composeConfig, outputPreferences, outputStyler, outputWriter);
        digACT.perform(serviceNames, showAll);
    }

    public void inspectACT(OutputPreferences outputPreferences, OutputStyler outputStyler, OutputWriter outputWriter, Collection<String> serviceNames) throws AdmiralServiceConfigNotFoundException {
        InspectACT inspectACT = new InspectACT(this, composeConfig, outputPreferences, outputStyler, outputWriter);
        inspectACT.perform(serviceNames);
    }

    public void psACT(OutputPreferences outputPreferences, OutputStyler outputStyler, OutputWriter outputWriter, Collection<String> serviceNames) throws AdmiralServiceConfigNotFoundException {

        ShowComposeACT showcomposeACT = new ShowComposeACT(this, composeConfig, outputPreferences, outputStyler, outputWriter);
        showcomposeACT.perform(serviceNames);
    }

    public void setACT(OutputPreferences outputPreferences, OutputStyler outputStyler, OutputWriter outputWriter) {
        SetACT setACT = new SetACT(composeConfig, outputPreferences, outputStyler, outputWriter);
        setACT.perform();
    }

    public void setACT(OutputPreferences outputPreferences, OutputStyler outputStyler, OutputWriter outputWriter, String var) {
        SetACT setACT = new SetACT(composeConfig, outputPreferences, outputStyler, outputWriter);
        setACT.perform(var);
    }

    public void showcomposeACT(OutputPreferences outputPreferences, OutputStyler outputStyler, OutputWriter outputWriter, Collection<String> serviceNames) throws AdmiralServiceConfigNotFoundException {
        ShowComposeACT showcomposeACT = new ShowComposeACT(this, composeConfig, outputPreferences, outputStyler, outputWriter);
        showcomposeACT.perform(serviceNames);
    }

    public void showparametersACT(OutputPreferences outputPreferences, OutputStyler outputStyler, OutputWriter outputWriter, Collection<String> serviceNames) throws AdmiralServiceConfigNotFoundException {
        ShowParametersACT showparametersACT = new ShowParametersACT(composeConfig, outputPreferences, outputStyler, outputWriter);
        showparametersACT.perform(serviceNames);
    }

    public void showconfigACT(OutputPreferences outputPreferences, OutputStyler outputStyler, OutputWriter outputWriter) {
        ShowConfigACT showconfigACT = new ShowConfigACT(composeConfig, outputPreferences, outputStyler, outputWriter);
        showconfigACT.perform();
    }

    public void joinServicesACT(List<String> serviceNames)
            throws AdmiralServiceConfigNotFoundException, AdmiralDockerException, InterruptedException {
        final Collection<AdmiralServiceConfig> services;
        final ActionHarness actionHarness = new ActionHarness("join", serviceNames);

        if (serviceNames.isEmpty()) {
            services = composeConfig.getServicesCopy();
            services.removeIf(c -> c.getCommands().join != Commands.Binding.AUTO);
        } else {
            services = expandGroups(serviceNames);
            services.removeIf(c -> c.getCommands().join == Commands.Binding.NEVER);
        }

        connectIfNecessary();
        dmc.joinServices(services);
        actionDone(actionHarness);
    }

    public void unjoinServicesACT(List<String> serviceNames)
            throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException {
        final Collection<AdmiralServiceConfig> services;
        final ActionHarness actionHarness = new ActionHarness("unjoin", serviceNames);

        if (serviceNames.isEmpty()) {
            services = composeConfig.getServicesCopy();
            services.removeIf(c -> c.getCommands().join != Commands.Binding.AUTO);
        } else {
            services = expandGroups(serviceNames);
            services.removeIf(c -> c.getCommands().join == Commands.Binding.NEVER);
        }

        // Guard
        // If we aren't connected, there is nothing to do.
        if (!dmc.isConnected())
            return;

        // Go
        dmc.unjoinServices(services);
        // Close our connection to the Docker Engine so that the ContainerLogStreamer threads exit.
        dmc.disconnectFromDockerEngine();
        actionDone(actionHarness);
    }

    public void stopACT(List<String> serviceNames)
            throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException {
        stopAction.perform(serviceNames);
    }

    public void testACT(OutputWriter outputWriter, List<String> args)
            throws AdmiralDockerException, InterruptedException {
        TestACT testACT = new TestACT(this, composeConfig, outputWriter);
        testACT.perform(args);
    }

    public void downACT(List<String> serviceNames)
            throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException {
        downAction.perform(serviceNames);
    }

    public String pingACT()
            throws AdmiralDockerException {
        connectIfNecessary();
        return dmc.ping();
    }

    public Version versionACT()
            throws AdmiralDockerException {
        connectIfNecessary();
        return dmc.version();
    }

    public Info infoACT()
            throws AdmiralDockerException {
        connectIfNecessary();
        return dmc.info();
    }

    public Collection<ContainerController> inspectContainersACT()
            throws AdmiralDockerException {
        connectIfNecessary();
        return dmc.inspectContainers();
    }

    public Map<String, String> getContainerEnvironmentVariablesACT(String containerName)
            throws AdmiralDockerException, InterruptedException {
        connectIfNecessary();
        return dmc.getContainerEnvironmentVariables(containerName);
    }

    public List<Container> listContainersACT()
            throws AdmiralDockerException {
        connectIfNecessary();
        return dmc.listContainers();
    }

    public void listACT(OutputStyler outputStyler, OutputWriter outputWriter)
            throws AdmiralDockerException {
        connectIfNecessary();
        ListACT listACT = new ListACT(this, dmc, outputStyler, outputWriter);
        listACT.perform();
    }

    public void psACT(OutputStyler outputStyler, OutputWriter outputWriter)
            throws AdmiralDockerException {
        connectIfNecessary();
        PsACT psACT = new PsACT(this, dmc, outputStyler, outputWriter);
        psACT.perform();
    }

    public void servicesACT(OutputStyler outputStyler, OutputWriter outputWriter)
            throws AdmiralServiceConfigNotFoundException, AdmiralDockerException {
        connectIfNecessary();
        ServicesACT servicesACT = new ServicesACT(this, dmc, outputStyler, outputWriter);
        servicesACT.perform();
    }


    public List<Image> listImagesACT()
            throws AdmiralDockerException {
        connectIfNecessary();
        return dmc.listImages();
    }

    /**
     * Convenience Method
     */
    public String matchAdmiralContainerName(Container container) {
        return dmc.matchAdmiralContainerName(container);
    }

    public List<String> getRunningContainerNameListACT()
            throws AdmiralDockerException  {
        connectIfNecessary();
        List<String> runningContainers = new ArrayList<>();
        for (AdmiralServiceConfig admiralServiceConfig : composeConfig.getServicesOrEmpty()) {
            final String name = admiralServiceConfig.getName();
            final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
            for (int i = 1; i <= replicas; i++) {
                final String containerName = composeConfig.calculateContainerName(name, i);
                final boolean running = dmc.isContainerRunning(containerName);
                if (running) {
                    runningContainers.add(containerName);
                }
            }
        }

        return runningContainers;
    }

    public void disconnectFromDockerEngineACT() throws AdmiralDockerException {
        final ActionHarness actionHarness = new ActionHarness("disconnect", Collections.emptyList());
        dmc.disconnectFromDockerEngine();
        actionDone(actionHarness);
    }

    @Override
    public void progress(ProgressMessage progressMessage) {
        publish(l -> l.imagePullingProgressMessage(progressMessage));
    }
}
