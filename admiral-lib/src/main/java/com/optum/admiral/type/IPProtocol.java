package com.optum.admiral.type;

public enum IPProtocol {
    tcp("tcp"),
    udp("udp");

    private final String name;
    IPProtocol(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
