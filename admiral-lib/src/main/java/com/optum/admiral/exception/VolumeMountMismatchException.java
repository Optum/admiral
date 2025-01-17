package com.optum.admiral.exception;

public class VolumeMountMismatchException extends AdmiralDockerException {
    private final String hostPath;
    private final String containerPath;

    public VolumeMountMismatchException(String hostPath, String containerPath) {
        this.hostPath = hostPath;
        this.containerPath = containerPath;
    }

    public String getHostPath() {
        return hostPath;
    }

    public String getContainerPath() {
        return containerPath;
    }

    @Override
    public String getMessage() {
        return "Volume mapping file/directory mismatch from " + hostPath + " to " + containerPath;
    }
}
