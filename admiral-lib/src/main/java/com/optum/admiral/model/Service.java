package com.optum.admiral.model;

import com.optum.admiral.config.AdmiralServiceConfig;

public class Service {
    private final String name;

    public Service(String name, AdmiralServiceConfig admiralServiceConfig) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
