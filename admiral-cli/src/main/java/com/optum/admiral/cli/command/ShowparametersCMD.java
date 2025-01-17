package com.optum.admiral.cli.command;

import com.optum.admiral.Admiral;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.preferences.UXPreferences;

import java.util.List;

public class ShowparametersCMD extends AbstractAdmiralCommand {

    public ShowparametersCMD() {
        super("showparameters", "Show parameters (USED variables passed to each service)");
    }

    @Override
    public void executeCommand(UXPreferences uxPreferences, Admiral admiral, OutputWriter outputWriter, List<String> args)
            throws AdmiralServiceConfigNotFoundException {
        admiral.showparametersACT(uxPreferences.outputPreferences, uxPreferences.outputStyler, outputWriter, args);
    }

}
