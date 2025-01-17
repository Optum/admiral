package com.optum.admiral.config;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ActionProgress {
    public String actionName;

    public double percentageTimeComplete;
    public long msRemaining;
    public Date estCompletionTime;
    public String matched;
    public String desc;
    public float rate;
    public Date adjustedCompletionTime;
    public boolean isLast;

    private static SimpleDateFormat whenFormat = new SimpleDateFormat("HH:mm:ss");

    public String whenDone() {
        return String.format("Estimated finish at %s",
//                whenFormat.format(estCompletionTime),
                whenFormat.format(adjustedCompletionTime));
//        return String.format("%s (%s adjusted)",
//                whenFormat.format(estCompletionTime),
//                whenFormat.format(adjustedCompletionTime));
    }

    public String whenDonePercentage() {
        return String.format("Estimated finish at %s - %.0f%%",
//                whenFormat.format(estCompletionTime),
                whenFormat.format(adjustedCompletionTime),
                percentageTimeComplete);
//        return String.format("%s (%s adjusted) - %.0f%%",
//                whenFormat.format(estCompletionTime),
//                whenFormat.format(adjustedCompletionTime),
//                percentageTimeComplete);
    }


    public String getFormatted() {
        return getFormatted("%.0f%% complete. Completion in %d seconds at %s (%s at %.1f%%).");
    }

    public String getFormatted(String format) {
        return String.format(format,
                percentageTimeComplete,
                (msRemaining / 1000),
                whenFormat.format(estCompletionTime),
                whenFormat.format(adjustedCompletionTime),
                (rate*100));
    }
}
