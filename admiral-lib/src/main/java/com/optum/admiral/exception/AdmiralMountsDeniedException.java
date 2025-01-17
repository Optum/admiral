package com.optum.admiral.exception;

public class AdmiralMountsDeniedException extends AdmiralDockerException {
    private final String mountName;

    public AdmiralMountsDeniedException(String mountName) {
        this.mountName = mountName;
    }

    public String getMountName() {
        return mountName;
    }
}
