package com.optum.admiral.shell.commandregistry;

import com.optum.admiral.Admiral;
import com.optum.admiral.console.Console;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.preferences.UXPreferences;
import com.optum.admiral.shell.AdmiralShellModelController;
import org.jline.console.CommandInput;

public class StatusCommandRegistry extends DASHCommandRegistry {
    public StatusCommandRegistry(Admiral admiral, AdmiralShellModelController admiralShellModelController, UXPreferences uxPreferences, Console console) {
        super("Status Commands", admiral, admiralShellModelController, uxPreferences, console);

        cmd("list",     this::listCMD,     CT.SERVICE, "Show summary of containers");
        cmd("port",     this::portCMD,     CT.PORT,    "Show port status");
        cmd("services", this::servicesCMD, CT.NULL,    "Show summary of services");
        cmd("ps",       this::psCMD,       CT.SERVICE, "Show detail of containers");

        registerCommands(commandExecute);
    }

    private void listCMD(CommandInput input) throws AdmiralDockerException {
        admiralShellModelController.listACT();
    }

    private void servicesCMD(CommandInput input) throws AdmiralDockerException, AdmiralServiceConfigNotFoundException {
        admiralShellModelController.servicesACT();
    }

    private void psCMD(CommandInput input) throws AdmiralDockerException {
        admiralShellModelController.psACT();
    }

    private void portCMD(CommandInput input) throws AdmiralDockerException {
        // Guard
        if (input.args().length!=1) {
            admiralShellModelController.showPortUseACT();
            return;
        }

        // Gather
        String portAsString = input.args()[0];
        int port;
        try {
            port = Integer.parseInt(portAsString);
        } catch (NumberFormatException e) {
            console.outln(os.errorFocus.format(portAsString) + os.error.format(" is not a valid port number."));
            return;
        }

        admiralShellModelController.showPortUseACT(port);
    }

}
