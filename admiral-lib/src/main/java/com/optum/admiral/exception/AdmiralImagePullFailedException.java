package com.optum.admiral.exception;

public class AdmiralImagePullFailedException extends AdmiralDockerException {
    private final String imageName;

    public AdmiralImagePullFailedException(String imageName) {
        this.imageName = imageName;
    }

    public String getImageName() {
        return imageName;
    }
}
