package com.optum.admiral.model.dockerjava;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerHostConfig;
import com.github.dockerjava.api.model.ContainerMount;
import com.github.dockerjava.api.model.ContainerNetworkSettings;
import com.github.dockerjava.api.model.ContainerPort;

import java.util.List;
import java.util.Map;

public class DockerJavaContainer implements com.optum.admiral.model.Container {
    private final Container c;

    /**
     * Can only be constructed by DockerJavaDockerModelImpl
     */
    DockerJavaContainer(Container c) {
        this.c = c;
    }

    @Override
    public String getId() {
        return c.getId();
    }

    @Override
    public String getCommand() {
        return c.getCommand();
    }

    @Override
    public String getImage() {
        return c.getImage();
    }

    @Override
    public String getImageId() {
        return c.getImageId();
    }

    @Override
    public Long getCreated() {
        return c.getCreated();
    }

    @Override
    public String getStatus() {
        return c.getStatus();
    }

    @Override
    public String getState() {
        return c.getState();
    }

    @Override
    public ContainerPort[] getPorts() {
        return c.getPorts();
    }

    @Override
    public Map<String, String> getLabels() {
        return c.getLabels();
    }

    @Override
    public String[] getNames() {
        return c.getNames();
    }

    @Override
    public Long getSizeRw() {
        return c.getSizeRw();
    }

    @Override
    public Long getSizeRootFs() {
        return c.getSizeRootFs();
    }

    @Override
    public ContainerNetworkSettings getNetworkSettings() {
        return c.getNetworkSettings();
    }

    @Override
    public ContainerHostConfig getHostConfig() {
        return c.getHostConfig();
    }

    @Override
    public List<ContainerMount> getMounts() {
        return c.getMounts();
    }
}
