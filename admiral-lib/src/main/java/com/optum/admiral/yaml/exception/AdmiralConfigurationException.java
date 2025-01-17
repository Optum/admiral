package com.optum.admiral.yaml.exception;

public class AdmiralConfigurationException extends Exception {
    private final String source;

    public AdmiralConfigurationException(String source, String message) {
        super(message);
        this.source = source;
    }

    public AdmiralConfigurationException(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
