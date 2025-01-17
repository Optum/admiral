package com.optum.admiral.mon;

import com.optum.admiral.cli.Optioner;
import com.optum.admiral.cli.OptionerSource;
import com.optum.admiral.preferences.PreferenceTracker;
import com.optum.admiral.preferences.PreferenceYaml;

public class MonitorPreferences {
    public final String logFont;
    public final int logFontSize;
    public final int logForegroundColor;
    public final int logBackgroundColor;

    private MonitorPreferences(Builder builder) {
        this.logFont = builder.logFont;
        this.logFontSize = builder.logFontSize;
        this.logForegroundColor = builder.logForegroundColor;
        this.logBackgroundColor = builder.logBackgroundColor;
    }

    public static class Builder extends PreferenceTracker implements OptionerSource, com.optum.admiral.cli.Builder<MonitorPreferences> {
        public String logFont;
        public int logFontSize;
        public int logForegroundColor;
        public int logBackgroundColor;

        @Override
        public Optioner[] getOptioners() {
            return new Optioner[0];
        }

        @Override
        public MonitorPreferences getData() {
            return new MonitorPreferences(this);
        }
    }

    public static class Yaml extends PreferenceYaml {
        public String logFont = null;
        public int logFontSize = 0;
        public int logForegroundColor = 0x1000000;  // Invalid means default
        public int logBackgroundColor = 0x1000000;  // Invalid means default

        public Builder updateBuilder(Builder builder) {
            builder.setO("logFont", null, logFont, source);
            builder.setO("logFontSize", 0, logFontSize, source);
            builder.setO("logBackgroundColor", 0x1000000, logFont, source);
            builder.setO("logForegroundColor", 0x1000000, logFont, source);
            return builder;
        }

        public Builder createBuilder() {
            return updateBuilder(new Builder());
        }
    }
}
