package com.optum.admiral.yaml;

import org.jline.utils.AttributedStyle;

public class ThemeEntryYaml {
    public ANSICOLOR foreground = ANSICOLOR.DEFAULT;
    public ANSICOLOR background = ANSICOLOR.DEFAULT;
    public String style;

    public enum ANSICOLOR {
        BLACK(AttributedStyle.BLACK),
        RED(AttributedStyle.RED),
        GREEN(AttributedStyle.GREEN),
        YELLOW(AttributedStyle.YELLOW),
        BLUE(AttributedStyle.BLUE),
        MAGENTA(AttributedStyle.MAGENTA),
        CYAN(AttributedStyle.CYAN),
        WHITE(AttributedStyle.WHITE),
        DEFAULT(-1);

        private final int value;

        ANSICOLOR(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
