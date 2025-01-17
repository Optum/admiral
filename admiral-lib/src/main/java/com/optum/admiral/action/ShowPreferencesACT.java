package com.optum.admiral.action;

import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.preferences.PreferenceResult;
import com.optum.admiral.preferences.UXPreferences;

public class ShowPreferencesACT {
    private final UXPreferences uxPreferences;
    private final AdmiralOptions admiralOptions;
    private final OutputStyler styler;
    private final OutputWriter writer;

    public ShowPreferencesACT(UXPreferences uxPreferences, OutputWriter outputWriter) {
        this.uxPreferences = uxPreferences;
        this.admiralOptions = uxPreferences.admiralOptions;
        this.styler = uxPreferences.outputStyler;
        this.writer = outputWriter;
    }

    public void perform() {
        final boolean showSource = uxPreferences.outputPreferences.showSource;

        writer.outln(styler.section.format("admiral_options:"));
        for(PreferenceResult preferenceResult : admiralOptions.tracker) {
            writer.outln(preferenceResult.toLine(styler, "  ", showSource));
        }
        writer.outln("");
        writer.outln(styler.section.format("output_preferences:"));
        for(PreferenceResult preferenceResult : uxPreferences.outputPreferences.tracker) {
            writer.outln(preferenceResult.toLine(styler, "  ", showSource));
        }
        writer.outln("");
        final String theme;
        if (uxPreferences.theme_filename ==null) {
            theme = styler.value.format("none") + " " + styler.builtin.format("[default]");
        } else {
            theme = styler.value.format(uxPreferences.theme_filename) +
                    (showSource ? " [" + styler.file.format(uxPreferences.theme_file.getAbsolutePath()) + "]" : "");
        }
        writer.outln(styler.section.format("theme_file:") + " " + theme);
    }

}

