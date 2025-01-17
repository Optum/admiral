package com.optum.admiral.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public interface Optioner {
    boolean isPreOption();
    Option getOption();
    void process(CommandLine commandLine);
}
