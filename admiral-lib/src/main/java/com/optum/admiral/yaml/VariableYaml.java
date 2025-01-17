package com.optum.admiral.yaml;

public class VariableYaml {
    public String name;
    public String first_code_version;
    public String last_code_version;

    // Used by SnakeYaml when variables are listed as objects like " - name: MYVAR"
    public VariableYaml() {
    }

    // Used by SnakeYaml when variables are just listed as " - MYVAR".
    public VariableYaml(String name) {
        this.name = name;
    }

    public String toString() {
        return String.format("%s %s %s", name, first_code_version, last_code_version);
    }
}
