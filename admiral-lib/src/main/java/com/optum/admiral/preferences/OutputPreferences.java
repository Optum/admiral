package com.optum.admiral.preferences;

import com.optum.admiral.cli.AbstractOptioner;
import com.optum.admiral.cli.Optioner;
import com.optum.admiral.cli.OptionerSource;
import com.optum.admiral.io.OutputStyler;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.util.Set;

public class OutputPreferences {
    public final boolean showActionAnnouncements;
    public final boolean showDebug;
    public final boolean showDockerEngineConnectionActivity;
    public final boolean showExecuteProgress;
    public final boolean showExecuteProgressBars;
    public final boolean showExpectedWarnings;
    public final boolean showIDs;
    public final boolean showInitialState;
    public final boolean showLoadActions;
    public final boolean showLogs;
    public final boolean showMonitorProgress;
    public final boolean showServiceHeadings;
    public final boolean showSource;
    public final boolean showStderr;
    public final boolean showStdout;
    public final boolean showTimer;
    public final TimerStyle showTimerStyle;
    public final boolean showVerbose;
    public final boolean showWaitProgress;
    public final boolean showWaitProgressBars;

    public final Set<PreferenceResult> tracker;

    public static OutputPreferences getDefaultOutputPreferences() {
        return new OutputPreferences.Yaml().updateBuilder(new OutputPreferences.Builder(), false).getData();
    }

    private OutputPreferences(OutputPreferences.Builder builder) {
        showActionAnnouncements = builder.showActionAnnouncements;
        showDebug = builder.showDebug;
        showDockerEngineConnectionActivity = builder.showDockerEngineConnectionActivity;
        showExecuteProgress = builder.showExecuteProgress;
        showExecuteProgressBars = builder.showExecuteProgressBars;
        showExpectedWarnings = builder.showExpectedWarnings;
        showIDs = builder.showIDs;
        showInitialState = builder.showInitialState;
        showLoadActions = builder.showLoadActions;
        showLogs = builder.showLogs;
        showMonitorProgress = builder.showMonitorProgress;
        showServiceHeadings = builder.showServiceHeadings;
        showSource = builder.showSource;
        showStderr = builder.showStderr;
        showStdout = builder.showStdout;
        showTimer = builder.showTimer;
        showTimerStyle = builder.showTimerStyle;
        showVerbose = builder.showVerbose;
        showWaitProgress = builder.showWaitProgress;
        showWaitProgressBars = builder.showWaitProgressBars;
        tracker = builder.getPreferenceResults();
    }

    public static class Builder extends PreferenceTracker implements OptionerSource, com.optum.admiral.cli.Builder<OutputPreferences> {
        public OutputStyler outputStyler = new OutputStyler(true);
        public boolean showActionAnnouncements;
        public boolean showDebug;
        public boolean showDockerEngineConnectionActivity;
        public boolean showExecuteProgress;
        public boolean showExecuteProgressBars;
        public boolean showExpectedWarnings;
        public boolean showIDs;
        public boolean showInitialState;
        public boolean showLoadActions;
        public boolean showLogs;
        public boolean showMonitorProgress;
        public boolean showServiceHeadings;
        public boolean showSource;
        public boolean showStderr;
        public boolean showStdout;
        public boolean showTimer;
        public TimerStyle showTimerStyle;
        public boolean showVerbose;
        public boolean showWaitProgress;
        public boolean showWaitProgressBars;

        private final Optioner debugOPT = new AbstractOptioner(false) {
            private final Option debugOption = Option.builder()
                    .longOpt("debug")
                    .desc("Enable debug mode")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return debugOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB("showDebug", true, "command line flag --debug");
            }
        };

