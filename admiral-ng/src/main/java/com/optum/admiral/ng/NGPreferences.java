package com.optum.admiral.ng;

import com.optum.admiral.cli.Optioner;
import com.optum.admiral.cli.OptionerSource;
import com.optum.admiral.preferences.PreferenceTracker;
import com.optum.admiral.preferences.PreferenceYaml;

public class NGPreferences {
    public final boolean flag;

    private NGPreferences(Builder builder) {
        this.flag = builder.flag;
    }

    public static class Builder extends PreferenceTracker implements OptionerSource, com.optum.admiral.cli.Builder<NGPreferences> {
        public boolean flag;

        @Override
        public Optioner[] getOptioners() {
            return new Optioner[0];
        }

        @Override
        public NGPreferences getData() {
            return new NGPreferences(this);
        }
    }

    public static class Yaml extends PreferenceYaml {
        public boolean flag;

        public Builder updateBuilder(Builder builder) {
            builder.setB("flag", false, flag, source);
            return builder;
        }

        public Builder createBuilder() {
            return updateBuilder(new Builder());
        }
    }
}
