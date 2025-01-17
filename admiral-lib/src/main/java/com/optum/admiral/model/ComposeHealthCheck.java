package com.optum.admiral.model;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import com.optum.admiral.exception.AdmiralDockerException;


import java.io.Closeable;
import java.io.IOException;

public class ComposeHealthCheck extends HealthCheck implements ResultCallback<Frame> {
    public final boolean cmdShell;
    public final String[] args;

    public ComposeHealthCheck(String id, boolean cmdShell, String[] args, long delay, long timeout, int retries, long interval, long minimum_interval, long rewait_period, long rewait_interval, boolean disabled) {
        super(id, delay, timeout, retries, interval, minimum_interval, rewait_period, rewait_interval, disabled);
        this.cmdShell = cmdShell;
        this.args = args;
    }

    @Override
    public long execute(DockerModelController dockerModelController, String containerName, int tryCount) {
        try {
            dockerModelController.healthCheckProgress(tryCount, retries, id, "Check", getTest());

            ExecCreateCmdResponse execCreateCmdResponse = dockerModelController.execute(containerName, this, args);

            // Wait (block using ourselves as the synchronization object) for execute completion
            synchronized (this) {
                this.wait();
            }

            final long exitCode = dockerModelController.checkExecResponse(execCreateCmdResponse);

            dockerModelController.healthCheckProgress(tryCount, retries, id, "Result", "Return Code: " + exitCode);
            return exitCode;
        } catch (AdmiralDockerException e) {
            String msg = e.getMessage()==null?"":e.getMessage();
            if ("java.io.IOException: Connection reset by peer".equals(msg)) {
                msg = "Connection reset by peer";
                dockerModelController.healthCheckProgress(tryCount, retries, id, "Connection Reset", msg);
                return -1;
            }
            dockerModelController.healthCheckProgress(tryCount, retries, id, "Error", msg);
            return -1;
        } catch (RuntimeException e) {
            dockerModelController.healthCheckProgress(tryCount, retries, id, "Runtime Exception", "");
            return -1;
        } catch (InterruptedException e) {
            dockerModelController.healthCheckProgress(tryCount, retries, id, "Wait Interrupted", "");
            return -1;
        }
    }

    @Override
    public String getTest() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\"");
        if (cmdShell) {
            sb.append("CMD-SHELL");
        } else {
            sb.append("CMD");
        }
        sb.append("\"");
        for(String s : args) {
            sb.append(String.format(", \"%s\"", s));
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String getId(String containerName) {
        if (id==null) {
            return containerName + " " + getTest();
        } else {
            return id;
        }
    }

    @Override
    public void onStart(Closeable closeable) {
    }

    @Override
    public void onNext(Frame frame) {
        // This is where we could read the output of the healthcheck if we wanted to.
    }

    @Override
    public void onError(Throwable throwable) {
    }

    @Override
    public void onComplete() {
        // Notify ourself.  This completes a wait().
        synchronized (this) {
            this.notifyAll();
        }
    }

    @Override
    public void close() throws IOException {
    }
}
