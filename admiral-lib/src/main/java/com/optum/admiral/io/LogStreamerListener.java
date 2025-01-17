package com.optum.admiral.io;

import com.optum.admiral.config.ActionProgress;

public interface LogStreamerListener {
    void addLine_OnLogStreamerThread(String containerName, String streamName, String line);
    void detectedProgress_OnLogStreamerThread(String streamName, ActionProgress actionProgress);
    void disconnected_OnLogStreamerThread();
}
