package com.optum.admiral.cli.command;

import com.optum.admiral.Admiral;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.preferences.UXPreferences;

import java.io.IOException;
import java.util.List;

public class ShowconfigCMD extends AbstractAdmiralCommand {

    public ShowconfigCMD() {
        super("showconfig", "Show config (DEFINED variables sorted by source)");
    }

    @Override
    public void executeCommand(UXPreferences uxPreferences, Admiral admiral, OutputWriter outputWriter, List<String> args)
            throws AdmiralDockerException, InterruptedException, IOException {
        admiral.showconfigACT(uxPreferences.outputPreferences, uxPreferences.outputStyler, outputWriter);
    }

}
