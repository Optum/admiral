package com.optum.admiral.exception;

import java.io.IOException;

public class AdmiralIOException extends AdmiralDockerException {
    private final IOException cause;

    public AdmiralIOException(IOException cause) {
        super(cause.getMessage());
        this.cause = cause;
    }

    @Override
    public synchronized IOException getCause() {
        return cause;
    }
}
