package com.optum.admiral.model;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.optum.admiral.io.LogStreamer;
import com.optum.admiral.key.LogStreamerKey;

import java.util.HashMap;
import java.util.Map;

public class ContainerController {
    private final String name;
    private final Service service;
    private final Map<LogStreamerKey, LogStreamer> logStreamers = new HashMap<>();

    private InspectContainerResponse inspectContainerResponse;

    public ContainerController(final String name, final Service service) {
        if (name==null)
            throw new IllegalArgumentException("Null name is not allowed");
        if (service==null)
            throw new IllegalArgumentException("Null service is not allowed");
        this.name = name;
        this.service = service;
    }

    public boolean addStreamer(LogStreamerKey logStreamerKey, LogStreamer logStreamer) {
        if (logStreamers.containsKey(logStreamerKey)) {
            return true;
        }
        logStreamers.put(logStreamerKey, logStreamer);
        return false;
    }

    public void shutdown() {
        detatch();
    }

    public void detatch() {
        for(LogStreamer logStreamer : logStreamers.values()) {
            logStreamer.requestShutdown();
        }
        // TODO: This is a bit naive to assume a thread correctly shuts down.  Need more robustness here.
        logStreamers.clear();
    }

    public String getName() {
        return name;
    }

    public String getServiceName() {
        return service.getName();
    }

    public void setInspectContainerResponse(InspectContainerResponse inspectContainerResponse) {
        this.inspectContainerResponse = inspectContainerResponse;
    }

    public InspectContainerResponse getInspectContainerResponse() {
        return inspectContainerResponse;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o==null)
            return false;

        if (!(o instanceof ContainerController))
            return false;

        ContainerController other = (ContainerController)o;
        return name.equals(other.name);
    }
}
