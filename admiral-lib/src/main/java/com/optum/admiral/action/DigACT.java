package com.optum.admiral.action;

import com.optum.admiral.Admiral;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.io.VariableWriterUtil;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.preferences.OutputPreferences;

import java.util.Collection;

public class DigACT {
    private final Admiral admiral;
    private final OutputWriter writer;
    private final ComposeConfig composeConfig;
    private final VariableWriterUtil variableWriterUtil;

    public DigACT(Admiral admiral, ComposeConfig composeConfig, OutputPreferences preferences, OutputStyler styler, OutputWriter writer) {
        this.admiral = admiral;
        this.composeConfig = composeConfig;
        this.writer = writer;
        this.variableWriterUtil = new VariableWriterUtil(preferences, styler, writer);
    }

    public void perform(Collection<String> serviceNames, boolean showAll) throws AdmiralServiceConfigNotFoundException {
        Collection<AdmiralServiceConfig> services = composeConfig.getServicesCopyOrAll(serviceNames);

        boolean needsBlankLine = false;
        for (AdmiralServiceConfig admiralServiceConfig : services) {
            if (needsBlankLine) {
                writer.outln("");
            }
            needsBlankLine=true;

            final String serviceName = admiralServiceConfig.getName();
            variableWriterUtil.writeDigACT(admiral, serviceName, admiralServiceConfig, showAll);
        }
    }

}
