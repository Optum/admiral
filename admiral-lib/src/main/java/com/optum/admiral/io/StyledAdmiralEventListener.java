package com.optum.admiral.io;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.optum.admiral.config.ActionProgress;
import com.optum.admiral.config.AdmiralContainerConfig;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.event.AdmiralEventListener;
import com.optum.admiral.exception.AdmiralNetworkHasActiveEndpointsException;
import com.optum.admiral.logging.ActionHarness;
import com.optum.admiral.model.DockerModelController;
import com.optum.admiral.model.ProgressMessage;
import com.optum.admiral.preferences.OutputPreferences;

import java.io.File;
import java.net.URL;
import java.util.List;

import static com.optum.admiral.io.StyledAdmiralEventListener.ActionState.BEGIN;
import static com.optum.admiral.io.StyledAdmiralEventListener.ActionState.DONE;
import static com.optum.admiral.io.StyledAdmiralEventListener.ActionState.SKIPPED;
import static com.optum.admiral.io.StyledAdmiralEventListener.ActionState.FAILED;
import static com.optum.admiral.io.StyledAdmiralEventListener.ActionType.CREATING;
import static com.optum.admiral.io.StyledAdmiralEventListener.ActionType.JOINING;
import static com.optum.admiral.io.StyledAdmiralEventListener.ActionType.LOADING;
import static com.optum.admiral.io.StyledAdmiralEventListener.ActionType.REMOVING;
import static com.optum.admiral.io.StyledAdmiralEventListener.ActionType.STARTING;
import static com.optum.admiral.io.StyledAdmiralEventListener.ActionType.STOPPING;
import static com.optum.admiral.io.StyledAdmiralEventListener.DataType.ADMIRALFILE;
import static com.optum.admiral.io.StyledAdmiralEventListener.DataType.COMPOSEFILE;
import static com.optum.admiral.io.StyledAdmiralEventListener.DataType.CONFIGFILE;
import static com.optum.admiral.io.StyledAdmiralEventListener.DataType.CONFIGOS;
import static com.optum.admiral.io.StyledAdmiralEventListener.DataType.CONFIGURL;
import static com.optum.admiral.io.StyledAdmiralEventListener.DataType.CONTAINER;
import static com.optum.admiral.io.StyledAdmiralEventListener.DataType.CONTAINERVARIABLES;
import static com.optum.admiral.io.StyledAdmiralEventListener.DataType.NETWORK;
import static com.optum.admiral.io.StyledAdmiralEventListener.DataType.PREFERENCESFILE;
import static com.optum.admiral.io.StyledAdmiralEventListener.DataType.SERVICE;

