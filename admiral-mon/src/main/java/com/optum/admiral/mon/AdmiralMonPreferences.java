package com.optum.admiral.mon;

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

public class AdmiralMonPreferences {
    public final AdmiralBootOptions admiralBootOptions;
    public final AdmiralOptions admiralOptions;
    public final MonitorPreferences monitorPreferences;
    public final OutputPreferences outputPreferences;
    public final OutputStyler outputStyler;

    private AdmiralMonPreferences(AdmiralBootOptions admiralBootOptions,
                                  AdmiralOptions admiralOptions,
                                  MonitorPreferences monitorPreferences,
                                  OutputPreferences outputPreferences) {
        this.admiralBootOptions = admiralBootOptions;
        this.admiralOptions = admiralOptions;
        this.monitorPreferences = monitorPreferences;
        this.outputPreferences = outputPreferences;
        this.outputStyler = new OutputStyler(true);
    }

    public static class Builder extends OptionerSourceAgregator implements com.optum.admiral.cli.Builder<AdmiralMonPreferences> {
        public final AdmiralBootOptions admiralBootOptions;
        public final AdmiralOptions.Builder admiralOptionsBuilder;
        public final MonitorPreferences.Builder monitorPreferencesBuilder;
        public final OutputPreferences.Builder outputPreferencesBuilder;

        public static Builder createBuilder() {
            AdmiralBootOptions admiralBootOptions = new AdmiralBootOptions();
            AdmiralOptions.Builder admiralOptionsBuilder = new AdmiralOptions.Builder("damon");
            MonitorPreferences.Builder monitorPreferencesBuilder = new MonitorPreferences.Builder();
            OutputPreferences.Builder outputPreferencesBuilder = new OutputPreferences.Builder();
            return new Builder(admiralBootOptions,
                    admiralOptionsBuilder,
                    monitorPreferencesBuilder,
                    outputPreferencesBuilder);
        }

        public Builder(AdmiralBootOptions admiralBootOptions,
                       AdmiralOptions.Builder admiralOptionsBuilder,
                       MonitorPreferences.Builder monitorPreferencesBuilder,
                       OutputPreferences.Builder outputPreferencesBuilder) {
            super(admiralBootOptions, admiralOptionsBuilder, monitorPreferencesBuilder, outputPreferencesBuilder);
            this.admiralBootOptions = admiralBootOptions;
            this.admiralOptionsBuilder = admiralOptionsBuilder;
            this.monitorPreferencesBuilder = monitorPreferencesBuilder;
            this.outputPreferencesBuilder = outputPreferencesBuilder;
        }

        @Override
        public AdmiralMonPreferences getData() {
            return new AdmiralMonPreferences(admiralBootOptions,
                admiralOptionsBuilder.getData(),
                monitorPreferencesBuilder.getData(),
                outputPreferencesBuilder.getData());
        }
    }

    public static class Yaml extends PreferenceYaml {
        public AdmiralOptions.Yaml admiral_options;
        public MonitorPreferences.Yaml monitor_preferences;
        public OutputPreferences.Yaml output_preferences;

        private void verify() {
            if (admiral_options == null)
                admiral_options = new AdmiralOptions.Yaml();
            if (monitor_preferences == null)
                monitor_preferences = new MonitorPreferences.Yaml();
            if (output_preferences == null)
                output_preferences = new OutputPreferences.Yaml();
        }

        public void updateBuilder(Builder builder) {
            verify();
            admiral_options.updateBuilder(builder.admiralOptionsBuilder);
            monitor_preferences.updateBuilder(builder.monitorPreferencesBuilder);
            output_preferences.updateBuilder(builder.outputPreferencesBuilder, false);
        }

        @Override
        public void setSource(File sourceFile) {
            super.setSource(sourceFile);
            verify();
            admiral_options.setSource(source);
            monitor_preferences.setSource(source);
            output_preferences.setSource(source);
        }
    }

    public static class Loader extends PreferenceYamlLoader<Yaml> {
        private final List<PreferenceSourceCandidate> possiblePreferencesFiles = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
                new PreferenceSourceCandidate(Source.CURRENT, new ArrayList<>(Arrays.asList(new File("local-damon.preferences")))),
                new PreferenceSourceCandidate(Source.HOME, new ArrayList<>(Arrays.asList(new File("local-damon.preferences")))),
                new PreferenceSourceCandidate(Source.HOME, new ArrayList<>(Arrays.asList(new File(".damon.preferences")))),
                new PreferenceSourceCandidate(Source.CURRENT, new ArrayList<>(Arrays.asList(new File(".damon.preferences"))))
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
