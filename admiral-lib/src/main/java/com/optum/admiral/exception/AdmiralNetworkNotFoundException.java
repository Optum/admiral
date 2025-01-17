package com.optum.admiral.exception;

public class AdmiralNetworkNotFoundException extends AdmiralDockerException {
    private final String networkName;

    public AdmiralNetworkNotFoundException(String networkName) {
        this.networkName = networkName;
    }

    public String getNetworkName() {
        return networkName;
    }
}
