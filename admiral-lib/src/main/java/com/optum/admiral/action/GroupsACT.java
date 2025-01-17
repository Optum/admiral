package com.optum.admiral.action;

import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;

import java.util.Set;

public class GroupsACT {
    private final ComposeConfig composeConfig;
    private final OutputStyler os;
    private final OutputWriter writer;

    public GroupsACT(ComposeConfig composeConfig, OutputStyler outputStyler, OutputWriter outputWriter) {
        this.composeConfig = composeConfig;
        this.writer = outputWriter;
        this.os = outputStyler;
    }

    public void perform() {
        writer.outln(os.section.format("Groups:"));
        boolean needsBlankLine = false;
        for (String serviceGroupName : composeConfig.getGroupEngine().getServiceGroupNames()) {
            if (needsBlankLine)
                writer.outln("");
            needsBlankLine = true;
            writer.outln("  " + os.group.format(serviceGroupName + ":"));
            Set<String> serviceNames = composeConfig.getGroupEngine().getServiceGroupList(serviceGroupName);
            for(String serviceName : serviceNames) {
                writer.outln("    " + os.service.format(serviceName));
            }
        }
    }

}
