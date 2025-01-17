package com.optum.admiral.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class OptionerProcessor {
    protected final String[] args;
    protected final OptionerSource optionerSource;

    private Options optionsCache = null;

    public OptionerProcessor(OptionerSource optionerSource, String[] args) {
        this.optionerSource = optionerSource;
        this.args = args;
    }

    public void preprocess() throws ParseException {
        CommandLine cl = getCommandLine();

        for(Optioner optioner : optionerSource.getOptioners()) {
            if (optioner.isPreOption()) {
                if (cl.hasOption(optioner.getOption())) {
                    optioner.process(cl);
                }
            }
        }
    }

    private CommandLine getCommandLine() throws ParseException {
        final CommandLineParser clp = new DefaultParser();
        final Options options = new Options();
        for(Optioner optioner : optionerSource.getOptioners()) {
            options.addOption(optioner.getOption());
        }
        optionsCache = options;
        return clp.parse(options, args);
    }

    public void process() throws ParseException {
        _process();
    }

    // We don't want to leak the CommandLine, but we want it available to derived classes.
    // Storing it as a member variable isn't null-safe.
    protected CommandLine _process() throws ParseException {
        CommandLine cl = getCommandLine();

        for(Optioner optioner : optionerSource.getOptioners()) {
            if ( (!optioner.isPreOption()) && cl.hasOption(optioner.getOption())) {
                optioner.process(cl);
            }
        }
        return cl;
    }

    public Options getOptionsCache() {
        return optionsCache;
    }

}
