package com.optum.admiral.type;

import com.optum.admiral.io.OutputWriter;

/**
 * Volume Long Syntax and Volume Short Syntax in Docker have different capabilities.  It is not possible to "just
 * convert a short syntax to a long syntax at parsing time and store everything long syntax."  They share some
 * features which are captured by "interface Volume" but otherwise are different.
 */
public class VolumeShortSyntax implements Volume  {
    private final String source;
    private final String target;
    private final String mode;

    public VolumeShortSyntax(String source, String target, String mode) {
        this.source = source;
        this.target = target;
        this.mode = mode;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getTarget() {
        return target;
    }

    public String getMode() {
        return mode;
    }

    @Override
    public void write(OutputWriter outputWriter) {
        outputWriter.outln(String.format("     - %s:%s:%s", source, target, mode));
    }
}
