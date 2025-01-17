package com.optum.admiral.action;

import com.optum.admiral.ContainerParameterProcessor;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.preferences.OutputPreferences;

import java.util.Collection;
import java.util.Map;

public class ShowParametersACT {
    private final OutputStyler os;
    private final OutputWriter writer;
    private final ComposeConfig composeConfig;

    public ShowParametersACT(ComposeConfig composeConfig, OutputPreferences preferences, OutputStyler styler, OutputWriter writer) {
        this.composeConfig = composeConfig;
        this.os = styler;
        this.writer = writer;
    }

    public void perform(Collection<String> serviceNames) throws AdmiralServiceConfigNotFoundException {
        Collection<AdmiralServiceConfig> services = composeConfig.getServicesCopyOrAll(serviceNames);

        boolean needsBlankLine = false;
        for (AdmiralServiceConfig admiralServiceConfig : services) {
            if (needsBlankLine) {
                writer.outln("");
            }
            needsBlankLine=true;

            writeName(admiralServiceConfig);
            writeEnvironment(admiralServiceConfig);
        }
    }

    private void writeName(AdmiralServiceConfig admiralServiceConfig) {
        writer.outln(String.format("%s:", os.serviceHeading.format(admiralServiceConfig.getName())));
    }

    private void writeEnvironment(AdmiralServiceConfig admiralServiceConfig) {
        if (admiralServiceConfig.getEnvironmentVariableCount() > 0) {
            for (Map.Entry<String, ContainerParameterProcessor.Entry> ev : admiralServiceConfig.getEnvironmentVariables().entrySet()) {
                final String key = ev.getKey();
                final String value = ev.getValue().getDisplayValue();
                writer.outln(" " + os.heading.format(key) + "=" + value);
            }
        }
    }

}
