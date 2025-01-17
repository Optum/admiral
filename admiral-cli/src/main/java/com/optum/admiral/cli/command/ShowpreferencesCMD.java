package com.optum.admiral.cli.command;

import com.optum.admiral.Admiral;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.preferences.UXPreferences;

import java.util.List;

public class ShowpreferencesCMD extends AbstractAdmiralCommand {

    public ShowpreferencesCMD() {
        super("showpreferences", "Show all user preferences");
    }

    @Override
    public void executeCommand(UXPreferences uxPreferences, Admiral admiral, OutputWriter outputWriter, List<String> args) {
        admiral.showPreferencesACT(uxPreferences, outputWriter);
    }

}
