package com.optum.admiral;

public class CleanExit extends RuntimeException {
    private final String cleanMessage;

    public CleanExit(String cleanMessage) {
        this.cleanMessage = cleanMessage;
    }

    public String getCleanMessage() {
        return cleanMessage;
    }
}
