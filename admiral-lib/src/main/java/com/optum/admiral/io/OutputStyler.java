package com.optum.admiral.io;

import com.optum.admiral.theme.ThemeEntry;
import com.optum.admiral.type.Assume;
import com.optum.admiral.yaml.ThemeEntryYaml;
import com.optum.admiral.yaml.ThemeEntryYaml.ANSICOLOR;
import com.optum.admiral.yaml.ThemeYaml;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;
import org.jline.utils.AttributedStyle;

import java.io.File;

import static com.optum.admiral.yaml.ThemeEntryYaml.ANSICOLOR.BLACK;
import static com.optum.admiral.yaml.ThemeEntryYaml.ANSICOLOR.BLUE;
import static com.optum.admiral.yaml.ThemeEntryYaml.ANSICOLOR.CYAN;
import static com.optum.admiral.yaml.ThemeEntryYaml.ANSICOLOR.DEFAULT;
import static com.optum.admiral.yaml.ThemeEntryYaml.ANSICOLOR.GREEN;
import static com.optum.admiral.yaml.ThemeEntryYaml.ANSICOLOR.MAGENTA;
import static com.optum.admiral.yaml.ThemeEntryYaml.ANSICOLOR.RED;
import static com.optum.admiral.yaml.ThemeEntryYaml.ANSICOLOR.WHITE;
import static com.optum.admiral.yaml.ThemeEntryYaml.ANSICOLOR.YELLOW;

public class OutputStyler {
    public OutputStyle barlinebody = OutputStyle.unformatted;
    public OutputStyle barlineheader = OutputStyle.unformatted;
    public OutputStyle builtin = OutputStyle.unformatted;
    public OutputStyle command = OutputStyle.unformatted;
    public OutputStyle console = OutputStyle.unformatted;
    public OutputStyle container = OutputStyle.unformatted;
    public OutputStyle containerStateCreated = OutputStyle.unformatted;
    public OutputStyle containerStateExited = OutputStyle.unformatted;
    public OutputStyle containerStateNotCreated = OutputStyle.unformatted;
    public OutputStyle containerStatePaused = OutputStyle.unformatted;
    public OutputStyle containerStateRunning = OutputStyle.unformatted;
    public OutputStyle debug = OutputStyle.unformatted;
    public OutputStyle error = OutputStyle.unformatted;
    public OutputStyle errorFocus = OutputStyle.unformatted;
    public OutputStyle file = OutputStyle.unformatted;
    public OutputStyle group = OutputStyle.unformatted;
    public OutputStyle heading = OutputStyle.unformatted;
    public OutputStyle help = OutputStyle.unformatted;
    public OutputStyle image = OutputStyle.unformatted;
    public OutputStyle linebody = OutputStyle.unformatted;
    public OutputStyle lineheader = OutputStyle.unformatted;
    public OutputStyle log = OutputStyle.unformatted;
    public OutputStyle network = OutputStyle.unformatted;
    public OutputStyle networkHeading = OutputStyle.unformatted;
    public OutputStyle port = OutputStyle.unformatted;
    public OutputStyle plain = OutputStyle.unformatted;
    public OutputStyle prompt = OutputStyle.unformatted;
    public OutputStyle section = OutputStyle.unformatted;
    public OutputStyle service = OutputStyle.unformatted;
    public OutputStyle serviceHeading = OutputStyle.unformatted;
    public OutputStyle subsection = OutputStyle.unformatted;
    public OutputStyle system = OutputStyle.unformatted;
    public OutputStyle url = OutputStyle.unformatted;
    public OutputStyle userprovided = OutputStyle.unformatted;
    public OutputStyle value = OutputStyle.unformatted;
    public OutputStyle valueFalse = OutputStyle.unformatted;
    public OutputStyle valueTrue = OutputStyle.unformatted;
    public OutputStyle verbose = OutputStyle.unformatted;
    public OutputStyle warning = OutputStyle.unformatted;

    public OutputStyler(boolean colors) {
        if (colors)
            defaults();
    }

