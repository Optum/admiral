package com.optum.admiral;

import com.optum.admiral.booter.Booter;
import com.optum.admiral.booter.BooterFactory;
import com.optum.admiral.booter.TheAdmiralWayBooterFactory;
import com.optum.admiral.booter.TheBestWayBooterFactory;
import com.optum.admiral.booter.TheDockerComposeWayBooterFactory;
import com.optum.admiral.cli.AbstractOptioner;
import com.optum.admiral.cli.Optioner;
import com.optum.admiral.cli.OptionerSource;
import com.optum.admiral.event.AdmiralEventPublisher;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.util.Arrays;


/**
 * These are the options needed when booting Docker Admiral.
 * In other words, these options set final members in the principal Docker Admiral objects.
 *
 */
public class AdmiralBootOptions implements OptionerSource {
    private BooterFactory booterFactory;
    private String preferencesFilename;

    public String getPreferencesFilename() {
        return preferencesFilename;
    }

    final Optioner admiralOPT = new AbstractOptioner(false) {
        private final Option admiralOption = org.apache.commons.cli.Option.builder("a")
                .longOpt("admiral")
                    .desc("Admiral YAML file to use")
                    .hasArg(true)
                    .argName("admiral.yml")
                    .build();

        @Override
        public Option getOption() {
            return admiralOption;
        }

        @Override
        public void process(CommandLine commandLine) {
            booterFactory = new TheAdmiralWayBooterFactory(commandLine.getOptionValue(admiralOption));
        }
    };

    final Optioner fileOPT = new AbstractOptioner(false) {
        final Option fileOption = org.apache.commons.cli.Option.builder("f")
                .longOpt("file")
                .desc("Docker-compose YAML file to use")
                .hasArgs()
                .argName("docker-compose.yml")
                .build();

        @Override
        public Option getOption() {
            return fileOption;
        }

        @Override
        public void process(CommandLine commandLine) {
            booterFactory = new TheDockerComposeWayBooterFactory(Arrays.asList(commandLine.getOptionValues(fileOption)));
        }
    };

    final Optioner preferencesOPT = new AbstractOptioner(true) {
        private final Option preferencesOption = org.apache.commons.cli.Option.builder("p")
                .longOpt("preferences")
                .desc("Preferences YAML file to use")
                .hasArg(true)
                .argName("preferences.yml")
                .build();

        @Override
        public Option getOption() {
            return preferencesOption;
        }

        @Override
        public void process(CommandLine commandLine) {
            preferencesFilename = commandLine.getOptionValue(preferencesOption);
        }
    };


    final Optioner versionOPT = new AbstractOptioner(true) {
        final Option versionOption = org.apache.commons.cli.Option.builder()
                .longOpt("version")
                .desc("Show the version and exit")
                .hasArg(false)
                .build();

        @Override
        public Option getOption() {
            return versionOption;
        }

        @Override
        public void process(CommandLine commandLine) {
            throw new CleanExit(Version.VERSION);
        }
    };

    final Optioner helpOPT = new AbstractOptioner(true) {
        final Option helpOption = org.apache.commons.cli.Option.builder()
                .longOpt("help")
                .desc("Show this help and exit")
                .hasArg(false)
                .build();

        @Override
        public Option getOption() {
            return helpOption;
        }

        @Override
        public void process(CommandLine commandLine) {
            throw new HelpExit();
        }
    };

    public Booter createBooter(AdmiralOptions admiralOptions, AdmiralEventPublisher admiralEventPublisher) {
        if (booterFactory==null) {
            booterFactory = new TheBestWayBooterFactory();
        }
        return booterFactory.createBooter(admiralOptions, admiralEventPublisher);
    }

    public Optioner[] getOptioners() {
        return new Optioner[] { admiralOPT, fileOPT, helpOPT, preferencesOPT, versionOPT};
    }
}
