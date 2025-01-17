package com.optum.admiral.logging;

/**
 * Simple Timer that holds start/stop time and elapsed time state.
 * It enforces a state-change policy of "auto-start, stop once" by throwing IllegalStateException if violated.
 */
public class SimpleTimer implements Timer {
    private final long startTime;
    private long stopTime = 0;
    private long elapsedTime = 0;

    public SimpleTimer() {
        // Go
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void stop() {
        // Guard
        if (isStopped()) {
            throw new IllegalStateException("Stop must only be called once.");
        }

        // Go
        stopTime = System.currentTimeMillis();
        elapsedTime = stopTime - startTime;
    }

    @Override
    public boolean isStopped() {
        return stopTime != 0;
    }

    @Override
    public long getStartTime() {
        // Go
        return startTime;
    }

    @Override
    public long getStopTime() {
        // Guard
        if (!isStopped()) {
            throw new IllegalStateException("Stop must be called before getStopTime.");
        }

        // Go
        return stopTime;
    }

    @Override
    public long getElapsedTime() {
        // Guard
        if (!isStopped()) {
            throw new IllegalStateException("Stop must be called before getElapsedTime.");
        }

        // Go
        return elapsedTime;
    }

}
