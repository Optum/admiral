package com.optum.admiral.exception;

public class AdmiralImageNotFoundException extends AdmiralDockerException {
    private final String imageName;

    public AdmiralImageNotFoundException(String imageName) {
        this.imageName = imageName;
    }

    public String getImageName() {
        return imageName;
    }
}
