package com.optum.admiral.console;

import com.optum.admiral.model.ProgressMessage;
import com.optum.admiral.preferences.OutputPreferences;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.io.ProgressMessageRenderer;
import org.jline.reader.LineReader;

public class ConsoleOutputWriter implements OutputWriter {
    private final Console console;
    private final LineReader reader;
    private final boolean showWaitProgressBars;
    private final OutputStyler outputStyler;
    private final ProgressMessageRenderer progressMessageRenderer;


    public ConsoleOutputWriter(Console console,
                               LineReader reader,
                               OutputPreferences outputPreferences,
                               OutputStyler outputStyler,
                               ProgressMessageRenderer progressMessageRenderer) {
        this.console = console;
        this.reader = reader;
        this.showWaitProgressBars = outputPreferences.showWaitProgressBars;
        this.outputStyler = outputStyler;
        this.progressMessageRenderer = progressMessageRenderer;
    }

    @Override
    public void outTrackedLine(String key, String contents) {
        console.outTrackedLine(key, contents);
    }

    @Override
    public void outln(String s) {
        console.outln(s);
    }

    @Override
    public void outStackTrace(Throwable e) {
        console.outStackTrace(e);
    }

    @Override
    public void progress(ProgressMessage progressMessage) {
        final String simpleMessage = outputStyler.log.format(progressMessage.id() + " " + progressMessage.status());
        if (reader.isReading()) {
            reader.printAbove(simpleMessage);
        } else if (showWaitProgressBars) {
            console.progress(progressMessage);
        } else {
            console.outln(simpleMessage);
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
        console.resetProgress();
    }
}
