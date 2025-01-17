package com.optum.admiral;

import com.optum.admiral.cli.AbstractOptioner;
import com.optum.admiral.cli.Optioner;
import com.optum.admiral.cli.OptionerSource;
import com.optum.admiral.preferences.PreferenceResult;
import com.optum.admiral.preferences.PreferenceTracker;
import com.optum.admiral.preferences.PreferenceYaml;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.util.Set;

public class AdmiralOptions {
    public final String applicationName;
    public final boolean parallelizePostCreateExecutes;
//    public final boolean splitOutputFromStdoutAndStderr;
    public final boolean killContainersWhenStopping;
    public final boolean waitForContainersWhenStarting;

    public final Set<PreferenceResult> tracker;

    private AdmiralOptions(Builder admiralOptionsBuilder) {
        this.applicationName = admiralOptionsBuilder.applicationName;
        this.parallelizePostCreateExecutes = admiralOptionsBuilder.parallelizePostCreateExecutes;
//        this.splitOutputFromStdoutAndStderr = admiralOptionsBuilder.splitOutputFromStdoutAndStderr;
        this.killContainersWhenStopping = admiralOptionsBuilder.killContainersWhenStopping;
        this.waitForContainersWhenStarting = admiralOptionsBuilder.waitForContainersWhenStarting;
        this.tracker = admiralOptionsBuilder.getPreferenceResults();
    }

    public static class Builder extends PreferenceTracker implements OptionerSource, com.optum.admiral.cli.Builder<AdmiralOptions> {
        public final String applicationName;
        public boolean parallelizePostCreateExecutes;
//        public boolean splitOutputFromStdoutAndStderr;
        public boolean killContainersWhenStopping;
        public boolean waitForContainersWhenStarting;

        public Builder(String applicationName) {
            this.applicationName = applicationName;
        }

        final Optioner killOPT = new AbstractOptioner(false) {
            final Option killOption = Option.builder()
                        .longOpt("kill")
                        .desc("Causes stop to immediately kill a container.")
                        .hasArg(false)
                        .build();

            @Override
            public Option getOption() {
                return killOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB("killContainersWhenStopping", true, "command line flag --kill");
            }
        };

        final Optioner nokillOPT = new AbstractOptioner(false) {
            final Option nokillOption = Option.builder()
                    .longOpt("no-kill")
                    .desc("Causes stop to wait normally before killing a container.")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return nokillOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB("killContainersWhenStopping", true, "command line flag --no-kill");
            }
        };

        final Optioner waitOPT = new AbstractOptioner(false) {
            final Option waitOption = Option.builder()
                    .longOpt("wait")
                    .desc("Wait for containers when starting")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return waitOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB("waitForContainersWhenStarting", true, "command line flag --wait");
                waitForContainersWhenStarting = true;
            }
        };

        final Optioner nowaitOPT = new AbstractOptioner(false) {
            final Option nowaitOption = Option.builder()
                    .longOpt("no-wait")
                    .desc("Don't wait for containers when starting")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return nowaitOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB("waitForContainersWhenStarting", false, "command line flag --no-wait");
                waitForContainersWhenStarting = false;
            }
        };

        @Override
        public Optioner[] getOptioners() {
            return new Optioner[] { killOPT, nokillOPT, waitOPT, nowaitOPT };
        }

        @Override
        public AdmiralOptions getData() {
            return new AdmiralOptions(this);
        }
    }

    public static class Yaml extends PreferenceYaml {
        public Boolean parallelizePostCreateExecutes;
//        public Boolean splitOutputFromStdoutAndStderr;
        public Boolean killContainersWhenStopping;
        public Boolean waitForContainersWhenStarting;

        public Builder updateBuilder(Builder builder) {
            builder.setB("parallelizePostCreateExecutes", false, parallelizePostCreateExecutes, source);
//            builder.setB("splitOutputFromStdoutAndStderr", false, splitOutputFromStdoutAndStderr, source);
            builder.setB("killContainersWhenStopping", false, killContainersWhenStopping, source);
            builder.setB("waitForContainersWhenStarting", true, waitForContainersWhenStarting, source);
            return builder;
        }

        public Builder createBuilder(String applicationName) {
            return updateBuilder(new Builder(applicationName));
        }
    }
}
