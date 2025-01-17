package com.optum.admiral.type;

public class DockerComposeNetworkName {
    private final String name;
    private final String projectName;
    private final String networkLogicalName;

    public DockerComposeNetworkName(String projectName, String networkLogicalName) {
        this.name = projectName + "_" + networkLogicalName;
        this.projectName = projectName;
        this.networkLogicalName = networkLogicalName;
    }

    public String getName() {
        return name;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getNetworkLogicalName() {
        return networkLogicalName;
    }
}
