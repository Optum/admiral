package com.optum.admiral.shell.commandregistry;

import com.optum.admiral.Admiral;
import com.optum.admiral.console.Console;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.preferences.UXPreferences;
import com.optum.admiral.shell.AdmiralShellModelController;
import org.jline.console.CommandInput;

import java.util.Arrays;

public class DigCommandRegistry extends DASHCommandRegistry {
    public DigCommandRegistry(Admiral admiral, AdmiralShellModelController admiralShellModelController, UXPreferences uxPreferences, Console console) {
        super("Dig Commands", admiral, admiralShellModelController, uxPreferences, console);

        cmd("dig",     this::digCMD,     CT.SERVICE, "Dig up parameter values listed in x-admiral_dig");
        cmd("digall",  this::digallCMD,  CT.SERVICE, "Dig up parameter values for all");
        cmd("digdeep", this::digdeepCMD, CT.SERVICE, "Dig up parameter values for everything in all containers");

        registerCommands(commandExecute);
    }

    private void digCMD(CommandInput input) throws AdmiralServiceConfigNotFoundException {
        admiralShellModelController.digACT(Arrays.asList(input.args()), false);
    }

    private void digallCMD(CommandInput input) throws AdmiralServiceConfigNotFoundException {
        admiralShellModelController.digACT(Arrays.asList(input.args()), true);
    }

    private void digdeepCMD(CommandInput input) throws AdmiralServiceConfigNotFoundException {
        admiralShellModelController.inspectACT(Arrays.asList(input.args()));
    }

}
