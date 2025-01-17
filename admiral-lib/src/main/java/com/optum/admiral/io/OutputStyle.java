package com.optum.admiral.io;

public interface OutputStyle {
    String format(String s);

    static OutputStyle unformatted = new OutputStyle() {
        @Override
        public String format(String s) {
            return s;
        }
    };
}
