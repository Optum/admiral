package com.optum.admiral.yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HealthCheckYaml {
    /**
     * This can't be called test or SnakeYaml Parser pukes.
     */
    private List<String> _test;
    public String x_admiral_id = null;
    public String interval = "30s";
    public String timeout = "30s";
    public String retries = "3";
    public String start_period = "0s";
    public String x_admiral_minimum_interval = null;
    public String x_admiral_rewait_period = null;
    public String x_admiral_rewait_interval = null;
    public String disable = "false";

    /**
     * This can't be called getTest() or SnakeYaml Parser pukes.
     */
    public List<String> getArgs() {
        return _test;
    }

    /**
     * This is what SnakeYaml Parser calls to set "test:" as either a list or a string.
     */
    public void setTest(Object o) {
        if (o instanceof String) {
            String s = (String)o;
            _test = Arrays.asList("CMD-SHELL", s);
        } else if (o instanceof List) {
            _test = new ArrayList<>();
            List list = (List)o;
            for(Object item : list) {
                if (item instanceof String) {
                    _test.add((String)item);
                } else {
                    throw new IllegalArgumentException("healthcheck test list item is not a string: " + item);
                }
            }
        }
    }

}
