package com.optum.admiral.exception;

public class AdmiralNetworkHasActiveEndpointsException extends AdmiralDockerException {
    private final String networkName;
    private final String networkId;

    public AdmiralNetworkHasActiveEndpointsException(String networkName, String networkId) {
        this.networkName = networkName;
        this.networkId = networkId;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getNetworkId() {
        return networkId;
    }
}
