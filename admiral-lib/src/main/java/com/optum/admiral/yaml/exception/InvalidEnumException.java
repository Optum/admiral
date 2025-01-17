package com.optum.admiral.yaml.exception;

import java.util.ArrayList;
import java.util.List;

public class InvalidEnumException extends Exception {
    private final String source;
    private final String className;
    private final int line;
    private final String propertyName;
    private final String invalidValue;
    private final String valuesListAsString;

    public InvalidEnumException(String source, String className, int line, String propertyName, String invalidValue) {
        super("Invalid Enum Valid");
        this.source = source;
        this.className = className;
        this.line = line;
        this.propertyName = propertyName;
        this.invalidValue = invalidValue;

        List<String> values = new ArrayList<>();
        try {
            Class<?> clazz = Class.forName(className);
            for (Object o : clazz.getEnumConstants()) {
                values.add(o.toString());
            }
        } catch (ClassNotFoundException e) {
            // Ignore and fall through to empty values.
        }
        if (values.isEmpty()) {
            valuesListAsString = "(Automatic listing of valid values failed.  Please see class " + className + " for details)";
        } else {
            valuesListAsString = String.join(", ", values);
        }
    }

    public int getLine() {
        return line;
    }

    public String getSource() {
        return source;
    }

    public String getClassName() {
        return className;
    }

    public String getValidValues() {
        return valuesListAsString;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getInvalidValue() {
        return invalidValue;
    }
}
