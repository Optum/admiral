package com.optum.admiral.shell.commandregistry;

import com.optum.admiral.Admiral;
import com.optum.admiral.console.Console;
import com.optum.admiral.preferences.UXPreferences;
import com.optum.admiral.shell.AdmiralShellModelController;
import org.jline.console.CommandInput;


public class LegacyCommandRegistry extends DASHCommandRegistry {
    public LegacyCommandRegistry(Admiral admiral, AdmiralShellModelController admiralShellModelController, UXPreferences uxPreferences, Console console) {
        super("Legacy Commands", admiral, admiralShellModelController, uxPreferences, console);

        cmd("config",      this::configCMD,      CT.NULL,        "Works like: docker-compose config");
        cmd("env",         this::envCMD,         CT.SERVICE_ENV, "Works like: unix env");
        cmd("set",         this::setCMD,         CT.VARIABLE,    "Works like: bash set");

        registerCommands(commandExecute);
    }

    private void configCMD(CommandInput input) {
        admiralShellModelController.configACT();
    }

    private void envCMD(CommandInput input) {
        admiralShellModelController.envACT(input.args());
    }

    private void setCMD(CommandInput input) {
        if (input.args().length==0) {
            admiralShellModelController.setACT();
        } else {
            admiralShellModelController.setACT(input.args());
        }
    }

}
