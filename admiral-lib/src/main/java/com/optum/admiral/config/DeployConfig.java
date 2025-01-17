package com.optum.admiral.config;

public class DeployConfig {
    final int replicas;
    public DeployConfig(int replicas) {
        this.replicas = replicas;
    }

    public int getReplicas() {
        return replicas;
    }
}
