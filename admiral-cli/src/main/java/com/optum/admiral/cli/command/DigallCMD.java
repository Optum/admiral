package com.optum.admiral.cli.command;

import com.optum.admiral.Admiral;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.preferences.UXPreferences;

import java.io.IOException;
import java.util.List;

public class DigallCMD extends AbstractAdmiralCommand {

    public DigallCMD() {
        super("digall", "Dig up parameters values for all");
    }

    @Override
    public void executeCommand(UXPreferences uxPreferences, Admiral admiral, OutputWriter outputWriter, List<String> args)
            throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException {
        admiral.digACT(uxPreferences.outputPreferences, uxPreferences.outputStyler, outputWriter, args, true);
    }

}
