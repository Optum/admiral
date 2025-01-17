package com.optum.admiral.action;

import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.type.Commands;

import static com.optum.admiral.type.Commands.Binding.AUTO;

public class CommandsACT {
    private final ComposeConfig composeConfig;
    private final OutputStyler os;
    private final OutputWriter writer;

    public CommandsACT(ComposeConfig composeConfig, OutputStyler outputStyler, OutputWriter outputWriter) {
        this.composeConfig = composeConfig;
        this.writer = outputWriter;
        this.os = outputStyler;
    }

    public void perform() {
        writer.outln(os.section.format("Commands:"));
        boolean needsBlankLine = false;
        for (AdmiralServiceConfig service : composeConfig.getServicesOrEmpty()) {
            Commands commands = service.getCommands();
            if (commands.allAUTO()) {
                continue;
            }
            if (needsBlankLine)
                writer.outln("");
            needsBlankLine = true;
            writer.outln("  " + os.group.format(service.getName() + ":"));
            writeLine("bounce", commands.bounce);
            writeLine("create", commands.create);
            writeLine("down", commands.down);
            writeLine("join", commands.join);
            writeLine("restart", commands.restart);
            writeLine("rm", commands.rm);
            writeLine("start", commands.start);
            writeLine("stop", commands.stop);
            writeLine("unjoin", commands.unjoin);
            writeLine("up", commands.up);
            writeLine("wait", commands.wait);
        }
    }

    private void writeLine(String name, Commands.Binding value) {
        if (value!=AUTO) {
            writer.outln("    " + name + ": " + value);
        }
    }

}
