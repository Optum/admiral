package com.optum.admiral.cli.command;

import com.optum.admiral.Admiral;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.preferences.UXPreferences;

import java.io.IOException;
import java.util.List;

public class ServicesCMD extends AbstractAdmiralCommand  {
    public ServicesCMD() {
        super("services", "Show summary of services");
    }

    @Override
    public void executeCommand(UXPreferences uxPreferences, Admiral admiral, OutputWriter outputWriter, List<String> args)
            throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException {
        admiral.servicesACT(uxPreferences.outputStyler, outputWriter);
    }

}
