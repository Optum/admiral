package com.optum.admiral.shell.commandregistry;

import com.optum.admiral.Admiral;
import com.optum.admiral.console.Console;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.preferences.UXPreferences;
import com.optum.admiral.shell.AdmiralShellModelController;
import org.jline.console.CommandInput;

public class ImageCommandRegistry extends DASHCommandRegistry {
    public ImageCommandRegistry(Admiral admiral, AdmiralShellModelController admiralShellModelController, UXPreferences uxPreferences, Console console) {
        super("Image Commands", admiral, admiralShellModelController, uxPreferences, console);

        cmd("lsi",     this::lsiCMD,     CT.IMAGE, "List images");

        registerCommands(commandExecute);
    }

    private void lsiCMD(CommandInput input) throws AdmiralDockerException {
        admiralShellModelController.listImagesACT();
    }

}
