package com.optum.admiral.io;

import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.ProgressHandler;
import com.optum.admiral.model.ProgressMessage;

import java.io.PrintStream;
import java.util.HashMap;

public class AdmiralProgressHandler implements ProgressHandler {
    private final PrintStream ps;
    private int untrackedLines=0;

    private final HashMap<String, Integer> linePosition = new HashMap<>();

    public AdmiralProgressHandler(PrintStream ps) {
        this.ps = ps;
    }

    /**
     * When output mode is rendering positioned lines, a non-positioned line (one without an id) causes the screen
     * buffer to scroll up by one line, but that offset is not
     */
    private int getPositionForKey(String key) {
        if (key==null) {
            // This is an untracked line.  We record that we have encountered it and return zero.
            ++untrackedLines;
            return 0;
        } else {
            // Size = the number of tracked lines (linePosition.size()) plus the number of untracked lines.
            int size = linePosition.size() + untrackedLines;
            if (linePosition.putIfAbsent(key, size) == null) {
                // This is a new line.  It, therefore, backs up 0 lines and is printed as is.
                return 0;
            } else {
                return size - linePosition.get(key);
            }
        }
    }

    private int getPositionForProgressMessageKey(String key) {
        return getPositionForKey(key);
    }

    public void outln(String contents) {
        ++untrackedLines;
        ps.println(contents);
        ps.flush();
    }

    public void output(String key, String contents) {
        final int position = getPositionForKey(key);
        printLineAt(position, contents);
        ps.flush();
    }

    @Override
    public void progress(ProgressMessage progressMessage) throws AdmiralDockerException {
        if (progressMessage.error() != null)
            throw new AdmiralDockerException(progressMessage.error());

        if (progressMessage.progressDetail() != null) {
            StringBuilder contents = new StringBuilder();
            String id = progressMessage.id();

            if (id!=null) {
                contents.append(id);
                contents.append(": ");
            }
            final String status = progressMessage.status();

            if (status!=null) {
                contents.append(status);
            }

            final String progress = progressMessage.progress();

            if (progress!=null) {
                contents.append(" ");
                contents.append(progress);
                contents.append("\n");
            }

            if (id==null) {
                id = status;
            }

            final int position = getPositionForProgressMessageKey(id);

            printLineAt(position, contents.toString());
        } else if (progressMessage.stream() != null) {
            ++untrackedLines;
            safePrint(progressMessage.stream());
        } else if (progressMessage.status() != null) {
            ++untrackedLines;
            ps.println(progressMessage.status());
        } else {
            ++untrackedLines;
            ps.println(progressMessage);
        }
        ps.flush();
    }

    private static final String MOVEUP_FORMAT = "\u001B[%dA";
    private static final String MOVEDOWN_FORMAT = "\u001B[%dB";
    private static final String DELETE_LINE = "\u001B[2K\r";
    private static final String CLEAR_SCREEN = "\u001B[2J\u001B[H";

    /**
     * Don't clear the screen if we are tracking lines.
     */
    public void clearScreen() {
        if (linePosition.isEmpty()) {
            ps.print(CLEAR_SCREEN);
        }
    }

    /**
     * Write the "contents" at "position" lines above the current location.
     * @param position The position up from bottom.
     * @param contents The formatted contents to be written.
     */
    private void printLineAt(int position, String contents) {
        if (position > 0) {
            ps.printf(MOVEUP_FORMAT, position);
            ps.print(DELETE_LINE);
        }
        safePrint(contents);
        if (position > 1) {
            ps.printf(MOVEDOWN_FORMAT, position - 1);
        }
    }

    private void safePrint(String s) {
        if (s.endsWith("\n")) {
            ps.print(s);
        } else {
            ps.println(s);
        }
    }
}
