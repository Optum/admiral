package com.optum.admiral.config;

import com.optum.admiral.type.SemanticVersion;
import com.optum.admiral.type.VariableSpec;

public class EnvironmentVariableConstraintException extends Exception {
    private final SemanticVersion currentVersion;
    private final VariableSpec variableSpec;

    public EnvironmentVariableConstraintException(SemanticVersion currentVersion, VariableSpec variableSpec) {
        this.currentVersion = currentVersion;
        this.variableSpec = variableSpec;
    }
}
