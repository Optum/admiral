package com.optum.admiral.type.exception;

public class InvalidSemanticVersion extends Exception {
    private final String value;

    public InvalidSemanticVersion(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
