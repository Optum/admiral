package com.optum.admiral.preferences;

import com.optum.admiral.type.Duration;

public enum TimerStyle {
    PRETTY {
        @Override
        public String format(long duration) {
            return Duration.prettyMS(duration);
        }
    },
    PRETTYNOMS {
        @Override
        public String format(long duration) {
            return Duration.prettyMS(duration, false);
        }
    },
    CONCISE {
        @Override
        public String format(long duration) {
            return Duration.conciseMS(duration);
        }
    },
    CONCISENOMS {
        @Override
        public String format(long duration) {
            return Duration.conciseMS(duration, false);
        }
    },
    MS {
        @Override
        public String format(long duration) {
            return Duration.rawMS(duration);
        }
    },
    S {
        @Override
        public String format(long duration) {
            return Duration.rawS(duration);
        }
    };

    public abstract String format(long duration);
}
