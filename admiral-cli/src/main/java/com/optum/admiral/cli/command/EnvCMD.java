package com.optum.admiral.cli.command;

import com.optum.admiral.Admiral;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.preferences.UXPreferences;

import java.io.IOException;
import java.util.List;

public class EnvCMD extends AbstractAdmiralCommand {

    public EnvCMD() {
        super("env", "Show all container parameters");
    }

    @Override
    public void executeCommand(UXPreferences uxPreferences, Admiral admiral, OutputWriter outputWriter, List<String> args)
            throws AdmiralDockerException, InterruptedException, IOException {
        if (args.isEmpty()) {
            admiral.envACT(uxPreferences.outputPreferences, uxPreferences.outputStyler, outputWriter);
        } else if (args.size()==1){
            admiral.envACT(uxPreferences.outputPreferences, uxPreferences.outputStyler, outputWriter, args.get(0));
        } else {
            admiral.envACT(uxPreferences.outputPreferences, uxPreferences.outputStyler, outputWriter, args.get(0), args.subList(1, args.size()).toArray(new String[0]));
        }
    }

}
