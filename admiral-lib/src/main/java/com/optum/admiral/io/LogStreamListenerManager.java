package com.optum.admiral.io;

import com.optum.admiral.config.ActionProgress;

import java.util.ArrayList;
import java.util.List;

public class LogStreamListenerManager {
    /**
     * This variable's contents is thread-exposed.  It is guarded by the LogStreamer instance object - so it must only be
     * touched (read or write) inside synchronized member methods.
     */
    private final List<LogStreamerListener> listeners = new ArrayList<>();

    public synchronized void addListener(LogStreamerListener logStreamerListener) {
        listeners.add(logStreamerListener);
    }

    public synchronized void notifyAll(String containerName, String streamName, String line) {
        for(LogStreamerListener listener : listeners) {
            listener.addLine_OnLogStreamerThread(containerName, streamName, line);
        }
    }

    public synchronized void progressAll(String streamName, ActionProgress actionProgress) {
        for(LogStreamerListener listener : listeners) {
            listener.detectedProgress_OnLogStreamerThread(streamName, actionProgress);
        }
    }

    public synchronized void disconnectedAll() {
        for(LogStreamerListener listener : listeners) {
            listener.disconnected_OnLogStreamerThread();
        }
        listeners.clear();
    }

}
