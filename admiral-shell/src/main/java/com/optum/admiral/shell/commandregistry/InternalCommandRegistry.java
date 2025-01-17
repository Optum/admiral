package com.optum.admiral.shell.commandregistry;

import com.optum.admiral.Admiral;
import com.optum.admiral.console.Console;
import com.optum.admiral.preferences.UXPreferences;
import com.optum.admiral.shell.AdmiralShellModelController;
import org.jline.console.CommandInput;

public class InternalCommandRegistry extends DASHCommandRegistry {
    public InternalCommandRegistry(Admiral admiral, AdmiralShellModelController admiralShellModelController, UXPreferences uxPreferences, Console console) {
        super("Internal Commands", admiral, admiralShellModelController, uxPreferences, console);

        cmd("clear", this::clearCMD, CT.NULL, "Clear the screen");
        cmd("jvm",   this::jvmCMD,   CT.NULL, "Show interesting properties of this JVM");

        registerCommands(commandExecute);
    }

    private void clearCMD(CommandInput input) {
        admiralShellModelController.clearACT();
    }

    private void jvmCMD(CommandInput input) {
        admiralShellModelController.jvmACT();
    }

}
