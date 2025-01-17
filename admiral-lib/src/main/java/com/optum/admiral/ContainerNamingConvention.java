package com.optum.admiral;

public interface ContainerNamingConvention {
    String calculateContainerName(String serviceName, int replicaInstance);
}
