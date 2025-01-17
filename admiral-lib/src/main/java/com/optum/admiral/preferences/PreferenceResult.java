package com.optum.admiral.preferences;

import com.optum.admiral.io.OutputStyler;

/**
 * It's kind of a mess to keep track of all the different preferences, where the come from, etc.
 * This class tries to help.
 */
public final class PreferenceResult implements Comparable<PreferenceResult> {
    private final String preference;
    private final String value;
    private final String source;
    private final SOURCETYPE sourcetype;

    enum SOURCETYPE {
        DEFAULT,
        FILE,
        OPTION
    }

    public PreferenceResult(String preference, String value, String source, SOURCETYPE sourcetype) {
        this.preference = preference;
        this.value = value;
        this.source = source;
        this.sourcetype = sourcetype;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof PreferenceResult))
            return false;

        PreferenceResult other = (PreferenceResult) o;
        return preference.equals(other.preference);
    }

    @Override
    public int hashCode() {
        return preference.hashCode();
    }

    @Override
    public int compareTo(PreferenceResult other) {
        return preference.compareTo(other.preference);
    }

    public String toLine(OutputStyler os, String indent, boolean showSource) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent);
        sb.append(os.heading.format(preference));
        sb.append(os.heading.format(": "));
        sb.append(os.value.format(value));
        if (showSource) {
            sb.append(" ");
            switch (sourcetype) {
                case FILE:
                    sb.append(os.file.format("[" + source + "]"));
                    break;
                case OPTION:
                    sb.append(os.system.format("[" + source + "]"));
                    break;
                case DEFAULT:
                default:
                    sb.append(os.builtin.format("[" + source + "]"));
            }
        }
        return sb.toString();
    }

}
