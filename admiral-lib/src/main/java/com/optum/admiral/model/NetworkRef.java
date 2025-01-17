package com.optum.admiral.model;

import java.util.ArrayList;
import java.util.List;

public class NetworkRef {
    private final String name;
    private final List<String> aliases = new ArrayList<>();

    public NetworkRef(String name, List<String> aliases) {
        this.name = name;
        this.aliases.addAll(aliases);
    }

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliases;
    }
}
