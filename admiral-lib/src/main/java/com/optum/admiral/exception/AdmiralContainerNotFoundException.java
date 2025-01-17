package com.optum.admiral.exception;

public class AdmiralContainerNotFoundException extends AdmiralDockerException {
    private final String containerName;

    public AdmiralContainerNotFoundException(String containerName) {
        this.containerName = containerName;
    }

    public String getContainerName() {
        return containerName;
    }
}
