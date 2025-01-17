package com.optum.admiral.event;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.optum.admiral.config.ActionProgress;
import com.optum.admiral.config.AdmiralContainerConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.exception.AdmiralNetworkHasActiveEndpointsException;
import com.optum.admiral.io.LogStreamer;
import com.optum.admiral.logging.ActionHarness;
import com.optum.admiral.model.DockerModelController;
import com.optum.admiral.model.ProgressMessage;

import java.io.File;
import java.net.URL;
import java.util.List;

public interface AdmiralEventListener {
    void containerAttachedToStream(LogStreamer LogStreamer);
    void containerCopiedFileTo(String containerName, String source, String target);
    void containerCopingFileTo(String containerName, String source, String target);
    void containerCopingFileToFailed(String containerName, String source, String target);
    void containerCreated(String containerName, CreateContainerResponse createContainerResponse, AdmiralContainerConfig admiralContainerConfig);
    void containerCreating(String containerName);
    void containerCreatingFailed(String containerName);
    void containerJoined(String containerName);
    void containerJoining(String containerName);
    void containerMountDenied(String containerName, String serviceName, String mountName);
    void containerNotFound(String containerName, boolean isWarning);
    void containerRemoved(String containerName, boolean skipped);
    void containerRemoving(String containerName);
    void containerStarted(String containerName, boolean skipped);
    void containerStarting(DockerModelController dockerModelController, String containerName, AdmiralContainerConfig admiralContainerConfig);
    void containerStartingFailed(String containerName);
    void containerStopped(String containerName, boolean skipped);
    void containerStopping(String containerName);

    void dependentServiceStarting(String serviceName);
    void dependentServiceStarted(String serviceName);

    void debugIsRunningBegin(String containerName);
    void debugIsRunningEnd(String containerName);
    void debugAttachToContainerBegin(String containerName, String streamName);
    void debugAttachToContainerEnd(String containerName, String streamName);
    void debugAttachToLogBegin(String containerName, String logName);
    void debugAttachToLogEnd(String containerName, String logName);

    void dockerEngineConnected();
    void dockerEngineConnecting();
    void dockerEngineDisconnected();

    void imagePulling(String imageName);
    void imagePullingProgressMessage(ProgressMessage progressMessage);

    void networkCreated(String networkFullName, CreateNetworkResponse createNetworkResponse, boolean skipped);
    void networkCreating(String networkFullName);
    void networkNotFound(String networkFullName);
    void networkRemoved(String networkFullName, boolean skipped);
    void networkRemoveFailed(String networkFullName, AdmiralNetworkHasActiveEndpointsException e);
    void networkRemoving(String networkFullName);

    void serviceCreated(String serviceName);
    void serviceCreating(String serviceName);
    void serviceJoined(String serviceName);
    void serviceJoining(String serviceName);
    void serviceNotFound(String serviceName);
    void serviceRemoved(String serviceName);
    void serviceRemoving(String serviceName);
    void serviceStarted(String serviceName);
    void serviceStarting(String serviceName);
    void serviceStopped(String serviceName);
    void serviceStopping(String serviceName);

    void loadingAdmiralConfiguration(File file);
    void loadingComposeConfiguration(File file);
    void loadingEnvironmentVariables(File file);
    void loadingSystemConfigurationVariables();
    void loadingConfigurationVariables(URL url);
    void loadingConfigurationVariables(File file);

    void loadedAdmiralConfiguration(File file);
    void loadedAdmiralPreferences(File file);
    void noAdmiralPreferencesFileFound(List<File> filesSearched);
    void loadedComposeConfiguration(File file);
    void loadedEnvironmentVariables(File file);
    void loadedSystemConfigurationVariables();
    void loadedConfigurationVariables(URL url);
    void loadedConfigurationVariables(File file);

    void verifiedMarkerFile(String name, File file);

    // Progress Events
    void addLine(String containerName, String streamName, String line);
    void waitProgressMessage(String url, String status, String progress);
    void waitProgressMessage(int current, int total, String url, String status, String progress);
    void executeHookStart(String cmdId, String s);
    void executeHookStdoutLine(String cmdId, String s);
    void executeHookStderrLine(String cmdId, String s);
    void executeHookDone(String cmdId, String s);
    void resetProgress(ActionHarness actionHarness);
    void logStreamProgress(String containerName, String streamName, ActionProgress actionProgress);

    // Error Events
    void localImageNotFound(String imageName);
    void invalidReferenceFormat(AdmiralContainerConfig admiralContainerConfig);
    void pulledImageNotFound(String imageName);
    void pulledImageManifestNotFound(String manifestName);
    void pulledImageAccessDenied();
    void hostNotFoundTryingToPullImage(String hostName);
    void expectedWarning(String message);
    void warning(String message);
    void error(String message);
    void verbose(String message);
    void debug(String message);
    void unhandledException(Throwable e);

    // Notification Events
    void deletedFile(File file);

    // Status Events
    void showInitialState(ComposeConfig composeConfig);
}
