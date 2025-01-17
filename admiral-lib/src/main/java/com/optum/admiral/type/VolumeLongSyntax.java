package com.optum.admiral.type;

import com.optum.admiral.io.OutputWriter;

/**
 * Volume Long Syntax and Volume Short Syntax in Docker have different capabilities.  It is not possible to "just
 * convert a short syntax to a long syntax at parsing time and store everything long syntax."  They share some
 * features which are captured by "interface Volume" but otherwise are different.
 */
public class VolumeLongSyntax implements Volume {
    private final String type;
    private final String source;
    private final String target;
    private final boolean readOnly;

    public VolumeLongSyntax(String type, String source, String target, boolean readOnly) {
        this.type = type;
        this.source = source;
        this.target = target;
        this.readOnly = readOnly;
    }

    public String getType() {
        return type;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getTarget() {
        return target;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void write(OutputWriter outputWriter) {
        outputWriter.outln(String.format("     - type: %s", type));
        outputWriter.outln(String.format("       source: %s", source));
        outputWriter.outln(String.format("       target: %s", target));
        outputWriter.outln(String.format("       read_only: %s", readOnly));
    }

}
