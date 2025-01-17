package com.optum.admiral.action;

import com.optum.admiral.ConfigVariableProcessor;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.preferences.OutputPreferences;

import java.util.Map;

public class SetACT {
    private final ComposeConfig composeConfig;
    private final OutputPreferences outputPreferences;
    private final OutputStyler os;
    private final OutputWriter writer;

    public SetACT(ComposeConfig composeConfig, OutputPreferences outputPreferences, OutputStyler outputStyler, OutputWriter outputWriter) {
        this.composeConfig = composeConfig;
        this.outputPreferences = outputPreferences;
        this.os = outputStyler;
        this.writer = outputWriter;
    }

    public void perform(String var) {
        Map<String, ConfigVariableProcessor.Entry> data = composeConfig.getData();
        final ConfigVariableProcessor.Entry entry = data.get(var);
        if (entry==null) {
            return;
        }
        processEntry(entry);
    }

    public void perform() {
        Map<String, ConfigVariableProcessor.Entry> data = composeConfig.getData();
        for (Map.Entry<String, ConfigVariableProcessor.Entry> e : data.entrySet()) {
            final ConfigVariableProcessor.Entry entry = e.getValue();
            processEntry(entry);
        }
    }

    private void processEntry(ConfigVariableProcessor.Entry entry) {
        final String key = entry.getKey();
        final String value = entry.getDisplayValue();
        final String source = entry.getSource();
        final String coloredSource;
        if (entry.getEntrySourceType() == ConfigVariableProcessor.EntrySourceType.FILE) {
            coloredSource = os.file.format("[" + source + "]");
        } else if (entry.getEntrySourceType() == ConfigVariableProcessor.EntrySourceType.URL) {
            coloredSource = os.url.format("[" + source + "]");
        } else if (entry.getEntrySourceType() == ConfigVariableProcessor.EntrySourceType.SYSTEM) {
            coloredSource = os.system.format("[" + source + "]");
        } else if (entry.getEntrySourceType() == ConfigVariableProcessor.EntrySourceType.BUILTIN) {
            coloredSource = os.builtin.format("[" + source + "]");
        } else if (entry.getEntrySourceType() == ConfigVariableProcessor.EntrySourceType.USERPROVIDED) {
            coloredSource = os.userprovided.format("[" + source + "]");
        } else {
            coloredSource = source;
        }
        final String optionalSource;
        if (outputPreferences.showSource) {
            optionalSource = " " + coloredSource;
        } else {
            optionalSource = "";
        }
        writer.outln(os.heading.format(key) + "=" + os.value.format(value) + optionalSource);
    }
}
