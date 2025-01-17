package com.optum.admiral.io;

import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.ProgressMessage;

import java.io.PrintStream;

public class PrintStreamOutputWriter implements OutputWriter {
    private final PrintStream printStream;
    private final ProgressMessageRenderer progressMessageRenderer;
    private AdmiralProgressHandler admiralProgressHandler;

    public PrintStreamOutputWriter(PrintStream printStream, ProgressMessageRenderer progressMessageRenderer) {
        this.printStream = printStream;
        this.progressMessageRenderer = progressMessageRenderer;
        this.admiralProgressHandler = new AdmiralProgressHandler(printStream);
    }

    @Override
    public void outTrackedLine(String key, String contents) {
        admiralProgressHandler.output(key, contents);
    }

    @Override
    public void outln(String s) {
        printStream.println(s);
        printStream.flush();
        resetProgress();
    }

    @Override
    public void outStackTrace(Throwable e) {
        e.printStackTrace(printStream);
        printStream.flush();
        resetProgress();
    }

    @Override
    public void progress(ProgressMessage progressMessage) {
        try {
            admiralProgressHandler.progress(progressMessage);
        } catch (AdmiralDockerException e) {
            outStackTrace(e);
        }
    }

    @Override
    public void progress(int current, int total, String url, String status, String progress) {
        ProgressMessage progressMessage = progressMessageRenderer.renderProgressMessage(current, total, url, status, progress);
        progress(progressMessage);
    }

    @Override
    public void progress(String url, String status, String progress) {
        ProgressMessage progressMessage = progressMessageRenderer.renderProgressMessage(url, status, progress);
        progress(progressMessage);
    }

    @Override
    public void resetProgress() {
        this.admiralProgressHandler = new AdmiralProgressHandler(printStream);
    }
}
