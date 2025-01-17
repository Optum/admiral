package com.optum.admiral.event;

public interface HealthCheckListener {
    void healthCheckProgress( String url, String status, String progress);
    void healthCheckProgress(int current, int total, String url, String status, String progress);
}
