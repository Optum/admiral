package com.optum.admiral.shell.commandregistry;

import com.optum.admiral.Admiral;
import com.optum.admiral.console.Console;
import com.optum.admiral.io.AdmiralExceptionContainmentField;
import com.optum.admiral.io.AdmiralExceptionContainmentField.AdmiralContainedException;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.preferences.UXPreferences;
import com.optum.admiral.shell.AdmiralShellModelController;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.impl.JlineCommandRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This class is the bridge between the JLine processor and the core functionality of Docker Admiral Shell which is
 * implemented as AdmiralShellModelController.
 *
 * TL;DR:
 *   Don't *DO* any business-logic thing in this class!
 *
 * As such, this class has the following roles:
 *
 * 1) Provide the necessary glue to JLine command processor loop.
 * 2) Intercept commands coming from JLine in *CMD methods
 *   2b) Parse the CommandInput from JLine
 *   2c) Invoke the appropriate business actions as *ACT methods
 * 3) Provide generalized exception handling for all *CMD methods (thrown by the *ACT methods).
 *
 * Very little interaction with the console happens here.  It should only be messages directly dealing with the
 * processing of command-line input.  (Missing required parameter).  But make careful note: do not validate
 * parameters here.  That must be done in the *ACT.  The *CMD's role is just to make sure if an *ACT has
 * a required parameter, it gets it.  Since most *ACTs have none, or support none or many, most *CMD methods accept
 * any number of parameters and therefore don't have errors to detect.  The rare instance where a *CMD requires
 * a parameter you will see the error reporting.
 *
 * This class also reports caught *ACT exceptions to the console.
 *
 */
public class DASHCommandRegistry extends JlineCommandRegistry {
    private final String name;
    protected final Admiral admiral;
    protected final UXPreferences uxPreferences;
    protected final AdmiralShellModelController admiralShellModelController;
    protected final Console console;
    protected final OutputStyler os;

    protected final Map<String, List<String>> commandInfo = new HashMap<>();
    protected final Map<String, CommandMethods> commandExecute = new HashMap<>();

    public enum CT {
        NULL,
        NETWORKCOMMANDS,
        SERVICE,
        SERVICE_ENV,
        PORT,
        IMAGE,
        VARIABLE
    }

    public Map<CT, List<String>> commandTypes = new HashMap<>();

    public List<String> getCommandsOfType(CT commandType) {
        return commandTypes.computeIfAbsent(commandType, k -> new ArrayList<>());
    }

    protected void cmd(String name, ShellCommand cmd, CT commandType, String help, String ... aliases) {
        Collection<String> set = commandTypes.computeIfAbsent(commandType, k -> new ArrayList<>());
        set.add(name);
        commandExecute.put(name, new CommandMethods(new Wrapper(cmd), null));
        commandInfo.put(name, Collections.singletonList(help));
        for(String alias : aliases) {
            set.add(alias);
            commandExecute.put(alias, new CommandMethods(new Wrapper(cmd), null));
            commandInfo.put(alias, Collections.singletonList("Alias to " + name));
        }
    }

    public DASHCommandRegistry(String name, Admiral admiral, AdmiralShellModelController admiralShellModelController, UXPreferences uxPreferences, Console console) {
        super();
        this.name = name;
        this.admiral = admiral;
        this.admiralShellModelController = admiralShellModelController;
        this.console = console;
        this.uxPreferences = uxPreferences;
        this.os = uxPreferences.outputStyler;
    }

    @Override
    public String name() {
        return name;
    }

    @FunctionalInterface
    public interface ShellCommand {
        void doCommand(CommandInput commandInput)
                throws
                AdmiralDockerException,
                AdmiralServiceConfigNotFoundException,
                IOException,
                InterruptedException;
    }

    class Wrapper implements Consumer<CommandInput> {
        private final ShellCommand cmd;
        Wrapper(ShellCommand cmd) {
            this.cmd = cmd;
        }
        @Override
        public void accept(CommandInput ci) {
            try {
                new AdmiralExceptionContainmentField(os).containExecution(() ->
                    cmd.doCommand(ci)
                );
            } catch (AdmiralContainedException e) {
                for (String message : e.getMessages()) {
                    console.outln(message);
                }
            }
        }
    }

    /**
     * Part of JLine processing
     */
    @Override
    public java.util.List<java.lang.String> commandInfo(java.lang.String command) {
        return commandInfo.get(command(command));
    }

    /**
     * Part of JLine processing
     */
    protected String command(String name) {
        if (commandExecute.containsKey(name)) {
            return name;
        }
        return null;
    }

}
