package com.optum.admiral.io;

import com.optum.admiral.event.AdmiralEventListener;
import com.optum.admiral.config.ActionProgress;
import com.optum.admiral.preferences.OutputPreferences;

public class AdmiralLogStreamerListener implements LogStreamerListener {
    private final OutputPreferences outputPreferences;
    private final String containerName;
    private final AdmiralEventListener admiralEventListener;

    public AdmiralLogStreamerListener(OutputPreferences outputPreferences, String containerName, AdmiralEventListener admiralEventListener) {
        this.outputPreferences = outputPreferences;
        this.containerName = containerName;
        this.admiralEventListener = admiralEventListener;
    }

    @Override
    public void addLine_OnLogStreamerThread(String containerName, String streamName, String line) {
        if ("stderr".equals(streamName)) {
            if (!outputPreferences.showStderr)
                return;
        } else if ("stdout".equals(streamName)) {
            if (!outputPreferences.showStdout)
                return;
        } else if (!outputPreferences.showLogs) {
            return;
        }
        admiralEventListener.addLine(containerName, streamName, line);
    }

    @Override
    public void detectedProgress_OnLogStreamerThread(String streamName, ActionProgress actionProgress) {
        admiralEventListener.logStreamProgress(containerName, streamName, actionProgress);
    }

    @Override
    public void disconnected_OnLogStreamerThread() {
    }
}
