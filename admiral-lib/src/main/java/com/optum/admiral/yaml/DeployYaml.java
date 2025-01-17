package com.optum.admiral.yaml;

public class DeployYaml {
    public int replicas=1;
    public DeployYaml() {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    replicas: ");
        sb.append(replicas);
        sb.append("\n");
        return sb.toString();
    }
}
