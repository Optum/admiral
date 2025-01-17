package com.optum.admiral;

public class DockerComposeContainerNamingConvention implements ContainerNamingConvention {

    final String projectName;

    public DockerComposeContainerNamingConvention(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public String calculateContainerName(String serviceName, int replicaInstance) {
        return projectName + "-" + serviceName + "-" + replicaInstance;
    }
}
