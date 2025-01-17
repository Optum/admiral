package com.optum.admiral.gui;

import com.optum.admiral.config.ActionProgress;
import com.optum.admiral.io.LogStreamerListener;

import javax.swing.*;

/**
 * UI Thread Dispatching class.  Thread-safe.
 *
 * This class pumps addLine invocations from the LogStreamer thread onto the UI thread.
 */
public class JLogStreamerListener implements LogStreamerListener {
    private StreamViewHandle streamViewHandle;

    public JLogStreamerListener(StreamViewHandle streamViewHandle) {
        this.streamViewHandle = streamViewHandle;
    }

    /**
     * Accepts a line on the LogStream thread and dispatches it to the UI thread.
     */
    @Override
    public void addLine_OnLogStreamerThread(String containerName, String streamName, String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                doAddLine_OnGUI(line);
            }
        });
    }

    private synchronized void doAddLine_OnGUI(String line) {
        if (streamViewHandle!=null) {
            streamViewHandle.addLine_OnGUI(line);
        }
    }

    @Override
    public void detectedProgress_OnLogStreamerThread(String streamName, ActionProgress actionProgress) {
    }

    @Override
    public synchronized void disconnected_OnLogStreamerThread() {
        streamViewHandle=null;
    }
}
