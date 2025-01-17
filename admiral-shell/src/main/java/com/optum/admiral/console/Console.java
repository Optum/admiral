package com.optum.admiral.console;

import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.io.AdmiralProgressHandler;
import com.optum.admiral.model.ProgressHandler;
import com.optum.admiral.model.ProgressMessage;
import org.jline.terminal.Terminal;

import java.io.PrintStream;

public class Console implements ProgressHandler {
    private final Terminal terminal;
    private final PrintStream ps;
    private int currentWidth;

    private AdmiralProgressHandler admiralProgressHandler;

    public Console(Terminal terminal, PrintStream ps) {
        this.terminal = terminal;
        this.ps = ps;
        this.admiralProgressHandler = null;
        // The call to terminal.getWidth() is massively expensive.  It does an OS exec call!  And don't even
        // think about calling it from different threads in cancel cleanup.  Poof.  So here we grab the value
        // from the main thread when we are constructed.
        // Elegant text formatting during a window resize event?  Pipe dream.
        // TODO: register a Signal.WINCH window resize signal handler on the main thread and update it when needed.
        this.currentWidth = terminal.getWidth();
    }

    public void updateCurrentWidth() {
        currentWidth = terminal.getWidth();
    }

    public int getWidth() {
        return currentWidth;
    }

    public synchronized void outStackTrace(Throwable t) {
        t.printStackTrace(ps);
        ps.flush();
    }

    public synchronized void outln(String s) {
        if (admiralProgressHandler!=null) {
            admiralProgressHandler.outln(s);
        } else {
            ps.println(s);
            ps.flush();
        }
    }

    public synchronized void outTrackedLine(String key, String contents) {
        createIfNecessary();
        admiralProgressHandler.output(key, contents);
    }

    public synchronized void clearScreen() {
        createIfNecessary();
        admiralProgressHandler.clearScreen();
    }

    @Override
    public synchronized void progress(ProgressMessage progressMessage) {
        try {
            createIfNecessary();
            admiralProgressHandler.progress(progressMessage);
        } catch (AdmiralDockerException e) {
            outStackTrace(e);
        }
    }

    private void createIfNecessary() {
        if (admiralProgressHandler==null) {
            admiralProgressHandler = new AdmiralProgressHandler(ps);
        }
    }

    public synchronized void resetProgress() {
        this.admiralProgressHandler = null;
    }
}