public class StyledAdmiralEventListener
        implements AdmiralEventListener {

    private final OutputPreferences preferences;
    private final OutputStyler styler;
    private final OutputWriter writer;
    private final OutputStyle commandStyle;
    private final VariableWriterUtil variableWriterUtil;

    public enum ActionType {
        STARTING("Starting"),
        CREATING ("Creating"),
        JOINING("Joining"),
        REMOVING("Removing"),
        STOPPING("Stopping"),
        LOADING("Loading");
        private final String text;
        ActionType(String text) {
            this.text = text;
        }
        @Override
        public String toString() {
            return text;
        }
    }

    public enum DataType {
        CONTAINER("Container"),
        SERVICE("Service"),
        NETWORK("Network"),
        ADMIRALFILE("Admiral File"),
        COMPOSEFILE("Compose File"),
        CONFIGURL("Config URL"),
        CONFIGFILE("Config File"),
        CONFIGOS("Config OS"),
        CONTAINERVARIABLES("container variables from"),
        PREFERENCESFILE("Preferences File");
        DataType(String prompt) {
            this.prompt = prompt;
        }
        private final String prompt;
        private OutputStyle outputStyle;
        private void setOutputStyle(OutputStyle outputStyle) {
            this.outputStyle = outputStyle;
        }
        @Override
        public String toString() {
            return prompt;
        }
        public String format(String item) {
            return outputStyle.format(item);
        }
    }

    public enum ActionState {
        BEGIN("..."),
        SKIPPED("skipped"),
        DONE("done"),
        FAILED("failed");
        private OutputStyle outputStyle;
        private final String state;
        ActionState(String state) {
            this.state = state;
        }
        private void setOutputStyle(OutputStyle outputStyle) {
            this.outputStyle = outputStyle;
        }
        @Override
        public String toString() {
            return outputStyle.format(state);
        }
    }

    public StyledAdmiralEventListener(OutputStyler outputStyler, OutputPreferences cliPreferences, OutputWriter outputWriter) {
        this.preferences = cliPreferences;
        this.styler = outputStyler;
        this.writer = outputWriter;
        this.variableWriterUtil = new VariableWriterUtil(cliPreferences, outputStyler, outputWriter);
        this.commandStyle = styler.command;

        CONTAINER.setOutputStyle(styler.container);
        SERVICE.setOutputStyle(styler.service);
        NETWORK.setOutputStyle(styler.network);
        ADMIRALFILE.setOutputStyle(styler.file);
        COMPOSEFILE.setOutputStyle(styler.file);
        CONFIGURL.setOutputStyle(styler.url);
        CONFIGFILE.setOutputStyle(styler.file);
        CONFIGOS.setOutputStyle(styler.system);
        CONTAINERVARIABLES.setOutputStyle(styler.file);

        BEGIN.setOutputStyle(styler.plain);
        SKIPPED.setOutputStyle(styler.warning);
        DONE.setOutputStyle(styler.command);
        FAILED.setOutputStyle(styler.error);
    }

    /**
     * Used by all the created methods.
     */
    private void showIDAndWarnings(String type, String id, List<String> warnings) {
        if (preferences.showIDs) {
            writer.outln(styler.log.format(type + " ID: ") + styler.container.format(id));
        }
        if (!warnings.isEmpty()) {
            for(String warning : warnings) {
                if (warning!=null && !warning.isEmpty())
                    writer.outln(styler.warning.format("Docker Engine WARNING: " + warning));
            }
        }
    }

    public void actionEvent(ActionType actionType, DataType dataType, ActionState actionState, String itemName) {
        final String key = actionType+":"+itemName;
        final String line = commandStyle.format(actionType.toString() + " " + dataType.toString() + ": ") + dataType.format(itemName) + " " + actionState;

        writer.outTrackedLine(key, line);
    }

    @Override
    public void containerAttachedToStream(LogStreamer logStreamer) {
        AdmiralLogStreamerListener admiralLogStreamerListener = new AdmiralLogStreamerListener(preferences, logStreamer.getContainerName(), this);
        logStreamer.addListener(admiralLogStreamerListener);
    }

    @Override
    public void containerCopiedFileTo(String containerName, String source, String target) {
        writer.outln(styler.command.format("Copied ") + styler.file.format(source) + styler.command.format(" to ")
                + styler.container.format(containerName) + styler.command.format(": ") + styler.file.format(target));
    }

    @Override
    public void containerCopingFileTo(String containerName, String source, String target) {
        if (preferences.showActionAnnouncements)
            writer.outln(styler.log.format("Coping ") + styler.file.format(source) + styler.log.format(" to ")
                    + styler.container.format(containerName) + styler.log.format(": ") + styler.file.format(target));
    }

    @Override
    public void containerCopingFileToFailed(String containerName, String source, String target) {
        if (preferences.showActionAnnouncements)
            writer.outln(styler.error.format("ERROR: Coping ") + styler.file.format(source) + styler.error.format(" to ")
                    + styler.container.format(containerName) + styler.error.format(": ") + styler.file.format(target) + styler.error.format(" failed"));
    }

    @Override
    public void containerCreated(String containerName, CreateContainerResponse createContainerResponse, AdmiralContainerConfig admiralContainerConfig) {
        actionEvent(CREATING, CONTAINER, DONE, containerName);
    }

    @Override
    public void containerCreating(String containerName) {
        actionEvent(CREATING, CONTAINER, BEGIN, containerName);
    }

    @Override
    public void containerCreatingFailed(String containerName) {
        actionEvent(CREATING, CONTAINER, FAILED, containerName);
    }

    @Override
    public void containerJoined(String containerName) {
        writer.outln(styler.command.format("Joined Container: ") + styler.container.format(containerName));
    }

    @Override
    public void containerJoining(String containerName) {
        if (preferences.showActionAnnouncements)
            writer.outln(styler.command.format("Joining Container: ") + styler.container.format(containerName));
    }

    @Override
    public void containerMountDenied(String containerName, String serviceName, String mountName) {
        writer.outln(styler.error.format("Volume mount not found: " + styler.file.format(mountName)));
        writer.outln(styler.help.format("This means the path listed above was not found (or inaccessible) on your host machine."));
        writer.outln(styler.help.format("It does not refer to a path in your container."));
        writer.outln(styler.help.format("Is the path relative and you are in the wrong directory?"));
        writer.outln(styler.help.format("Is the path defined with an environment variable that is missing or set incorrectly?"));
        writer.outln(styler.help.format("The command " ) + styler.command.format("showcompose") + styler.help.format(" will show the composed definition of ") + styler.container.format(containerName) + " (" + styler.serviceHeading.format(serviceName) + styler.help.format("), specifically listing the defined ") + styler.subsection.format("Volumes") + styler.help.format("."));
    }

    @Override
    public void containerNotFound(String containerName, boolean isWarning) {
        if (isWarning) {
            if (preferences.showExpectedWarnings)
                writer.outln(styler.warning.format("WARNING: Container ") + styler.container.format(containerName) + styler.error.format(" not found."));
        } else {
            writer.outln(styler.error.format("ERROR: Container ") + styler.container.format(containerName) + styler.error.format(" not found."));
        }
    }

    @Override
    public void networkNotFound(String networkName) {
        writer.outln(styler.error.format("Network not found: ") + styler.network.format(networkName));
        if (networkName.length()==64) {
            writer.outln(styler.help.format("Networks that are used then deleted leave containers still pointing to the original network."));
            writer.outln(styler.help.format("Recreated networks DO NOT reattach containers from the original network."));
            writer.outln(styler.help.format("If you have used and deleted (then recreated) a network, you probably need to ") + styler.command.format("bounce") + styler.help.format(" your containers."));
        } else {
            writer.outln(styler.help.format("To create all your networks, use the command: ") + styler.command.format("network create"));
        }
    }

    @Override
    public void containerRemoved(String containerName, boolean skipped) {
        actionEvent(REMOVING, CONTAINER, skipped ? SKIPPED : DONE, containerName);
    }

    @Override
    public void containerRemoving(String containerName) {
        actionEvent(REMOVING, CONTAINER, BEGIN, containerName);
    }

    @Override
    public void containerStarted(String containerName, boolean skipped) {
        actionEvent(STARTING, CONTAINER, skipped ? SKIPPED : DONE, containerName);
    }

    @Override
    public void containerStarting(DockerModelController dockerModelController, String containerName, AdmiralContainerConfig admiralContainerConfig) {
        variableWriterUtil.writeContainerVariables(dockerModelController, containerName, admiralContainerConfig);
        actionEvent(STARTING, CONTAINER, BEGIN, containerName);
    }

    @Override
    public void containerStartingFailed(String containerName) {
        actionEvent(STARTING, CONTAINER, FAILED, containerName);
    }

    @Override
    public void containerStopped(String containerName, boolean skipped) {
        actionEvent(STOPPING, CONTAINER, skipped ? SKIPPED : DONE, containerName);
    }

    @Override
    public void containerStopping(String containerName) {
        actionEvent(STOPPING, CONTAINER, BEGIN, containerName);
    }


    @Override
    public void dependentServiceStarted(String serviceName) {
    }

    @Override
    public void debugIsRunningBegin(String containerName) {
        if (preferences.showDebug)
            writer.outln(styler.debug.format("DEBUG Is Running Begin: ") + styler.container.format(containerName));
    }

    @Override
    public void debugIsRunningEnd(String containerName) {
        if (preferences.showDebug)
            writer.outln(styler.debug.format("DEBUG Is Running End: ") + styler.container.format(containerName));
    }

    @Override
    public void debugAttachToContainerBegin(String containerName, String streamName) {
        if (preferences.showDebug)
            writer.outln(styler.debug.format("DEBUG Attach To Container Begin: ") + styler.container.format(containerName) + ":" + styler.file.format(streamName));
    }

    @Override
    public void debugAttachToContainerEnd(String containerName, String streamName) {
        if (preferences.showDebug)
            writer.outln(styler.debug.format("DEBUG Attach To Container End: ") + styler.container.format(containerName) + ":" + styler.file.format(streamName));
    }

    @Override
    public void debugAttachToLogBegin(String containerName, String logName) {
        if (preferences.showDebug)
            writer.outln(styler.debug.format("DEBUG Attach To Log Begin: ") + styler.container.format(containerName) + ":" + styler.file.format(logName));
    }

    @Override
    public void debugAttachToLogEnd(String containerName, String logName) {
        if (preferences.showDebug)
            writer.outln(styler.debug.format("DEBUG Attach To Log End: ") + styler.container.format(containerName) + ":" + styler.file.format(logName));
    }

    @Override
    public void dependentServiceStarting(String serviceName) {
    }

    @Override
    public void invalidReferenceFormat(AdmiralContainerConfig admiralContainerConfig) {
        writer.outln(styler.error.format("ERROR: Invalid Reference Format."));
        writer.outln(styler.help.format("This can happen when trying to create a container with a blank or invalid image name."));
        if (admiralContainerConfig.getImage().length()==0) {
            writer.outln(styler.help.format("The image name for this container is blank."));
        } else if (":".equals(admiralContainerConfig.getImage())) {
            writer.outln(styler.help.format("The image name for this container is effectively blank. (") + styler.image.format(":") + styler.help.format(")"));
        } else {
            writer.outln(styler.help.format("The image name for this container is: ") + styler.image.format(admiralContainerConfig.getImage()));
        }
        writer.outln(styler.help.format("The command ") + styler.command.format("showcompose " + admiralContainerConfig.getName()) + styler.help.format(" will show the composed service details."));
    }

    @Override
    public void localImageNotFound(String imageName) {
        writer.outln(styler.warning.format("WARNING: Local Image " + styler.image.format(imageName) + styler.warning.format(" not found.")));
    }

    @Override
    public void pulledImageNotFound(String imageName) {
        writer.outln(styler.error.format("ERROR: Pulling Image " + styler.image.format(imageName) + styler.error.format(" failed because it could not found.")));
        writer.outln(styler.help.format("This specific variation of the error is likely due to a bad image name."));
    }

    @Override
    public void pulledImageManifestNotFound(String manifestName) {
        writer.outln(styler.error.format("ERROR: Pulling Image " + styler.image.format(manifestName) + styler.error.format(" failed because it could not found.")));
        writer.outln(styler.help.format("This specific variation of the error is likely due to a bad image tag."));
    }

    @Override
    public void pulledImageAccessDenied() {
        writer.outln(styler.error.format("ERROR: Pulling Image Access Denied.  Either you are not authorized, or you may need to perform a \"docker login\" to the host."));
    }

    @Override
    public void hostNotFoundTryingToPullImage(String hostName) {
        writer.outln(styler.error.format("ERROR: Image could not be pulled.  Host " + styler.url.format(hostName) + styler.error.format(" could not be found.  Is the host wrong or temporarily unavailable?")));
    }

    @Override
    public void imagePulling(String imageName) {
        writer.outln(styler.command.format("Pulling: " + styler.image.format(imageName)));
    }

    @Override
    public void imagePullingProgressMessage(ProgressMessage progressMessage) {
        writer.progress(progressMessage);
    }

    @Override
    public void serviceCreated(String serviceName) {
        if (!preferences.showServiceHeadings)
            return;

        actionEvent(CREATING, SERVICE, DONE, serviceName);
    }

    @Override
    public void serviceCreating(String serviceName) {
        if (!preferences.showServiceHeadings)
            return;

        actionEvent(CREATING, SERVICE, BEGIN, serviceName);
    }

    @Override
    public void serviceJoined(String serviceName) {
        if (!preferences.showServiceHeadings)
            return;

        actionEvent(JOINING, SERVICE, DONE, serviceName);
    }

    @Override
    public void serviceJoining(String serviceName) {
        if (!preferences.showServiceHeadings)
            return;

        actionEvent(JOINING, SERVICE, BEGIN, serviceName);
    }

    @Override
    public void serviceNotFound(String serviceName) {
        writer.outln(styler.error.format("ERROR: Service ") + styler.service.format(serviceName) + styler.error.format(" not found."));
    }

    @Override
    public void serviceRemoved(String serviceName) {
        if (!preferences.showServiceHeadings)
            return;

        actionEvent(REMOVING, SERVICE, DONE, serviceName);
    }

    @Override
    public void serviceRemoving(String serviceName) {
        if (!preferences.showServiceHeadings)
            return;

        actionEvent(REMOVING, SERVICE, BEGIN, serviceName);
    }

    @Override
    public void serviceStarted(String serviceName) {
        if (!preferences.showServiceHeadings)
            return;

        actionEvent(STARTING, SERVICE, DONE, serviceName);
    }

    @Override
    public void serviceStarting(String serviceName) {
        if (!preferences.showServiceHeadings)
            return;

        actionEvent(STARTING, SERVICE, BEGIN, serviceName);
    }

    @Override
    public void serviceStopped(String serviceName) {
        if (!preferences.showServiceHeadings)
            return;

        actionEvent(STOPPING, SERVICE, DONE, serviceName);
    }

    @Override
    public void serviceStopping(String serviceName) {
        if (!preferences.showServiceHeadings)
            return;

        actionEvent(STOPPING, SERVICE, BEGIN, serviceName);
    }

    @Override
    public void deletedFile(File file) {
        writer.outln(styler.command.format("Deleted File: ") + styler.file.format(file.getName()));
    }

    @Override
    public void resetProgress(ActionHarness actionHarness) {
        writer.resetProgress();
        if (preferences.showTimer) {
            long elapsedTime = actionHarness.getTimer().getElapsedTime();
            final String result = preferences.showTimerStyle.format(elapsedTime);
            writer.outln(styler.debug.format("Timer: " + result));
        }
    }

    @Override
    public void logStreamProgress(String containerName, String streamName, ActionProgress actionProgress) {
        writer.progress((int)Math.round(actionProgress.percentageTimeComplete),
                100,
                containerName + ":/" + actionProgress.actionName,
                actionProgress.desc, actionProgress.isLast ? "done" : actionProgress.whenDone());
    }

    @Override
    public void waitProgressMessage(int current, int total, String url, String status, String progress) {
        writer.progress(current, total, url, status, progress);
    }

    @Override
    public void waitProgressMessage(String url, String status, String progress) {
        writer.progress(url, status, progress);
    }

    // **************
    // *** LOADED ***
    // **************
    private void loaded(DataType dataType, String item) {
        if (preferences.showLoadActions)
            actionEvent(LOADING, dataType, DONE, item);
    }

    @Override
    public void loadedAdmiralConfiguration(File file) {
        loaded(ADMIRALFILE, file.getPath());
    }

    @Override
    public void loadedAdmiralPreferences(File file) {
        loaded(PREFERENCESFILE, file.getPath());
    }

    @Override
    public void noAdmiralPreferencesFileFound(List<File> filesSearched) {
        if (preferences.showLoadActions) {
            writer.outln(styler.command.format("Preferences File not found; using defaults.  Files searched:"));
            for(File file : filesSearched) {
                writer.outln("  " + styler.file.format(file.getAbsolutePath()));
            }
        }
    }

    @Override
    public void verifiedMarkerFile(String name, File file) {
        if (preferences.showLoadActions) {
            writer.outln(styler.command.format("Verified marker file:") + " " + styler.heading.format(name) + "=" + styler.file.format(file.getAbsolutePath()));
        }
    }

    @Override
    public void loadedComposeConfiguration(File file) {
        loaded(COMPOSEFILE, file.getPath());
    }

    @Override
    public void loadedConfigurationVariables(URL url) {
        loaded(CONFIGURL, url.toString());
    }

    @Override
    public void loadedConfigurationVariables(File file) {
        loaded(CONFIGFILE, file.getPath());
    }

    @Override
    public void loadedEnvironmentVariables(File file) {
        loaded(CONTAINERVARIABLES, file.getPath());
    }

    @Override
    public void loadedSystemConfigurationVariables() {
        loaded(CONFIGOS, "system_environment_variables");
    }

    // ***************
    // *** LOADING ***
    // ***************
    private void loading(DataType dataType, String item) {
        if (preferences.showLoadActions) {
            actionEvent(LOADING, dataType, BEGIN, item);
        }
    }

    @Override
    public void loadingAdmiralConfiguration(File file) {
        loading(ADMIRALFILE, file.getPath());
    }

    @Override
    public void loadingComposeConfiguration(File file) {
        loading(COMPOSEFILE, file.getPath());
    }

    @Override
    public void loadingConfigurationVariables(File file) {
        loading(CONFIGFILE, file.getPath());
    }

    @Override
    public void loadingConfigurationVariables(URL url) {
        loading(CONFIGURL, url.toString());
    }

    @Override
    public void loadingEnvironmentVariables(File file) {
        loading(CONTAINERVARIABLES, file.getPath());
    }

    @Override
    public void loadingSystemConfigurationVariables() {
        loading(CONFIGOS, "system_environment_variables");
    }

    @Override
    public void warning(String message) {
        writer.outln(styler.warning.format(message));
    }

    @Override
    public void expectedWarning(String message) {
        writer.outln(styler.warning.format(message));
    }

    @Override
    public void error(String message) {
        writer.outln(styler.error.format(message));
    }

    @Override
    public void verbose(String message) {
        if (preferences.showVerbose)
            writer.outln(styler.verbose.format(message));
    }

    @Override
    public void debug(String message) {
        if (preferences.showDebug)
            writer.outln(styler.debug.format(message));
    }

    @Override
    public void showInitialState(ComposeConfig composeConfig) {
        if (composeConfig.getServicesOrEmpty().isEmpty()) {
            writer.outln(styler.warning.format("No services defined."));
            return;
        }
        writer.outln(styler.section.format("Services Managed by this Admiral Config:"));
        for(AdmiralServiceConfig admiralServiceConfig : composeConfig.getServicesOrEmpty()) {
            writer.outln("  " + styler.service.format(admiralServiceConfig.getName()));
        }
    }

    @Override
    public void unhandledException(Throwable e) {
        writer.outln("Admiral received an exception that it was not expecting.  Hopefully the stack trace is useful.");
        writer.outln("If you think this is a condition that should be expected and handled, please share.");
        writer.outln("Exception class: " + e.getClass().toString());
        writer.outln("Exception message: " + e.getMessage());
        writer.outStackTrace(e);
    }

    @Override
    public void executeHookStart(String cmdId, String s) {
        writer.progress(cmdId, "Starting", styler.log.format(s));
    }

    @Override
    public void addLine(String containerName, String streamName, String line) {
        final String lineToPrint;
        if (line.endsWith("\n")) {
            lineToPrint = line.substring(0, line.length()-1);
        } else {
            lineToPrint = line;
        }
        writer.outln(styler.container.format(containerName) + ": " + styler.file.format(streamName) + ": " + styler.log.format(lineToPrint));
    }

    @Override
    public void executeHookStdoutLine(String cmdId, String s) {
        // Guard
        if (!preferences.showExecuteProgress)
            return;

        writer.progress(cmdId, "Stdout", styler.log.format(s));
    }

    @Override
    public void executeHookStderrLine(String cmdId, String s) {
        // Guard
        if (!preferences.showExecuteProgress)
            return;

        writer.progress(cmdId, "Stderr", styler.error.format(s));
    }

    @Override
    public void executeHookDone(String cmdId, String s) {
        writer.progress(cmdId, "Finished", styler.log.format(s));
    }

    @Override
    public void networkCreating(String networkFullName) {
        actionEvent(CREATING, NETWORK, BEGIN, networkFullName);
    }

    @Override
    public void networkRemoving(String networkFullName) {
        actionEvent(REMOVING, NETWORK, BEGIN, networkFullName);
    }

    @Override
    public void dockerEngineConnected() {
        if (preferences.showDockerEngineConnectionActivity)
            writer.outln(styler.command.format("Connected to Docker Engine"));
    }

    @Override
    public void dockerEngineConnecting() {
        if (preferences.showDockerEngineConnectionActivity)
            writer.outln(styler.command.format("Connecting to Docker Engine"));
    }

    @Override
    public void dockerEngineDisconnected() {
        if (preferences.showDockerEngineConnectionActivity)
            writer.outln(styler.command.format("Disconnected from Docker Engine"));
    }

    @Override
    public void networkCreated(String networkFullName, CreateNetworkResponse createNetworkResponse, boolean skipped) {
        actionEvent(CREATING, NETWORK, skipped ? SKIPPED : DONE, networkFullName);
    }

    @Override
    public void networkRemoved(String networkFullName, boolean skipped) {
        actionEvent(REMOVING, NETWORK, skipped ? SKIPPED : DONE, networkFullName);
    }

    @Override
    public void networkRemoveFailed(String networkFullName, AdmiralNetworkHasActiveEndpointsException e) {
        actionEvent(REMOVING, NETWORK, FAILED, networkFullName);
        writer.outln(styler.error.format("Network Remove Failed: Network Still In Use."));
        writer.outln(styler.help.format("Network name: ") + styler.network.format(e.getNetworkName()));
        writer.outln(styler.help.format("Network id: ") + styler.network.format(e.getNetworkId()));
    }

}
