package com.optum.admiral.shell.commandregistry;

import com.optum.admiral.Admiral;
import com.optum.admiral.console.Console;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.preferences.UXPreferences;
import com.optum.admiral.shell.AdmiralShellModelController;
import org.jline.console.CommandInput;

import java.util.Arrays;

public class ShowCommandRegistry extends DASHCommandRegistry {
    public ShowCommandRegistry(Admiral admiral, AdmiralShellModelController admiralShellModelController, UXPreferences uxPreferences, Console console) {
        super("Show Commands", admiral, admiralShellModelController, uxPreferences, console);

        cmd("showcommands",    this::showcommandsCMD,    CT.NULL,    "Show tweaks commands");
        cmd("showcompose",     this::showcomposeCMD,     CT.SERVICE, "Show compose results (services)");
        cmd("showconfig",      this::showconfigCMD,      CT.NULL,    "Show config results (variables)");
        cmd("showgroups",      this::showgroupsCMD,      CT.NULL,    "Show service groups");
        cmd("showparameters",  this::showparametersCMD,  CT.SERVICE, "Show parameters passed to each service");
        cmd("showpreferences", this::showpreferencesCMD, CT.NULL,    "Show all user preferences");

        registerCommands(commandExecute);
    }

    private void showcommandsCMD(CommandInput input) {
        admiralShellModelController.commandsACT();
    }

    private void showgroupsCMD(CommandInput input) {
        admiralShellModelController.groupsACT();
    }

    private void showpreferencesCMD(CommandInput input) {
        admiralShellModelController.showPreferencesACT();
    }

    private void showcomposeCMD(CommandInput input) throws AdmiralServiceConfigNotFoundException {
        admiralShellModelController.showcomposeACT(Arrays.asList(input.args()));
    }

    private void showparametersCMD(CommandInput input) throws AdmiralServiceConfigNotFoundException {
        admiralShellModelController.showparametersACT(Arrays.asList(input.args()));
    }

    private void showconfigCMD(CommandInput input) {
        admiralShellModelController.showconfigACT();
    }

}
