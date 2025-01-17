package com.optum.admiral.model;

public interface ProgressMessage {
    String id();
    String status();
    String stream();
    String error();
    String progress();
    ProgressDetail progressDetail();
}
