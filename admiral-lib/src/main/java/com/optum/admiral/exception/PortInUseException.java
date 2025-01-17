package com.optum.admiral.exception;

public class PortInUseException extends AdmiralDockerException {
    private final String containerName;
    private final int port;

    public PortInUseException(String containerName, int port) {
        this.containerName = containerName;
        this.port = port;
    }

    public String getContainerName() {
        return containerName;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String getMessage() {
        return "PortInUseException:" + containerName + ":" + port;
    }
}
