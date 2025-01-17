package com.optum.admiral.cli.command;

import com.optum.admiral.cli.Command;

/**
 * This class is for da commands that don't interact with Admiral, such as version and help.  It
 * seems irresponsible to construct an Admiral runtime just to print help or the version, so we have this.
 */
public abstract class AbstractCommand<D> implements Command<D> {
    protected final String command;
    protected final String help;

    protected AbstractCommand(String command, String help) {
        this.command = command;
        this.help = help;
    }

    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public String getHelp() {
        return help;
    }

}
