package com.optum.admiral.preferences;

import com.optum.admiral.AdmiralBootOptions;
import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.cli.OptionerSourceAgregator;
import com.optum.admiral.event.AdmiralEventPublisher;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.util.FileService;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UXPreferences {
    public final AdmiralBootOptions admiralBootOptions;
    public final AdmiralOptions admiralOptions;
    public final OutputPreferences outputPreferences;
    public final String theme_filename;
    public final File theme_file;
    public final OutputStyler outputStyler;

    private UXPreferences(AdmiralBootOptions admiralBootOptions,
                          AdmiralOptions admiralOptions,
                          OutputPreferences outputPreferences,
                          String theme_filename,
                          File theme_file,
                          OutputStyler outputStyler) {
        this.admiralBootOptions = admiralBootOptions;
        this.admiralOptions = admiralOptions;
        this.outputPreferences = outputPreferences;
        this.theme_filename = theme_filename;
        this.theme_file = theme_file;
        this.outputStyler = outputStyler;
    }

    public static class Builder extends OptionerSourceAgregator implements com.optum.admiral.cli.Builder<UXPreferences> {
        public final AdmiralBootOptions admiralBootOptions;
        public final AdmiralOptions.Builder admiralOptionsBuilder;
        public final OutputPreferences.Builder outputPreferencesBuilder;
        public String theme_filename;
        public File theme_file;
        public OutputStyler outputStyler;

        public static Builder createBuilder(String applicationName) {
            AdmiralBootOptions admiralBootOptions = new AdmiralBootOptions();
            AdmiralOptions.Builder admiralOptionsBuilder = new AdmiralOptions.Builder(applicationName);
            OutputPreferences.Builder outputPreferencesBuilder = new OutputPreferences.Builder();
            return new Builder(admiralBootOptions,
                    admiralOptionsBuilder,
                    outputPreferencesBuilder,
                    new OutputStyler(true));
        }

        public Builder(AdmiralBootOptions admiralBootOptions,
                       AdmiralOptions.Builder admiralOptionsBuilder,
                       OutputPreferences.Builder outputPreferencesBuilder,
                       OutputStyler outputStyler) {
            super(admiralBootOptions, admiralOptionsBuilder, outputPreferencesBuilder);
            this.admiralBootOptions = admiralBootOptions;
            this.admiralOptionsBuilder = admiralOptionsBuilder;
            this.outputPreferencesBuilder = outputPreferencesBuilder;
            this.outputStyler = outputStyler;
        }

        @Override
        public UXPreferences getData() {
            return new UXPreferences(admiralBootOptions,
                    admiralOptionsBuilder.getData(),
                    outputPreferencesBuilder.getData(),
                    theme_filename,
                    theme_file,
                    outputStyler);
        }

    }

    public static class Yaml extends PreferenceYaml {
        public AdmiralOptions.Yaml admiral_options;
        public OutputPreferences.Yaml output_preferences;

        // This is the source
        public String theme_file;
        // This is the result
        private File theme_filefile;
        private OutputStyler outputStyler;

        private void verify() {
            if (admiral_options == null)
                admiral_options = new AdmiralOptions.Yaml();
            if (output_preferences == null)
                output_preferences = new OutputPreferences.Yaml();
            if (outputStyler == null)
                outputStyler = new OutputStyler(true);
        }

        public void updateBuilder(Builder builder) {
            verify();
            admiral_options.updateBuilder(builder.admiralOptionsBuilder);
            output_preferences.updateBuilder(builder.outputPreferencesBuilder, true);
            builder.theme_filename = theme_file;
            builder.theme_file = theme_filefile;
            builder.outputStyler = outputStyler;
        }

        @Override
        public void setSource(File sourceFile) {
            super.setSource(sourceFile);
            verify();
            admiral_options.setSource(source);
            output_preferences.setSource(source);
        }
    }

    public static class Loader extends PreferenceYamlLoader<Yaml> {
        private final List<PreferenceSourceCandidate> possiblePreferencesFiles;

        public Loader(AdmiralEventPublisher admiralEventPublisher, String appPreferenceFileName) {
            super(new Constructor(Yaml.class, new LoaderOptions()), admiralEventPublisher);
            possiblePreferencesFiles = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
                    new PreferenceSourceCandidate(Source.CURRENT, new ArrayList<>(Arrays.asList(new File("local-" + appPreferenceFileName + ".preferences")))),
                    new PreferenceSourceCandidate(Source.HOME, new ArrayList<>(Arrays.asList(new File("local-" + appPreferenceFileName + ".preferences")))),
                    new PreferenceSourceCandidate(Source.HOME, new ArrayList<>(Arrays.asList(new File("." + appPreferenceFileName + ".preferences")))),
                    new PreferenceSourceCandidate(Source.CURRENT, new ArrayList<>(Arrays.asList(new File("." + appPreferenceFileName + ".preferences"))))
            )));
        }

        @Override
        protected Yaml createDefault() {
            return new Yaml();
        }

        @Override
        protected void loadReferencedFilesFrom(Yaml outputYaml, File sourceYamlFile)
                throws
                    AdmiralConfigurationException,
                InvalidEnumException,
                    InvalidBooleanException,
                    PropertyNotFoundException {
            // Load Theme Yaml if requested.
            if (outputYaml.theme_file != null) {
                final FileService fileService;
                try {
                    fileService = new FileService(sourceYamlFile.getParentFile());
                } catch (IOException e) {
                    throw new AdmiralConfigurationException("Admiral Preferences Directory" , e.getMessage());
                }
                File themeFile = fileService.relativeFile(outputYaml.theme_file);
                outputYaml.theme_filefile = themeFile;
                outputYaml.outputStyler = OutputStyler.loadTheme(themeFile);
            } else {
                outputYaml.outputStyler = new OutputStyler(true);
            }
        }

        @Override
        protected List<PreferenceSourceCandidate> getPossiblePreferenceFiles() {
            return possiblePreferencesFiles;
        }
    }
}
