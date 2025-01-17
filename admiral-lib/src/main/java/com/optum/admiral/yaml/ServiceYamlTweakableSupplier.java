package com.optum.admiral.yaml;

import java.util.List;

public interface ServiceYamlTweakableSupplier {
    List<String> getDig();
    List<String> getExposedPorts();
    List<String> getGroups();
    public List<String> getDependsOn();
}
