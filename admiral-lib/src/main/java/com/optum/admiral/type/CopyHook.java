package com.optum.admiral.type;

public class CopyHook {
    final String source;
    final String target;

    public CopyHook(String source, String target) {
        this.source = source;
        this.target = target;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }
}
