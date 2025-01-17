package com.optum.admiral.cli;

import java.util.List;

public interface Command<D> {
    String getCommand();
    String getHelp();
    void run(D data, List<String> args);
}