    public String formatAssume(Assume assume) {
        if (assume == Assume.EXITED) {
            return containerStateExited.format("[EXITED]");
        } else if (assume == Assume.RUNNING) {
            return containerStateRunning.format("[RUNNING]");
        } else if (assume == Assume.PAUSED) {
            return containerStatePaused.format("[PAUSED]");
        } else if (assume == Assume.NOTCREATED) {
            return containerStateNotCreated.format("[NOTCREATED]");
        } else if (assume == Assume.CREATED) {
            return containerStateCreated.format("[CREATED]");
        } else {
            return "[" + assume + "]";
        }
    }

    public String formatState(String state) {
        if ("exited".equals(state)) {
            return containerStateExited.format("[EXITED]");
        } else if ("running".equals(state)) {
            return containerStateRunning.format("[RUNNING]");
        } else if ("paused".equals(state)) {
            return containerStatePaused.format("[PAUSED]");
        } else if ("not created".equals(state)) {
            return containerStateNotCreated.format("[NOTCREATED]");
        } else if ("created".equals(state)) {
            return containerStateCreated.format("[CREATED]");
        } else {
            return "[" + state + "]";
        }
    }

    public String formatValue(String v) {
        if ("false".equalsIgnoreCase(v) || "no".equalsIgnoreCase(v)) {
            return valueFalse.format(v);
        } else if ("true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v)) {
            return valueTrue.format(v);
        } else {
            return value.format(v);
        }
    }
    
    public static OutputStyler loadTheme(File themeFile) throws InvalidBooleanException, PropertyNotFoundException, AdmiralConfigurationException, InvalidEnumException {
        ThemeYaml themeYaml = ThemeYaml.loadFromYamlFile(themeFile);
        OutputStyler outputStyler = new OutputStyler(true);
        if (themeYaml!=null) {
            outputStyler.barlineheader = get(themeYaml, "barlinebody", outputStyler.barlineheader);
            outputStyler.barlineheader = get(themeYaml, "barlineheader", outputStyler.barlineheader);
            outputStyler.builtin = get(themeYaml, "builtin", outputStyler.builtin);
            outputStyler.command = get(themeYaml, "command", outputStyler.command);
            outputStyler.console = get(themeYaml, "console", outputStyler.console);
            outputStyler.container = get(themeYaml, "container", outputStyler.container);
            outputStyler.containerStateCreated = get(themeYaml, "containerStateCreated", outputStyler.containerStateCreated);
            outputStyler.containerStateExited = get(themeYaml, "containerStateExited", outputStyler.containerStateExited);
            outputStyler.containerStateNotCreated = get(themeYaml, "containerStateNotCreated", outputStyler.containerStateNotCreated);
            outputStyler.containerStatePaused = get(themeYaml, "containerStatePaused", outputStyler.containerStatePaused);
            outputStyler.containerStateRunning = get(themeYaml, "containerStateRunning", outputStyler.containerStateRunning);
            outputStyler.debug = get(themeYaml, "debug", outputStyler.debug);
            outputStyler.error = get(themeYaml, "error", outputStyler.error);
            outputStyler.errorFocus = get(themeYaml, "errorFocus", outputStyler.errorFocus);
            outputStyler.file = get(themeYaml, "file", outputStyler.file);
            outputStyler.heading = get(themeYaml, "heading", outputStyler.heading);
            outputStyler.help = get(themeYaml, "help", outputStyler.help);
            outputStyler.group = get(themeYaml, "group", outputStyler.group);
            outputStyler.image = get(themeYaml, "image", outputStyler.image);
            outputStyler.lineheader = get(themeYaml, "linebody", outputStyler.lineheader);
            outputStyler.lineheader = get(themeYaml, "lineheader", outputStyler.lineheader);
            outputStyler.log = get(themeYaml, "log", outputStyler.log);
            outputStyler.network = get(themeYaml, "network", outputStyler.network);
            outputStyler.networkHeading = get(themeYaml, "networkHeading", outputStyler.network);
            outputStyler.plain = get(themeYaml, "plain", outputStyler.plain);
            outputStyler.port = get(themeYaml, "port", outputStyler.port);
            outputStyler.prompt = get(themeYaml, "prompt", outputStyler.prompt);
            outputStyler.section = get(themeYaml, "section",outputStyler.section);
            outputStyler.service = get(themeYaml, "service", outputStyler.service);
            outputStyler.serviceHeading = get(themeYaml, "serviceHeading", outputStyler.service);
            outputStyler.subsection = get(themeYaml, "subsection",outputStyler.section);
            outputStyler.system = get(themeYaml, "system",outputStyler.system);
            outputStyler.url = get(themeYaml, "url", outputStyler.url);
            outputStyler.userprovided = get(themeYaml, "userprovided", outputStyler.userprovided);
            outputStyler.value = get(themeYaml, "value", outputStyler.value);
            outputStyler.valueFalse = get(themeYaml, "valueFalse", outputStyler.valueFalse);
            outputStyler.valueTrue = get(themeYaml, "valueTrue", outputStyler.valueTrue);
            outputStyler.verbose = get(themeYaml, "verbose", outputStyler.verbose);
            outputStyler.warning = get(themeYaml, "warning", outputStyler.warning);
        }
        return outputStyler;
    }

