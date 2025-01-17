package com.optum.admiral.io;

public class AdmiralFileException extends Exception {
    private final String filename;
    private final String context;

    public AdmiralFileException(String context, String filename) {
        super(context);
        this.context = context;
        this.filename = filename;
    }

    public String getContext() {
        return context;
    }

    public String getFilename() {
        return filename;
    }
}
