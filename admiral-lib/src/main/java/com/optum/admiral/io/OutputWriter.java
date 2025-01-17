package com.optum.admiral.io;

import com.optum.admiral.model.ProgressMessage;

public interface OutputWriter {
    void outTrackedLine(String key, String contents);
    void outln(String s);
    void outStackTrace(Throwable e);
    void progress(ProgressMessage progressMessage);
    void progress(int current, int total, String url, String status, String progress);
    void progress(String url, String status, String progress);
    void resetProgress();
}
