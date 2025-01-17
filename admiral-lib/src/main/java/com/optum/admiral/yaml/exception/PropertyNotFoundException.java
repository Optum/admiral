package com.optum.admiral.yaml.exception;

public class PropertyNotFoundException extends Exception {
    private final String source;
    private final int line;
    private final String propertyName;

    public PropertyNotFoundException(String source, int line, String propertyName) {
        super("Property Not Found");
        this.source = source;
        this.line = line;
        this.propertyName = propertyName;
    }

    public String getSource() {
        return source;
    }

    public int getLine() {
        return line;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
