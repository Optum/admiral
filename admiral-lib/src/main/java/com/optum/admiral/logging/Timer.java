package com.optum.admiral.logging;

public interface Timer {
    void stop();
    boolean isStopped();
    long getStartTime();
    long getStopTime();
    long getElapsedTime();
}
