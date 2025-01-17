package com.optum.admiral.type;

import com.optum.admiral.io.OutputWriter;

/**
 * Record the commonality between Volume Long Syntax and Volume Short Syntax.
 */
public interface Volume {
    void write(OutputWriter outputWriter);
    String getSource();
    String getTarget();
}
