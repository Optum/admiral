package com.optum.admiral.model;

public class Network {
    final String name;
    final String fullName;
    final String id;

    public Network(String name, String fullName, String id) {
        this.name = name;
        this.fullName = fullName;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getId() {
        return id;
    }
}
