package com.optum.admiral.type;

public class ActionMonitorMarker {
    public final long msFromStart;
    public final String regex;
    public final String desc;

    public ActionMonitorMarker(long msFromStart, String regex, String desc) {
        this.msFromStart = msFromStart;
        this.regex = regex;
        if (desc==null || desc.trim().length()<1) {
            this.desc = regex;
        } else {
            this.desc = desc;
        }
    }
}
