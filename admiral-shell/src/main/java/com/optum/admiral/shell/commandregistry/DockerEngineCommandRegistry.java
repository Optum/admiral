package com.optum.admiral.shell.commandregistry;

import com.optum.admiral.Admiral;
import com.optum.admiral.console.Console;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.preferences.UXPreferences;
import com.optum.admiral.shell.AdmiralShellModelController;
import org.jline.console.CommandInput;

public class DockerEngineCommandRegistry extends DASHCommandRegistry {
    public DockerEngineCommandRegistry(Admiral admiral, AdmiralShellModelController admiralShellModelController, UXPreferences uxPreferences, Console console) {
        super("Docker Engine Commands", admiral, admiralShellModelController, uxPreferences, console);

        cmd("info",    this::infoCMD,    CT.NULL, "Show interesting properties of the Docker Engine");
        cmd("ping",    this::pingCMD,    CT.NULL, "Ping the Docker Engine");
        cmd("version", this::versionCMD, CT.NULL, "Show the Docker Engine version");

        registerCommands(commandExecute);
    }

    private void pingCMD(CommandInput input) throws AdmiralDockerException {
        admiralShellModelController.pingACT();
    }

    private void versionCMD(CommandInput input) throws AdmiralDockerException {
        admiralShellModelController.versionACT();
    }

    private void infoCMD(CommandInput input) throws AdmiralDockerException {
        admiralShellModelController.infoACT(input);
    }

}
