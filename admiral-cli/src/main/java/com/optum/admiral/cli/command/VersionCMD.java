package com.optum.admiral.cli.command;

import com.optum.admiral.Version;
import com.optum.admiral.preferences.UXPreferences;

import java.util.List;

public class VersionCMD extends AbstractCommand<UXPreferences> {

    public VersionCMD() {
        super("version", "Show version");
    }

    @Override
    public void run(UXPreferences data, List<String> args) {
        System.out.println(Version.VERSION);
    }
}
