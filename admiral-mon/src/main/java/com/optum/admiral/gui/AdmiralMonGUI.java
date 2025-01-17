package com.optum.admiral.gui;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.optum.admiral.config.ActionProgress;
import com.optum.admiral.config.AdmiralContainerConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.event.AdmiralEventListener;
import com.optum.admiral.exception.AdmiralNetworkHasActiveEndpointsException;
import com.optum.admiral.io.LogStreamer;
import com.optum.admiral.logging.ActionHarness;
import com.optum.admiral.model.DockerModelController;
import com.optum.admiral.model.ProgressMessage;
import com.optum.admiral.mon.MonitorPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * This class is the Thread-safe public interaction to the Admiral Mon GUI.
 * It takes events and pumps them to the GUI Thread.
 *
 * Why so many empty methods?  We may someday want the GUI to be more than log widows - show status icons and other
 * telemetry.  A bit annoying to have to stub everything, but will be amazingly useful to have them already here when
 * it comes time to implement a more robust GUI.
 */
public class AdmiralMonGUI implements AdmiralEventListener, WindowListener {
    // The AdmiralGUI is invoked from the non-GUI thread, but does all its work on the GUI thread.
    // That means all the state of AdmiralGUI must only be read/written on the GUI thread.
    // The _OnGUI suffix on fields and methods tries to show this point.
    private final MonitorPreferences guiPreferences;
    private JFrame frame_OnGUI;
    private JGlobalView globalView_OnGUI;

    private final Preferences prefs = Preferences.userNodeForPackage(AdmiralMonGUI.class);

