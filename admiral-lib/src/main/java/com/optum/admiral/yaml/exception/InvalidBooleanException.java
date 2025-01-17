package com.optum.admiral.yaml.exception;

public class InvalidBooleanException extends Exception {
    private final String source;
    private final int line;
    private final String propertyName;
    private final String invalidValue;

    public InvalidBooleanException(String source, int line, String propertyName, String invalidValue) {
        super("Invalid Boolean");
        this.source = source;
        this.line = line;
        this.propertyName = propertyName;
        this.invalidValue = invalidValue;
    }

    public int getLine() {
        return line;
    }

    public String getSource() {
        return source;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getInvalidValue() {
        return invalidValue;
    }
}
