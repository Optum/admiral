package com.optum.admiral.model;

public interface Debugger {
    void log(String s);
    void log(String prompt, String value);
}
