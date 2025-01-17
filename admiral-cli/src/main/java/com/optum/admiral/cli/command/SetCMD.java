package com.optum.admiral.cli.command;

import com.optum.admiral.Admiral;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.preferences.UXPreferences;

import java.io.IOException;
import java.util.List;

public class SetCMD extends AbstractAdmiralCommand {

    public SetCMD() {
        super("set", "Show all environment variables");
    }

    @Override
    public void executeCommand(UXPreferences uxPreferences, Admiral admiral, OutputWriter outputWriter, List<String> args)
            throws AdmiralDockerException, InterruptedException, IOException {
        if (args.isEmpty()) {
            admiral.setACT(uxPreferences.outputPreferences, uxPreferences.outputStyler, outputWriter);
        } else {
            for(String var : args) {
                admiral.setACT(uxPreferences.outputPreferences, uxPreferences.outputStyler, outputWriter, var);
            }
        }
    }

}