    public AdmiralMonGUI(MonitorPreferences guiPreferences) {
        this.guiPreferences = guiPreferences;

        try {
            UIManager.setLookAndFeel(
                    UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        showGUI();
    }

    /**
     * Will show the GUI - first creating it if necessary.
     */
    private void showGUI() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                doShowGUI_OnGUI();
            }
        });
    }

    private String getScreenIdToUse() {
        return prefs.get("DEVICE", null);
    }

    private void setWindowGeometryFromUserPreferences(GraphicsConfiguration graphicsConfiguration) {
        Rectangle rectangle = graphicsConfiguration.getBounds();
        // If this is the first time ever loading the GUI (or if one of the prefs get deleted) we set our dimentions
        // to 75% x 75% centered (centered = offset by 1/8th).
        int width = prefs.getInt("WIDTH", Math.round(rectangle.width*0.75F));
        int height = prefs.getInt("HEIGHT", Math.round(rectangle.height*0.75F));
        int x = prefs.getInt("X", rectangle.x + Math.round(rectangle.width*0.125F));
        int y = prefs.getInt("Y", rectangle.y + Math.round(rectangle.height*0.125F));

        // Make sure we are within the screen's dimensions.
        if (!rectangle.contains(x, y, width, height)) {
            x = Math.max(rectangle.x, x);
            y = Math.max(rectangle.y, y);
            width = Math.min(width, rectangle.width);
            height = Math.min(height, rectangle.height);
        }
        frame_OnGUI.setSize(width,height);
        frame_OnGUI.setLocation(x, y);
    }

    /**
     * Null means use default.
     */
    private GraphicsConfiguration getGraphicsConfiguration(String screenToUse) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        // Start with the default screen.
        GraphicsDevice graphicsDevice = ge.getDefaultScreenDevice();

        // If we are looking for a specific screen, try to find it.
        // If we can't find it, we'll end up using the default from above.
        if (screenToUse!=null) {
            GraphicsDevice[] screens = ge.getScreenDevices();
            for (GraphicsDevice screen : screens) {
                if (screenToUse.equals(screen.getIDstring())) {
                    graphicsDevice = screen;
                    break;
                }
            }
        }

        // Return the configuration for the chosen device.
        return graphicsDevice.getDefaultConfiguration();
    }

    private void saveWindowGeometryToUserPreferences() {
        int width = frame_OnGUI.getWidth();
        int height = frame_OnGUI.getHeight();
        int x = frame_OnGUI.getX();
        int y = frame_OnGUI.getY();
        GraphicsDevice device = frame_OnGUI.getGraphicsConfiguration().getDevice();
        prefs.put("DEVICE", device.getIDstring());
        prefs.putInt("WIDTH", width);
        prefs.putInt("HEIGHT", height);
        prefs.putInt("X", x);
        prefs.putInt("Y", y);
    }

    // Here is the code if we decide to use it.
    private void clearUserPrefererences() {
        prefs.remove("DEVICE");
        prefs.remove("WIDTH");
        prefs.remove("HEIGHT");
        prefs.remove("X");
        prefs.remove("Y");
    }

    private void doShowGUI_OnGUI() {
        if (frame_OnGUI == null) {
            String screenIdToUse = getScreenIdToUse();
            GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration(screenIdToUse);
            frame_OnGUI = new JFrame("Docker Admiral MONitor", graphicsConfiguration);
            frame_OnGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            setWindowGeometryFromUserPreferences(graphicsConfiguration);
            Font font = null;
            Color foreground = null;
            Color background = null;
            if (guiPreferences.logFont!=null && guiPreferences.logFontSize>0) {
                font = new Font(guiPreferences.logFont, Font.PLAIN, guiPreferences.logFontSize);
            }
            if (guiPreferences.logForegroundColor<=0xffffff) {
                foreground = new Color(guiPreferences.logForegroundColor);
            }
            if (guiPreferences.logBackgroundColor<=0xffffff) {
                background = new Color(guiPreferences.logBackgroundColor);
            }
            globalView_OnGUI = new JGlobalView(font, foreground, background);
            frame_OnGUI.getContentPane().add(globalView_OnGUI);
            frame_OnGUI.setVisible(true);
        }
    }

    @Override
    public void windowOpened(WindowEvent e) {
        // Ignore event
    }

    @Override
    public void windowClosing(WindowEvent e) {
        shutdown_OnGUI();
    }

    @Override
    public void windowClosed(WindowEvent e) {
        // Ignore event
    }

    @Override
    public void windowIconified(WindowEvent e) {
        // Ignore event
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        // Ignore event
    }

    @Override
    public void windowActivated(WindowEvent e) {
        // Ignore event
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        // Ignore event
    }

    public void shutdown() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                shutdown_OnGUI();
            }
        });
    }

    private void shutdown_OnGUI() {
        if (frame_OnGUI!=null) {
            saveWindowGeometryToUserPreferences();
            frame_OnGUI.setVisible(false);
            frame_OnGUI.dispose();
        }
    }

    /**
     * Pump event to GUI
     */
    @Override
    public void containerAttachedToStream(LogStreamer logStreamer) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                doAttachedToContainerStream_OnGUI(logStreamer);
            }
        });
    }

    private void doAttachedToContainerStream_OnGUI(LogStreamer logStreamer) {
        if (globalView_OnGUI != null) {
            globalView_OnGUI.createTabForContainerStream_OnGUI(logStreamer);
        }
    }

    @Override
    public void serviceCreating(String serviceName) {
        // Ignore event
    }

    @Override
    public void serviceJoined(String serviceName) {
        // Ignore event
    }

    @Override
    public void serviceRemoving(String serviceName) {
        // Ignore event
    }

    @Override
    public void serviceStarting(String serviceName) {
        // Ignore event
    }

    @Override
    public void serviceJoining(String serviceName) {
        // Ignore event
    }

    @Override
    public void serviceStopping(String serviceName) {
        // Ignore event
    }

    @Override
    public void dependentServiceStarting(String serviceName) {
        // Ignore event
    }

    @Override
    public void containerCreating(String containerName) {
        // Ignore event
    }

    @Override
    public void containerCreatingFailed(String containerName) {
        // Ignore event
    }

    @Override
    public void containerJoined(String containerName) {
        // Ignore event
    }

    @Override
    public void containerStarting(DockerModelController dockerModelController, String containerName, AdmiralContainerConfig admiralContainerConfig) {
        // Ignore event
    }

    @Override
    public void containerStartingFailed(String containerName) {
        // Ignore event
    }


    @Override
    public void containerJoining(String containerName) {
        // Ignore event
    }

    @Override
    public void containerStopping(String containerName) {
        // Ignore event
    }

    @Override
    public void containerRemoving(String containerName) {
        // Ignore event
    }

    private void doRemovingContainer_OnGUI(String containerName) {
        if (globalView_OnGUI != null) {
            globalView_OnGUI.removeTabForContainer_OnGUI(containerName);
        }
    }

    @Override
    public void containerMountDenied(String containerName, String serviceName, String mountName) {
        // Ignore event
    }

        @Override
    public void containerNotFound(String containerName, boolean isWarning) {
        // Ignore event
    }

    @Override
    public void networkNotFound(String networkName) {
        // Ignore event
    }

    @Override
    public void serviceNotFound(String serviceName) {
        // Ignore event
    }

    @Override
    public void invalidReferenceFormat(AdmiralContainerConfig admiralContainerConfig) {
        // Ignore event
    }

    @Override
    public void localImageNotFound(String imageName) {
        // Ignore event
    }

    @Override
    public void pulledImageNotFound(String imageName) {
        // Ignore event
    }

    @Override
    public void pulledImageManifestNotFound(String manifestName) {
        // Ignore event
    }

    @Override
    public void pulledImageAccessDenied() {
        // Ignore event
    }

    @Override
    public void hostNotFoundTryingToPullImage(String hostName) {
    }


    @Override
    public void expectedWarning(String message) {
        // Ignore event
    }

    @Override
    public void imagePulling(String imageName) {
        // Ignore event
    }

    @Override
    public void deletedFile(File file) {
        // Ignore event
    }

    @Override
    public void imagePullingProgressMessage(ProgressMessage progressMessage) {
        // Ignore event
    }

    @Override
    public void waitProgressMessage(String url, String status, String progress) {
        // Ignore event
    }

    @Override
    public void waitProgressMessage(int current, int total, String url, String status, String progress) {
        // Ignore event
    }

    @Override
    public void loadingAdmiralConfiguration(File file) {
        // Ignore event
    }

    @Override
    public void loadingComposeConfiguration(File file) {
        // Ignore event
    }

    @Override
    public void loadingConfigurationVariables(URL url) {
        // Ignore event
    }

    @Override
    public void loadingEnvironmentVariables(File file) {
        // Ignore event
    }

    @Override
    public void loadingSystemConfigurationVariables() {
        // Ignore event
    }

    @Override
    public void loadingConfigurationVariables(File file) {
        // Ignore event
    }

    @Override
    public void warning(String message) {
        // Ignore event
    }

    @Override
    public void error(String message) {
        // Ignore event
    }

    @Override
    public void verbose(String message) {
        // Ignore event
    }

    @Override
    public void debug(String message) {
        // Ignore event
    }

    @Override
    public void showInitialState(ComposeConfig composeConfig) {
        // Ignore event
    }

    @Override
    public void unhandledException(Throwable e) {
        // Ignore event
    }

    @Override
    public void executeHookStart(String cmdId, String s) {
        // Ignore event
    }

    @Override
    public void executeHookStdoutLine(String cmdId, String s) {
        // Ignore event
    }

    @Override
    public void executeHookStderrLine(String cmdId, String s) {
        // Ignore event
    }

    @Override
    public void executeHookDone(String cmdId, String s) {
        // Ignore event
    }

    @Override
    public void resetProgress(ActionHarness actionHarness) {
        // Ignore event
    }

    @Override
    public void logStreamProgress(String containerName, String streamName, ActionProgress actionProgress) {
        // Ignore event
    }

    @Override
    public void containerCopingFileTo(String containerName, String source, String target) {
        // Ignore event
    }

    @Override
    public void containerCopingFileToFailed(String containerName, String source, String target) {
        // Ignore event
    }

    @Override
    public void networkCreating(String networkFullName) {
        // Ignore event
    }

    @Override
    public void networkRemoving(String networkFullName) {
        // Ignore event
    }

    @Override
    public void dockerEngineConnected() {
        // Ignore event
    }

    @Override
    public void dockerEngineConnecting() {
        // Ignore event
    }

    @Override
    public void dockerEngineDisconnected() {
        // Ignore event
    }

    @Override
    public void serviceCreated(String serviceName) {
        // Ignore event
    }

    @Override
    public void serviceRemoved(String serviceName) {
        // Ignore event
    }

    @Override
    public void serviceStarted(String serviceName) {
        // Ignore event
    }

    @Override
    public void serviceStopped(String serviceName) {
        // Ignore event
    }

    @Override
    public void dependentServiceStarted(String serviceName) {
        // Ignore event
    }

    @Override
    public void debugIsRunningBegin(String containerName) {
        // Ignore event
    }

    @Override
    public void debugIsRunningEnd(String containerName) {
        // Ignore event
    }

    @Override
    public void debugAttachToContainerBegin(String containerName, String streamName) {
        // Ignore event
    }

    @Override
    public void debugAttachToContainerEnd(String containerName, String streamName) {
        // Ignore event
    }

    @Override
    public void debugAttachToLogBegin(String containerName, String logName) {
        // Ignore event
    }

    @Override
    public void debugAttachToLogEnd(String containerName, String logName) {
        // Ignore event
    }

    @Override
    public void containerCreated(String containerName, CreateContainerResponse createContainerResponse, AdmiralContainerConfig admiralContainerConfig) {
        // Ignore event
    }

    @Override
    public void containerStarted(String containerName, boolean skipped) {
        // Ignore event
    }

    @Override
    public void containerStopped(String containerName, boolean skipped) {
        // Ignore event
    }

    @Override
    public void containerRemoved(String containerName, boolean skipped) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                doRemovingContainer_OnGUI(containerName);
            }
        });
    }

    @Override
    public void loadedAdmiralConfiguration(File file) {
        // Ignore event
    }

    @Override
    public void loadedAdmiralPreferences(File file) {
        // Ignore event
    }

    @Override
    public void noAdmiralPreferencesFileFound(List<File> filesSearched) {
        // Ignore event
    }

    @Override
    public void verifiedMarkerFile(String name, File file) {
        // Ignore event
    }

    @Override
    public void loadedComposeConfiguration(File file) {
        // Ignore event
    }

    @Override
    public void loadedEnvironmentVariables(File file) {
        // Ignore event

    }

    @Override
    public void loadedSystemConfigurationVariables() {
        // Ignore event
    }

    @Override
    public void loadedConfigurationVariables(URL url) {
        // Ignore event
    }

    @Override
    public void loadedConfigurationVariables(File file) {
        // Ignore event
    }

    @Override
    public void addLine(String containerName, String streamName, String line) {
        // Ignore event
    }

    @Override
    public void containerCopiedFileTo(String containerName, String source, String target) {
        // Ignore event
    }

    @Override
    public void networkCreated(String networkFullName, CreateNetworkResponse createNetworkResponse, boolean skipped) {
        // Ignore event
    }

    @Override
    public void networkRemoved(String networkFullName, boolean skipped) {
        // Ignore event
    }

    @Override
    public void networkRemoveFailed(String networkFullName, AdmiralNetworkHasActiveEndpointsException e) {
        // Ignore event
    }

}
