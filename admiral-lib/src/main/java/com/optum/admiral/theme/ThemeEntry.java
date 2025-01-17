package com.optum.admiral.theme;

import com.optum.admiral.io.OutputStyle;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class ThemeEntry implements OutputStyle {
    // -1 means DEFAULT
    public static final ThemeEntry defaultTheme = new ThemeEntry(-1, -1, AttributedStyle.DEFAULT);

    public int foreground;
    public int background;
    public AttributedStyle style;

    public ThemeEntry(int foreground, int background, AttributedStyle style) {
        this.foreground = foreground;
        this.background = background;
        this.style = style;
    }

    @Override
    public String format(String s) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        AttributedStyle attributedStyle = style;
        if (foreground>=0) {
            attributedStyle = attributedStyle.foreground(foreground);
        }
        if (background>=0) {
            attributedStyle = attributedStyle.background(background);
        }
        builder.style(attributedStyle);
        builder.append(s);
        builder.style(AttributedStyle.DEFAULT);

        return builder.toAnsi();
    }
}
