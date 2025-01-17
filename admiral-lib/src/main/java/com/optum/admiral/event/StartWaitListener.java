package com.optum.admiral.event;

public interface StartWaitListener {
    void startWaitProgress(int current, int total, String url, String status, String progress);
}
