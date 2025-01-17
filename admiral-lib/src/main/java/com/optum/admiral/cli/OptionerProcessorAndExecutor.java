package com.optum.admiral.cli;

import com.optum.admiral.io.AdmiralExceptionContainmentField;
import com.optum.admiral.io.AdmiralExceptionContainmentField.AdmiralContainedException;
import org.apache.commons.cli.CommandLine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OptionerProcessorAndExecutor<D, B extends Builder<D>> extends OptionerProcessor {
    private final Map<String, Command<D>> commandMap = new TreeMap<>();
    private Command<D> defaultCommand;

    public OptionerProcessorAndExecutor(OptionerSource optionerSource, String[] args) {
        super(optionerSource, args);
    }

    public void processAndExecute(B builder) throws AdmiralContainedException {
        CommandLine cl = new AdmiralExceptionContainmentField<CommandLine>().containInitialization(this::_process);
        D data = builder.getData();
        findAndExecuteCommand(data, cl.getArgList());
    }

    public void setDefaultCommand(Command<D> command) {
        addCommand(command);
        defaultCommand = command;
    }

    public Collection<Command<D>> getCommands() {
        return commandMap.values();
    }

    public void addCommand(Command<D> command) {
        commandMap.put(command.getCommand(), command);
    }

    private void findAndExecuteCommand(D data, List<String> argsList) {
        // Gather
        final List<String> parms = new ArrayList<>();
        final Command<D> command;

        if (argsList.isEmpty()) {
            command = defaultCommand;
        } else {
            command = commandMap.getOrDefault(argsList.get(0), defaultCommand);
            argsList.remove(0);
            parms.addAll(argsList);
        }

        // Guard
        if (command == null)
            return;

        // Go
        command.run(data, parms);
    }
}
