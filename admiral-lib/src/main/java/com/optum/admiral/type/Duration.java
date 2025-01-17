package com.optum.admiral.type;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Duration {
    private static final long SECONDS = 1000;
    private static final long MINUTES = 60 * 1000;
    private static final long HOURS = 60 * 60 * 1000;
    private static final long DAYS = 24 * 60 * 60 * 1000;

    private long ms;

    public Duration(String s) {
        this.ms = parseToMS(s);
    }

    public String prettyS() {
        return prettyMS(this.getMS());
    }

    public static String prettyMS(long ms) {
        return prettyMS(ms, false);
    }

    public String prettyMS() {
        return prettyMS(this.getMS(), true);
    }

    public static String prettyMS(long ms, boolean showMS) {
        if (ms==0) {
            return "0 seconds";
        }

        StringBuilder sb = new StringBuilder();
        final long days = ms/DAYS;
        final long afterDays = ms - days*DAYS;
        final long hours = afterDays/HOURS;
        final long afterHours = afterDays - hours*HOURS;
        final long minutes = afterHours/MINUTES;
        final long afterMinutes = afterHours - minutes*MINUTES;
        final long seconds = afterMinutes/SECONDS;
        final long milliseconds = afterMinutes - seconds*SECONDS;
        boolean adding = false;
        if (days>0) {
            sb.append(String.format("%d day", days));
            if (days!=1)
                sb.append("s");
            adding = true;
        }
        if (hours>0) {
            if (adding)
                sb.append(" ");
            sb.append(String.format("%d hour", hours));
            if (hours!=1)
                sb.append("s");
            adding = true;
        }
        if (minutes>0) {
            if (adding)
                sb.append(" ");
            sb.append(String.format("%d minute", minutes));
            if (minutes!=1)
                sb.append("s");
            adding = true;
        }
        if (seconds>0) {
            if (adding)
                sb.append(" ");
            sb.append(String.format("%d second", seconds));
            if (seconds!=1)
                sb.append("s");
            adding = true;
        }
        if (showMS) {
            if (milliseconds>0) {
                if (adding)
                    sb.append(" ");
                sb.append(String.format("%d millisecond", milliseconds));
                if (milliseconds != 1)
                    sb.append("s");
            }
        } else {
            if (ms<1000) {
                sb.append("Less than a second");
            }
        }
        return sb.toString();
    }

    public static String conciseMS(long ms) {
        return conciseMS(ms, false);
    }

    public static String conciseMS(long ms, boolean showMS) {
        if (ms==0) {
            return "0ms";
        }

        StringBuilder sb = new StringBuilder();
        final long days = ms/DAYS;
        final long afterDays = ms - days*DAYS;
        final long hours = afterDays/HOURS;
        final long afterHours = afterDays - hours*HOURS;
        final long minutes = afterHours/MINUTES;
        final long afterMinutes = afterHours - minutes*MINUTES;
        final long seconds = afterMinutes/SECONDS;
        final long milliseconds = afterMinutes - seconds*SECONDS;
        if (days>0)
            sb.append(String.format("%dd", days));
        if (hours>0)
            sb.append(String.format("%dh", hours));
        if (minutes>0)
            sb.append(String.format("%dm", minutes));
        if (seconds>0)
            sb.append(String.format("%ds", seconds));
        if (showMS && milliseconds>0)
            sb.append(String.format("%dms", milliseconds));
        return sb.toString();
    }

    public static String rawMS(long ms) {
        return ms + "ms";
    }

    public static String rawS(long ms) {
        return (ms/1000) + "s";
    }

    public long getMS() {
        return ms;
    }

    public int getSeconds() {
        return (int)(ms/1000);
    }

    private static long parseToMS(String source) {
        Pattern pattern = Pattern.compile("^((\\d+)d)?((\\d+)h)?((\\d+)m)?((\\d+)s)?((\\d+)ms)?$");
        Matcher matcher = pattern.matcher(source);
        long ms = 0;
        while(matcher.find()) {
            ms += nullSafeToLong(matcher.group(10)); // ms
            ms += nullSafeToLong(matcher.group(8)) * SECONDS; // s
            ms += nullSafeToLong(matcher.group(6)) * MINUTES; // m
            ms += nullSafeToLong(matcher.group(4)) * HOURS; // h
            ms += nullSafeToLong(matcher.group(2)) * DAYS; // d
        }
        return ms;
    }

    private static long nullSafeToLong(String s) throws NumberFormatException {
        if (s==null)
            return 0L;
        else
            return Long.parseLong(s);
    }
}
