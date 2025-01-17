package com.optum.admiral.type;

public class UserProvidedVariable {
    public String name;
    public TYPE type;
    public String prompt;
    public String _default;
    public String required;
    public String hidden;
    public String format;

    public void setDefault(String _default) {
        this._default = _default;
    }

    public enum TYPE {
        TEXT,
        INTEGER,
        DATETIME;
    }
}
