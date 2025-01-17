package com.optum.admiral.io;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.optum.admiral.key.LogStreamerKey;
import com.optum.admiral.config.ActionMonitor;
import com.optum.admiral.config.ActionProgress;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * This class has the responsibility of pulling Docker Container stdout/stderr (Log) stream bytes by line and
 * sending the lines to a LogStreamListener.
 *
 * With the new Docker-Java library we do not need to do Threading - instead we implement
 * a callback object and are invoked as data is pulled from the container for us.
 *
 * This class will (presumably - to be tested and proved) be invoked on a Docker-Java listener thread.
 *
 * The LogStreamListener will be invoked ON THE LogStreamer THREAD !!!  (That's how in general it works.)  Be sure your
 * LogStreamListener is thread-safe.  Think of the LogStreamListener as the thread-bridge between this code and "your
 * code."  So it is your responsibility in LogStreamListener to "jump the thread barrier" into the appropriate thread
 * for running your response to the event.
 * If you can't point to what you're doing to make it thread-safe, it almost certainly ISN'T thread-safe.
 *
 */
public class ContainerLogStreamer implements LogStreamer, ResultCallback<Frame> {
    /**
     * This is our primary key.
     */
    private final LogStreamerKey primaryKey;

    private final String containerName;
    private final String streamName;

    private final List<ActionMonitor> actionMonitors;

    /**
     * Composition delegation to manage our listeners
     */
    private final LogStreamListenerManager logStreamListenerManager = new LogStreamListenerManager();

    /**
     * Get our primary key
     */
    @Override
    public LogStreamerKey getPrimaryKey() {
        return primaryKey;
    }

    private boolean shutdownRequested = false;

    /**
     * This is intentionally an inexpensive constructor to wrap the ugly primary key (StreamerKey) code.  It is
     * expected that a ContainerLogStreamer will be constructed simply in order to
     */
    public ContainerLogStreamer(String containerName, String groupName, String streamName, List<ActionMonitor> actionMonitors) {
        this.containerName = containerName;
        this.streamName = streamName;
        this.actionMonitors = actionMonitors;
        primaryKey = new LogStreamerKey(containerName, groupName, streamName);
    }

    @Override
    public void addListener(LogStreamerListener logStreamerListener) {
        logStreamListenerManager.addListener(logStreamerListener);
    }

    @Override
    public synchronized void requestShutdown() {
        logStreamListenerManager.disconnectedAll();

        shutdownRequested = true;
    }

    @Override
    public void onStart(Closeable closeable) {
    }

    @Override
    public void onNext(Frame f) {
        final String line =  new String(f.getPayload()).trim();
        logStreamListenerManager.notifyAll(containerName, streamName, line);
        for(ActionMonitor actionMonitor : actionMonitors) {
            ActionProgress actionProgress = actionMonitor.checkForProgress(line);
            if (actionProgress != null) {
                logStreamListenerManager.progressAll(streamName, actionProgress);
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {
    }

    @Override
    public void close() throws IOException {
    }
}
