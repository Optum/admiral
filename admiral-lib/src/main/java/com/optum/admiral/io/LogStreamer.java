package com.optum.admiral.io;

import com.optum.admiral.key.LogStreamerKey;

public interface LogStreamer {
    default String getContainerName() {
        return getPrimaryKey().containerName.tabName;
    }

    default String getStreamName() {
        return getPrimaryKey().streamName.tabName;
    }

    LogStreamerKey getPrimaryKey();
    void addListener(LogStreamerListener logStreamerListener);
    void requestShutdown();
}
