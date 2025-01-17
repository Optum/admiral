package com.optum.admiral.exception;

public class AdmiralDockerException extends Exception {
    protected AdmiralDockerException() {
    }

    public AdmiralDockerException(String message) {
        super(message);
    }
}
