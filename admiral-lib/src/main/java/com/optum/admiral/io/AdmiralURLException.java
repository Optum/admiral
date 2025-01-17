package com.optum.admiral.io;

public class AdmiralURLException extends Exception {
    private final String url;
    private final String context;

    public AdmiralURLException(String context, String url) {
        super(context);
        this.context = context;
        this.url = url;
    }

    public String getContext() {
        return context;
    }

    public String getURL() {
        return url;
    }
}
