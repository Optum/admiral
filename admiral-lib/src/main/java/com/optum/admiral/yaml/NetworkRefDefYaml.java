package com.optum.admiral.yaml;

import java.util.Collections;
import java.util.List;

public class NetworkRefDefYaml {
    List<String> aliases = Collections.emptyList();

    public NetworkRefDefYaml() {
        System.out.println("NRDY ");
    }

    public NetworkRefDefYaml(String s) {
        System.out.println("NRDY " + s);
    }

//    public void setAliases(List<String> aliases) {
//        System.out.println("Set aliases " + aliases);
//        if (aliases==null)
//            return;
//        aliases = aliases;
//    }
//
//    public List<String> getAliases() {
//        return aliases;
//    }
}