    public static ThemeEntry make(ANSICOLOR foreground, ANSICOLOR background, String style) {
        final int fg = foreground.getValue();
        final int bg = background.getValue();
        AttributedStyle st = convertStyle(style);
        return new ThemeEntry(fg, bg, st);
    }

    public static OutputStyle get(ThemeYaml themeYaml, String name, OutputStyle defaultValue) {
        ThemeEntryYaml themeEntryYaml = themeYaml.get(name);
        if (themeEntryYaml==null)
            return defaultValue;
        else {
            final int fg = themeEntryYaml.foreground.getValue();
            final int bg = themeEntryYaml.background.getValue();
            AttributedStyle st = convertStyle(themeEntryYaml.style);
            return new ThemeEntry(fg, bg, st);
        }
    }

    public static AttributedStyle convertStyle(String s) {
        if (s==null)
            return AttributedStyle.DEFAULT;

        switch (s) {
            case BOLD:
                return AttributedStyle.BOLD;
            case INVERSE:
                return AttributedStyle.INVERSE;
            default:
                return AttributedStyle.DEFAULT;
        }
    }

    private void defaults() {
        barlinebody = make(DEFAULT, DEFAULT, null);
        barlineheader = make(MAGENTA, DEFAULT, null);
        builtin = make(CYAN, DEFAULT, INVERSE);
        command = make(GREEN, DEFAULT, null);
        console = make(DEFAULT, DEFAULT, null);
        container = make(BLUE, DEFAULT, null);
        containerStateCreated = make(CYAN, DEFAULT, INVERSE);
        containerStateExited = make(RED, DEFAULT, INVERSE);
        containerStateNotCreated = make(MAGENTA, DEFAULT, INVERSE);
        containerStatePaused = make(YELLOW, DEFAULT, INVERSE);
        containerStateRunning = make(GREEN, DEFAULT, INVERSE);
        debug = make(BLACK, YELLOW, null);
        error = make(RED, DEFAULT, BOLD);
        errorFocus = make(RED, DEFAULT, INVERSE);
        file = make(YELLOW, DEFAULT, null);
        group = make(RED, DEFAULT, null);
        heading = make(DEFAULT, DEFAULT, BOLD);
        help = make(YELLOW, DEFAULT, null);
        image = make(CYAN, DEFAULT, null);
        linebody = make(DEFAULT, DEFAULT, INVERSE);
        lineheader = make(MAGENTA, DEFAULT, INVERSE);
        log = make(DEFAULT, DEFAULT, null);
        network = make(CYAN, DEFAULT, null);
        networkHeading = make(CYAN, DEFAULT, BOLD);
        port = make(YELLOW, DEFAULT, INVERSE);
        plain = OutputStyle.unformatted;
        prompt = make(GREEN, DEFAULT, BOLD);
        section = make(DEFAULT, DEFAULT, INVERSE);
        service = make(BLUE, DEFAULT, INVERSE);
        serviceHeading = make(BLUE, WHITE, INVERSE);
        subsection = make(MAGENTA, DEFAULT, BOLD);
        system = make(RED, DEFAULT, INVERSE);
        url = make(BLUE, DEFAULT, null);
        userprovided = make(CYAN, DEFAULT, INVERSE);
        value = make(GREEN, DEFAULT, null);
        valueFalse = make(RED, DEFAULT, INVERSE);
        valueTrue = make(GREEN, DEFAULT, INVERSE);
        verbose = make(MAGENTA, DEFAULT, null);
        warning = make(YELLOW, DEFAULT, BOLD);
    }
    
    private static final String INVERSE = "INVERSE";
    private static final String BOLD = "BOLD";

}
