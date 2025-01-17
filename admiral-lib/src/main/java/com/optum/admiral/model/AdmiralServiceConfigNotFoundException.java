package com.optum.admiral.model;

public class AdmiralServiceConfigNotFoundException extends Exception {
    private final String serviceName;

    public AdmiralServiceConfigNotFoundException(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