        private final Optioner tailOPT = new AbstractOptioner(false) {
            private final Option tailOption = Option.builder("t")
                    .longOpt("tail")
                    .desc("Tail post create commands")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return tailOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB(SHOWEXECUTEPROGRESS, true, "command line flag -t (--tail)");
            }
        };

        private final Optioner verboseOPT = new AbstractOptioner(false) {
            private final Option verboseOption = Option.builder("v")
                    .longOpt("verbose")
                    .desc("Show verbose messages")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return verboseOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB(SHOWVERBOSE, true, "command line flag -v (--verbose)");
            }
        };

        private final Optioner sourceOPT = new AbstractOptioner(false) {
            private final Option sourceOption = Option.builder()
                    .longOpt("source")
                    .desc("Show source of various data")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return sourceOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB(SHOWSOURCE, true, "command line flag --source");
            }
        };

        private final Optioner nosourceOPT = new AbstractOptioner(false) {
            private final Option nosourceOption = Option.builder()
                    .longOpt("no-source")
                    .desc("Don't show source of various data")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return nosourceOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB(SHOWSOURCE, false, "command line flag --no-source");
            }
        };

        private final Optioner noloadOPT = new AbstractOptioner(false) {
            private final Option noloadOption = Option.builder()
                    .longOpt("no-load")
                    .desc("Don't show load actions messages")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return noloadOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB(SHOWLOADACTIONS, false, "command line flag --no-load");
            }
        };


        private final Optioner logsOPT = new AbstractOptioner(false) {
            private final Option logsOption = Option.builder()
                    .longOpt("logs")
                    .desc("Show logs")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return logsOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB(SHOWLOGS, true, "command line flag --logs");
            }
        };

        private final Optioner nologsOPT = new AbstractOptioner(false) {
            private final Option nologsOption = Option.builder()
                    .longOpt("no-logs")
                    .desc("Don't show logs")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return nologsOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB(SHOWLOGS, false, "command line flag --no-logs");
            }
        };

        private final Optioner stderrOPT = new AbstractOptioner(false) {
            private final Option stderrOption = Option.builder()
                    .longOpt("stderr")
                    .desc("Show stderr")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return stderrOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB(SHOWSTDERR, true, "command line flag --stderr");
            }
        };

        private final Optioner nostderrOPT = new AbstractOptioner(false) {
            private final Option nostderrOption = Option.builder()
                    .longOpt("no-stderr")
                    .desc("Don't show stderr")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return nostderrOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB(SHOWSTDERR, false, "command line flag --no-stderr");
            }
        };

        private final Optioner stdoutOPT = new AbstractOptioner(false) {
            private final Option stdoutOption = Option.builder()
                    .longOpt("stdout")
                    .desc("Show stdout")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return stdoutOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB(SHOWSTDOUT, true, "command line flag --stdout");
            }
        };

        private final Optioner nostdoutOPT = new AbstractOptioner(false) {
            private final Option nostdoutOption = Option.builder()
                    .longOpt("no-stdout")
                    .desc("Don't show stdout")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return nostdoutOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB(SHOWSTDOUT, false, "command line flag --no-stdout");
            }
        };

        private final Optioner timerOPT = new AbstractOptioner(false) {
            private final Option timerOption = Option.builder()
                    .longOpt("timer")
                    .desc("Show timer")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return timerOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB(SHOWTIMER, true, "command line flag --timer");
            }
        };

        private final Optioner notimerOPT = new AbstractOptioner(false) {
            private final Option notimerOption = Option.builder()
                    .longOpt("no-timer")
                    .desc("Don't show timer")
                    .hasArg(false)
                    .build();

            @Override
            public Option getOption() {
                return notimerOption;
            }

            @Override
            public void process(CommandLine commandLine) {
                overrideB(SHOWTIMER, false, "command line flag --no-timer");
            }
        };

        @Override
        public Optioner[] getOptioners() {
            return new Optioner[] { debugOPT, tailOPT, verboseOPT, logsOPT, nologsOPT, stderrOPT, nostderrOPT, stdoutOPT, nostdoutOPT, sourceOPT, nosourceOPT, noloadOPT, timerOPT, notimerOPT };
        }

        @Override
        public OutputPreferences getData() {
            return new OutputPreferences(this);
        }
    }

    /**
     * This is where Default values for Admiral Output Preferences are defined.
     */
    public static class Yaml extends PreferenceYaml {
        public Boolean showActionAnnouncements;
        public Boolean showDebug;
        public Boolean showDockerEngineConnectionActivity;
        public Boolean showExecuteProgress;
        public Boolean showExecuteProgressBars;
        public Boolean showExpectedWarnings;
        public Boolean showIDs;
        public Boolean showInitialState;
        public Boolean showLoadActions;
        public Boolean showLogs;
        public Boolean showMonitorProgress;
        public Boolean showServiceHeadings;
        public Boolean showSource;
        public Boolean showStderr;
        public Boolean showStdout;
        public Boolean showTimer;
        public TimerStyle showTimerStyle;
        public Boolean showVerbose;
        public Boolean showWaitProgress;
        public Boolean showWaitProgressBars;

        public Builder updateBuilder(Builder builder, boolean isShell) {
            builder.outputStyler = new OutputStyler(true);
            builder.setB("showActionAnnouncements", isShell, showActionAnnouncements, source);
            builder.setB("showDebug", false, showDebug, source);
            builder.setB("showDockerEngineConnectionActivity", false, showDockerEngineConnectionActivity, source);
            builder.setB(SHOWEXECUTEPROGRESS, false, showExecuteProgress, source);
            builder.setB("showExecuteProgressBars", true, showExecuteProgressBars, source);
            builder.setB("showExpectedWarnings", false, showExpectedWarnings, source);
            builder.setB("showIDs", false, showIDs, source);
            builder.setB("showInitialState", true, showInitialState, source);
            builder.setB("showLoadActions", true, showLoadActions, source);
            builder.setB(SHOWLOGS, !isShell, showLogs, source);
            builder.setB("showMonitorProgress", true, showMonitorProgress, source);
            builder.setB("showServiceHeadings", false, showServiceHeadings, source);
            builder.setB(SHOWSOURCE, true, showSource, source);
            builder.setB(SHOWSTDERR, !isShell, showStderr, source);
            builder.setB(SHOWSTDOUT, !isShell, showStdout, source);
            builder.setB(SHOWTIMER, false, showTimer, source);
            builder.setTS("showTimerStyle", TimerStyle.PRETTYNOMS, showTimerStyle, source);
            builder.setB(SHOWVERBOSE, false, showVerbose, source);
            builder.setB("showWaitProgress", true, showWaitProgress, source);
            builder.setB("showWaitProgressBars", true, showWaitProgressBars, source);
            return builder;
        }

        public Builder createBuilder() {
            return updateBuilder(new Builder(), false);
        }
    }

    private static final String SHOWEXECUTEPROGRESS = "showExecuteProgress";
    private static final String SHOWLOGS = "showLogs";
    private static final String SHOWSOURCE = "showSource";
    private static final String SHOWLOADACTIONS = "showLoadActions";
    private static final String SHOWSTDERR = "showStderr";
    private static final String SHOWSTDOUT = "showStdout";
    private static final String SHOWVERBOSE = "showVerbose";
    private static final String SHOWTIMER = "showTimer";
}
