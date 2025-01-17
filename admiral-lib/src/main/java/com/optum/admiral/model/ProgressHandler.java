package com.optum.admiral.model;

import com.optum.admiral.exception.AdmiralDockerException;

public interface ProgressHandler {
    void progress(ProgressMessage progressMessage) throws AdmiralDockerException;
}
