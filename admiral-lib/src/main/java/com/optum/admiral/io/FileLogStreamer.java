package com.optum.admiral.io;

import com.optum.admiral.config.ActionMonitor;
import com.optum.admiral.config.ActionProgress;
import com.optum.admiral.key.LogStreamerKey;
import com.optum.admiral.model.DockerModelController;
import com.optum.admiral.type.LogMonitor;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import java.io.File;

public class FileLogStreamer implements TailerListener, LogStreamer {
    private final String containerName;
    private final LogMonitor logMonitor;

    private final File logFile;
    private final String logFileName;
    private final LogStreamListenerManager logStreamListenerManager = new LogStreamListenerManager();
    private final Tailer tailer;
    private final LogStreamerKey logStreamerKey;

    public FileLogStreamer(String containerName, LogMonitor logMonitor) {
        this.containerName = containerName;
        this.logMonitor = logMonitor;

        logFile = new File(logMonitor.filename);
        logFileName = logFile.getName();
        logStreamerKey = new LogStreamerKey(containerName, "C", logFileName);

        this.tailer = new Tailer(logFile, this, 1000, true);
    }

    @Override
    public LogStreamerKey getPrimaryKey() {
        return logStreamerKey;
    }

    public void start(ThreadGroup logStreamerThreadGroup, boolean reconnecting, DockerModelController dmc) {
        if ((!reconnecting) && logMonitor.deleteAtStart) {
            dmc.deleteFile(logFile);
        }

        Thread thread = new Thread(logStreamerThreadGroup, tailer, "TailStreamer for " + containerName + ":/" + logFileName);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void addListener(LogStreamerListener logStreamerListener) {
        logStreamListenerManager.addListener(logStreamerListener);
    }

    @Override
    public void init(Tailer tailer) {
    }

    @Override
    public void fileNotFound() {
    }

    @Override
    public void fileRotated() {
    }

    @Override
    public void handle(String line) {
        logStreamListenerManager.notifyAll(containerName, logFileName, line+"\n");
        for(ActionMonitor actionMonitor : logMonitor.getActionMonitors()) {
            ActionProgress actionProgress = actionMonitor.checkForProgress(line);
            if (actionProgress !=null) {
                logStreamListenerManager.progressAll(logFileName, actionProgress);
            }
        }
    }

    @Override
    public void handle(Exception e) {
    }

    @Override
    public void requestShutdown() {
        logStreamListenerManager.disconnectedAll();

        // Tailer is well-behaved.  When asked to stop it exits its loop and its thread dies.
        tailer.stop();
    }

}
