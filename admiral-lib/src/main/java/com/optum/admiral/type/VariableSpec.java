package com.optum.admiral.type;

public class VariableSpec {
    public final String name;
    public SemanticVersion firstCodeVersion;
    public SemanticVersion lastCodeVersion;

    public VariableSpec(String name, SemanticVersion firstCodeVersion, SemanticVersion lastCodeVersion) {
        this.name = name;
        this.firstCodeVersion = firstCodeVersion;
        this.lastCodeVersion = lastCodeVersion;
    }

    public boolean hasCodeVersionConstraints() {
        return firstCodeVersion!=null || lastCodeVersion!=null;
    }

    // "Inside both edges" is true
    // "Outside either edge" is false, otherwise true
    public boolean isAllowedFor(SemanticVersion codeVersion) {
        if (firstCodeVersion!=null && lastCodeVersion!=null) {
            return codeVersion.atLeast(firstCodeVersion) && codeVersion.atMost(lastCodeVersion);
        }

        if (firstCodeVersion!=null && !codeVersion.atLeast(firstCodeVersion))
            return false;

        if (lastCodeVersion!=null && !codeVersion.atMost(lastCodeVersion))
            return false;

        return true;
    }

    // "Inside both edges" is true.
    // "Inside either edge" is true, otherwise false
    public boolean isRequiredFor(SemanticVersion codeVersion) {
        if (firstCodeVersion!=null && lastCodeVersion!=null) {
            return codeVersion.atLeast(firstCodeVersion) && codeVersion.atMost(lastCodeVersion);
        }

        if (firstCodeVersion!=null && codeVersion.atLeast(firstCodeVersion))
            return true;

        if (lastCodeVersion!=null && codeVersion.atMost(lastCodeVersion))
            return true;

        return false;
    }

}
