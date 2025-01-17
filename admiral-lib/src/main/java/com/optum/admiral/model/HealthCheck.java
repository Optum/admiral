package com.optum.admiral.model;

import com.optum.admiral.event.HealthCheckListener;
import com.optum.admiral.type.Duration;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class HealthCheck {
    public final String id;
    public final long start_period;
    public final long timeout;
    public final int retries;
    public final long interval;
    public final long minimum_interval;
    public final long rewait_period;
    public final long rewait_interval;
    public final boolean disabled;

    public HealthCheck(String id, long start_period, long timeout, int retries, long interval, long minimum_interval, long rewait_period, long rewait_interval, boolean disabled) {
        this.id = id;
        this.start_period = start_period;
        this.timeout = timeout;
        this.retries = retries;
        this.interval = interval;
        this.minimum_interval = minimum_interval;
        this.rewait_period = rewait_period;
        this.rewait_interval = rewait_interval;
        this.disabled = disabled;
    }

    public abstract String getTest();

    /**
     * Return 0 = successful
     * Return -1 = not successful, continue
     * Return -2 = error, abort
     */
    public abstract long execute(DockerModelController dockerModelController, String containerName, int tryCount);

    public abstract String getId(String containerName);

    private static SimpleDateFormat whenFormat = new SimpleDateFormat("HH:mm:ss");

    private String triggerTime(long delay) {
        long now = new Date().getTime();
        Date then = new Date(now + delay);
        return whenFormat.format(then);
    }

    public boolean executeHealthCheck(DockerModelController dockerModelController, HealthCheckListener healthCheckLlistener, String containerName, boolean rewait) {
        final String id = getId(containerName);
        final long delayBeforeFirstCheck = (rewait) ? rewait_period : start_period;
        healthCheckLlistener.healthCheckProgress(id, "Initial Delay", "Wait: " + Duration.prettyMS(delayBeforeFirstCheck) + ". First try at " + triggerTime(delayBeforeFirstCheck));
        try {
            Thread.sleep(delayBeforeFirstCheck);
        } catch (InterruptedException e) {
            return false;
        }

        int tryCount = 1;
        long currentInterval = (rewait) ? rewait_interval : interval;
        while (tryCount < retries) {
            final long exitCode;
            exitCode = execute(dockerModelController, containerName, tryCount);
            if (exitCode == 0) {
                return true;
            } else if (exitCode == -2) {
                return false;
            }
            try {
                healthCheckLlistener.healthCheckProgress(tryCount, retries, id, "Sleep", "Wait: " + Duration.prettyMS(currentInterval) + ". Next try at " + triggerTime(currentInterval));
                Thread.sleep(currentInterval);
            } catch (InterruptedException e) {
                return false;
            }
            tryCount ++;
            currentInterval = currentInterval/2;
            if (currentInterval < minimum_interval) {
                currentInterval = minimum_interval;
            }
        }
        return false;
    }

}
