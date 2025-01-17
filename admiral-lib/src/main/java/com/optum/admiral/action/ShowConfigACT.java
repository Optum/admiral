package com.optum.admiral.action;

import com.optum.admiral.ConfigVariableProcessor;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.preferences.OutputPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ShowConfigACT {
    private final ComposeConfig composeConfig;
    private final OutputPreferences outputPreferences;
    private final OutputStyler os;
    private final OutputWriter writer;

    public ShowConfigACT(ComposeConfig composeConfig, OutputPreferences outputPreferences, OutputStyler outputStyler, OutputWriter outputWriter) {
        this.composeConfig = composeConfig;
        this.outputPreferences = outputPreferences;
        this.os = outputStyler;
        this.writer = outputWriter;
    }

    private static class Sorter implements Comparator<ConfigVariableProcessor.Entry> {
        @Override
        public int compare(ConfigVariableProcessor.Entry o1, ConfigVariableProcessor.Entry o2) {
            {
                final int compare = o1.sourceType.compareTo(o2.sourceType);
                if (compare != 0)
                    return compare;
            }
            {
                final int compare = o1.source.compareTo(o2.source);
                if (compare != 0)
                    return compare;
            }
            {
                final int compare = o1.key.compareTo(o2.key);
                return compare;
            }
        }
    }

    public void perform() {
        Map<String, ConfigVariableProcessor.Entry> data = composeConfig.getData();
        List<ConfigVariableProcessor.Entry> list = new ArrayList<>(data.values());
        Collections.sort(list, new Sorter());
        ConfigVariableProcessor.Entry lastEntry = null;
        for (ConfigVariableProcessor.Entry entry : list) {
            if ((lastEntry==null) || (lastEntry.sourceType!=entry.sourceType) || (!lastEntry.source.equals(entry.source))) {
                if (lastEntry!=null) {
                    writer.outln("");
                }
                final String source = entry.source;
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
                writer.outln(os.section.format("Config from:") + " " + coloredSource);
            }
            processEntry(entry);
            lastEntry = entry;
        }
    }

    private void processEntry(ConfigVariableProcessor.Entry entry) {
        final String key = entry.getKey();
        final String value = entry.getDisplayValue();
        writer.outln( os.heading.format(key) + "=" + os.formatValue(value));
    }
}
