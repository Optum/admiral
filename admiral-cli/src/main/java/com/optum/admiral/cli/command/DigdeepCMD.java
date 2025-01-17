package com.optum.admiral.cli.command;

import com.optum.admiral.Admiral;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.preferences.UXPreferences;

import java.io.IOException;
import java.util.List;

public class DigdeepCMD extends AbstractAdmiralCommand {

    public DigdeepCMD() {
        super("digdeep", "Dig up parameter values for everything in all containers");
    }

    @Override
    public void executeCommand(UXPreferences uxPreferences, Admiral admiral, OutputWriter outputWriter, List<String> args)
            throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException {
        admiral.inspectACT(uxPreferences.outputPreferences, uxPreferences.outputStyler, outputWriter, args);
    }

}
