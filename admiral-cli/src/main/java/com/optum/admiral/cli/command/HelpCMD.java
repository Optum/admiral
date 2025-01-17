package com.optum.admiral.cli.command;

import com.optum.admiral.Version;
import com.optum.admiral.cli.Command;
import com.optum.admiral.cli.OptionerProcessorAndExecutor;
import com.optum.admiral.preferences.UXPreferences;
import org.apache.commons.cli.HelpFormatter;

import java.util.List;

public class HelpCMD extends AbstractCommand<UXPreferences> {
    private final OptionerProcessorAndExecutor<UXPreferences, UXPreferences.Builder> op;

    public HelpCMD(OptionerProcessorAndExecutor<UXPreferences, UXPreferences.Builder> op) {
        super("help", "Show this help");
        this.op = op;
    }

    @Override
    public void run(UXPreferences data, List<String> args) {
        run();
    }

    public void run() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(132, "da [OPTION OPTION ...] COMMAND [service service ...]",
                "\nControl your Docker containers with Admiral!\n\nCOMMANDs:\n" +
                        generateCommandsBlock() +
                        "\n\nOPTIONs:\n",
                op.getOptionsCache(),
                "\nVersion " + Version.VERSION + " - (c) Optum.");
    }

    private String generateCommandsBlock() {
        int maximumLengthOfCommand = 0;
        for(Command<UXPreferences> command : op.getCommands()) {
            maximumLengthOfCommand = Math.max(maximumLengthOfCommand, command.getCommand().length());
        }

        // Add three padding
        final int padWithSpacesUpTo = maximumLengthOfCommand + 3;
        StringBuilder sb = new StringBuilder();
        for(Command<UXPreferences> command : op.getCommands()) {
            sb.append(" ");
            sb.append(command.getCommand());
            for(int i=command.getCommand().length(); i<padWithSpacesUpTo; i++)
                sb.append(" ");
            sb.append(command.getHelp());
            sb.append("\n");
        }
        return sb.toString();
    }
}
