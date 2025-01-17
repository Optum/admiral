package com.optum.admiral.key;

public class LogStreamerKey {
    public final TabKey containerName;
    public final TabKey streamName;

    public LogStreamerKey(String containerName, String groupName, String streamName) {
        this.containerName = new TabKey("", containerName);
        this.streamName = new TabKey(groupName, streamName);
    }

    @Override
    public boolean equals(Object o) {
        if (o==null)
            return false;
        if (!(o instanceof LogStreamerKey))
            return false;
        LogStreamerKey other = (LogStreamerKey) o;
        return containerName.equals(other.containerName) && streamName.equals(other.streamName);
    }

    @Override
    public int hashCode() {
        return containerName.hashCode() + streamName.hashCode();
    }
}
