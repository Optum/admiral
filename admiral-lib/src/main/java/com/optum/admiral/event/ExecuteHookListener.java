package com.optum.admiral.event;

public interface ExecuteHookListener {
    void startLine(String cmdId, String s);
    void stdoutLine(String cmdId, String s);
    void stderrLine(String cmdId, String s);
    void doneLine(String cmdId, String s);
}
