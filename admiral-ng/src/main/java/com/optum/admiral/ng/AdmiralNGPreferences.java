package com.optum.admiral.ng;

import com.optum.admiral.AdmiralBootOptions;
import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.cli.OptionerSourceAgregator;
import com.optum.admiral.event.AdmiralEventPublisher;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.preferences.OutputPreferences;
import com.optum.admiral.preferences.PreferenceYaml;
import com.optum.admiral.preferences.PreferenceYamlLoader;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdmiralNGPreferences {
    public final AdmiralBootOptions admiralBootOptions;
    public final AdmiralOptions admiralOptions;
    public final NGPreferences ngPreferences;
    public final OutputPreferences outputPreferences;
    public final OutputStyler outputStyler;

    private AdmiralNGPreferences(AdmiralBootOptions admiralBootOptions,
                                 AdmiralOptions admiralOptions,
                                 NGPreferences ngPreferences,
                                 OutputPreferences outputPreferences) {
        this.admiralBootOptions = admiralBootOptions;
        this.admiralOptions = admiralOptions;
        this.ngPreferences = ngPreferences;
        this.outputPreferences = outputPreferences;
        this.outputStyler = new OutputStyler(true);
    }

    public static class Builder extends OptionerSourceAgregator implements com.optum.admiral.cli.Builder<AdmiralNGPreferences> {
        public final AdmiralBootOptions admiralBootOptions;
        public final AdmiralOptions.Builder admiralOptionsBuilder;
        public final NGPreferences.Builder ngPreferencesBuilder;
        public final OutputPreferences.Builder outputPreferencesBuilder;

        public static Builder createBuilder() {
            AdmiralBootOptions admiralBootOptions = new AdmiralBootOptions();
            AdmiralOptions.Builder admiralOptionsBuilder = new AdmiralOptions.Builder("dang");
            NGPreferences.Builder ngPreferencesBuilder = new NGPreferences.Builder();
            OutputPreferences.Builder outputPreferencesBuilder = new OutputPreferences.Builder();
            return new Builder(admiralBootOptions,
                    admiralOptionsBuilder,
                    ngPreferencesBuilder,
                    outputPreferencesBuilder);
        }

        public Builder(AdmiralBootOptions admiralBootOptions,
                       AdmiralOptions.Builder admiralOptionsBuilder,
                       NGPreferences.Builder ngPreferencesBuilder,
                       OutputPreferences.Builder outputPreferencesBuilder) {
            super(admiralBootOptions, admiralOptionsBuilder, ngPreferencesBuilder, outputPreferencesBuilder);
            this.admiralBootOptions = admiralBootOptions;
            this.admiralOptionsBuilder = admiralOptionsBuilder;
            this.ngPreferencesBuilder = ngPreferencesBuilder;
            this.outputPreferencesBuilder = outputPreferencesBuilder;
        }

        @Override
        public AdmiralNGPreferences getData() {
            return new AdmiralNGPreferences(admiralBootOptions,
                admiralOptionsBuilder.getData(),
                ngPreferencesBuilder.getData(),
                outputPreferencesBuilder.getData());
        }
    }

    public static class Yaml extends PreferenceYaml {
        public AdmiralOptions.Yaml admiral_options;
        public NGPreferences.Yaml ng_preferences;
        public OutputPreferences.Yaml output_preferences;

        public void verify() {
            if (admiral_options == null)
                admiral_options = new AdmiralOptions.Yaml();
            if (ng_preferences == null)
                ng_preferences = new NGPreferences.Yaml();
            if (output_preferences == null)
                output_preferences = new OutputPreferences.Yaml();
        }

        public void updateBuilder(Builder builder) {
            verify();
            admiral_options.updateBuilder(builder.admiralOptionsBuilder);
            ng_preferences.updateBuilder(builder.ngPreferencesBuilder);
            output_preferences.updateBuilder(builder.outputPreferencesBuilder, false);
        }

        @Override
        public void setSource(File sourceFile) {
            super.setSource(sourceFile);
            verify();
            admiral_options.setSource(source);
            ng_preferences.setSource(source);
            output_preferences.setSource(source);
        }
    }

    public static class Loader extends PreferenceYamlLoader<Yaml> {
        private final List<PreferenceSourceCandidate> possiblePreferencesFiles = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
                new PreferenceSourceCandidate(Source.CURRENT, new ArrayList<>(Collections.singletonList(new File("local-dang.preferences")))),
                new PreferenceSourceCandidate(Source.HOME, new ArrayList<>(Collections.singletonList(new File("local-dang.preferences")))),
                new PreferenceSourceCandidate(Source.HOME, new ArrayList<>(Collections.singletonList(new File(".dang.preferences")))),
                new PreferenceSourceCandidate(Source.CURRENT, new ArrayList<>(Collections.singletonList(new File(".dang.preferences"))))
        )));

        public Loader(AdmiralEventPublisher admiralEventPublisher) {
            super(new Constructor(Yaml.class, new LoaderOptions()), admiralEventPublisher);
        }

        @Override
        protected Yaml createDefault() {
            return new Yaml();
        }

        @Override
        protected List<PreferenceSourceCandidate> getPossiblePreferenceFiles() {
            return possiblePreferencesFiles;
        }
    }
}
