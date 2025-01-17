package com.optum.admiral.action;

import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.io.VariableWriterUtil;
import com.optum.admiral.preferences.OutputPreferences;

public class EnvACT {
    private final OutputWriter writer;
    private final ComposeConfig composeConfig;
    private final VariableWriterUtil variableWriterUtil;

    public EnvACT(ComposeConfig composeConfig, OutputPreferences preferences, OutputStyler styler, OutputWriter writer) {
        this.composeConfig = composeConfig;
        this.writer = writer;
        this.variableWriterUtil = new VariableWriterUtil(preferences, styler, writer);
    }

    public void perform() {
        boolean needsBlankLine = false;
        for (AdmiralServiceConfig admiralServiceConfig : composeConfig.getServicesOrEmpty()) {
            if (needsBlankLine) {
                writer.outln("");
            }
            needsBlankLine=true;
            variableWriterUtil.writeEnvACT(admiralServiceConfig);
        }
    }

    public void perform(String serviceName) {
        AdmiralServiceConfig admiralServiceConfig = composeConfig.getServiceConfig(serviceName);
        if (admiralServiceConfig==null)
            return;

        variableWriterUtil.writeEnvACT(admiralServiceConfig);
    }

    public void perform(String serviceName, String[] vars) {
        AdmiralServiceConfig admiralServiceConfig = composeConfig.getServiceConfig(serviceName);
        if (admiralServiceConfig==null)
            return;

        variableWriterUtil.writeEnvACT(admiralServiceConfig, vars);
    }

}
