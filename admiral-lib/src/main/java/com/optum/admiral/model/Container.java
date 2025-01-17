package com.optum.admiral.model;

import com.github.dockerjava.api.model.ContainerMount;

import java.util.List;
import java.util.Map;

public interface Container {
    String getId();

    String getCommand();

    String getImage();

    String getImageId();

    Long getCreated();

    String getStatus();

    String getState();

    com.github.dockerjava.api.model.ContainerPort[] getPorts();

    Map<String, String> getLabels();

    String[] getNames();

    Long getSizeRw();

    Long getSizeRootFs();

    com.github.dockerjava.api.model.ContainerNetworkSettings getNetworkSettings();

    com.github.dockerjava.api.model.ContainerHostConfig getHostConfig();

    List<ContainerMount> getMounts();

}