package com.optum.admiral.config;

import java.util.List;

public class InvalidDependsOnException extends Exception {
    public static class InvalidDependsOn {
        public final String serviceName;
        public final String dependsOnName;
        public InvalidDependsOn(String serviceName, String dependsOnName) {
            this.serviceName = serviceName;
            this.dependsOnName = dependsOnName;
        }
    }

    private final List<InvalidDependsOn> invalidDependsOns;

    public InvalidDependsOnException(List<InvalidDependsOn> invalidDependOns) {
        this.invalidDependsOns = invalidDependOns;
    }

    public List<InvalidDependsOn> getInvalidDependsOn() {
        return invalidDependsOns;
    }
}
