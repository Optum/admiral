package com.optum.admiral.model.dockerjava;

import com.github.dockerjava.api.model.Image;

public class DockerJavaImage implements com.optum.admiral.model.Image {
    private final Image i;

    DockerJavaImage(Image i) {
        this.i = i;
    }

    @Override
    public String[] getRepoTags() {
        return i.getRepoTags();
    }

    @Override
    public String getId() {
        return i.getId();
    }

    @Override
    public Long getCreated() {
        return i.getCreated();
    }
}
