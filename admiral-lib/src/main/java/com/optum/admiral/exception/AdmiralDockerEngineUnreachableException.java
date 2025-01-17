package com.optum.admiral.exception;

public class AdmiralDockerEngineUnreachableException extends AdmiralDockerException {
    private final boolean dockerSocketMissing;

    public AdmiralDockerEngineUnreachableException(boolean dockerSocketMissing) {
        this.dockerSocketMissing = dockerSocketMissing;
    }

    public boolean isDockerSocketMissing() {
        return dockerSocketMissing;
    }
}
