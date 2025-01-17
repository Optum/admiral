package com.optum.admiral.preferences;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.TreeSet;

public class PreferenceTracker {
    private final Set<PreferenceResult> values = new TreeSet<>();

    /**
     * This isn't just add... it first removes, then adds, since Set.add will not update.
     * @param preferenceResult
     */
    public void addOrUpdate(PreferenceResult preferenceResult) {
        values.remove(preferenceResult);
        values.add(preferenceResult);
    }

    public void overrideB(String fieldName, boolean newValue, String source) {
        doit(fieldName, newValue, source, PreferenceResult.SOURCETYPE.OPTION);
    }

    public void setO(String fieldName, Object defaultValue, Object candidateValue, File valueSource) {
        if (valueSource==null) {
            setO(fieldName, defaultValue, candidateValue, "builtin", PreferenceResult.SOURCETYPE.DEFAULT);
        } else {
            setO(fieldName, defaultValue, candidateValue, valueSource.getAbsolutePath(), PreferenceResult.SOURCETYPE.FILE);
        }
    }

    public void setO(String fieldName, Object defaultValue, Object candidateValue, String valueSource, PreferenceResult.SOURCETYPE sourcetype) {
        final String source;
        final Object value;
        if (candidateValue==null) {
            source = "default";
            value = defaultValue;
            sourcetype = PreferenceResult.SOURCETYPE.DEFAULT;
        } else {
            source = valueSource;
            value = candidateValue;
        }
        doitO(fieldName, value, source, sourcetype);
    }

    private void doitO(String fieldName, Object value, String source, PreferenceResult.SOURCETYPE sourcetype) {
        try {
            final Field field = getClass().getField(fieldName);
            field.set(this, value);
            addOrUpdate(new PreferenceResult(fieldName, (value==null) ? "null" : value.toString(), source, sourcetype));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            System.out.println("Well, that's not supposed to happen.");
            e.printStackTrace();
        }
    }

    public void setB(String fieldName, boolean defaultValue, Boolean candidateValue, File valueSource) {
        if (valueSource==null) {
            setB(fieldName, defaultValue, candidateValue, "builtin", PreferenceResult.SOURCETYPE.DEFAULT);
        } else {
            setB(fieldName, defaultValue, candidateValue, valueSource.getAbsolutePath(), PreferenceResult.SOURCETYPE.FILE);
        }
    }

    public void setTS(String fieldName, TimerStyle defaultValue, TimerStyle candidateValue, File valueSource) {
        if (valueSource==null) {
            setTS(fieldName, defaultValue, candidateValue, "builtin", PreferenceResult.SOURCETYPE.DEFAULT);
        } else {
            setTS(fieldName, defaultValue, candidateValue, valueSource.getAbsolutePath(), PreferenceResult.SOURCETYPE.FILE);
        }
    }

    public void setB(String fieldName, boolean defaultValue, Boolean candidateValue, String valueSource, PreferenceResult.SOURCETYPE sourcetype) {
        final String source;
        final boolean value;
        if (candidateValue==null) {
            source = "default";
            value = defaultValue;
            sourcetype = PreferenceResult.SOURCETYPE.DEFAULT;
        } else {
            source = valueSource;
            value = candidateValue;
        }
        doit(fieldName, value, source, sourcetype);
    }

    public void setTS(String fieldName, TimerStyle defaultValue, TimerStyle candidateValue, String valueSource, PreferenceResult.SOURCETYPE sourcetype) {
        final String source;
        final TimerStyle value;
        if (candidateValue==null) {
            source = "default";
            value = defaultValue;
            sourcetype = PreferenceResult.SOURCETYPE.DEFAULT;
        } else {
            source = valueSource;
            value = candidateValue;
        }
        doitTS(fieldName, value, source, sourcetype);
    }

    private void doit(String fieldName, boolean value, String source, PreferenceResult.SOURCETYPE sourcetype) {
        try {
            final Field field = getClass().getField(fieldName);
            field.set(this, value);
            addOrUpdate(new PreferenceResult(fieldName, Boolean.toString(value), source, sourcetype));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            System.out.println("Well, that's not supposed to happen.");
            e.printStackTrace();
        }
    }

    private void doitTS(String fieldName, TimerStyle value, String source, PreferenceResult.SOURCETYPE sourcetype) {
        try {
            final Field field = getClass().getField(fieldName);
            field.set(this, value);
            addOrUpdate(new PreferenceResult(fieldName, value.toString(), source, sourcetype));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            System.out.println("Well, that's not supposed to happen.");
            e.printStackTrace();
        }
    }

    public Set<PreferenceResult> getPreferenceResults() {
        return values;
    }
}
