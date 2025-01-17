package com.optum.admiral.cli.command;

import com.optum.admiral.Admiral;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.preferences.UXPreferences;

import java.io.IOException;
import java.util.List;

public class ConfigCMD extends AbstractAdmiralCommand {
    public ConfigCMD() {
        super("config", "Show the fully-composed configuration");
    }

    @Override
    public void executeCommand(UXPreferences uxPreferences, Admiral admiral, OutputWriter outputWriter, List<String> args)
            throws AdmiralDockerException, InterruptedException, IOException {
        admiral.configACT(outputWriter);
    }
}
