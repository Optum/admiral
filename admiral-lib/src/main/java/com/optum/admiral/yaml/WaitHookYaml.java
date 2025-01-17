package com.optum.admiral.yaml;

public class WaitHookYaml {
    public String id;
    public String url;
    public String search;
    public String success_when_redirected = "false";
    public String host = "localhost";
    public String port;
    public String interval = "30s";
    public String timeout = "30s";
    public String retries = "3";
    public String start_period = "0s";
    public String minimum_interval = null;
    public String rewait_period = null;
    public String rewait_interval = null;
    public String disable = "false";
}
