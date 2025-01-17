package com.optum.admiral.yaml;

import com.optum.admiral.type.Assume;

import java.util.Collections;
import java.util.List;

public class TweaksYaml implements ServiceYamlTweakableSupplier {
    public TweaksYaml() {
    }

    public CommandsYaml commands;
    public Assume assume;

    // These need to be private to force YamlParser to call our set* methods.  The reason we need the method
    // calls is because YamlParser doesn't call with empty collections, it calls with nulls.  We need our methods
    // to convert the null to an empty collection.
    // Null values here means the sections was not in the tweaks block.
    // Empty collections here means the sections WERE in the tweaks block, but was an empty block.
    // *BIG* difference.
    // Null values do nothing.
    // Empty collection erase the existing values.
    public List<String> _dig;
    private List<String> _groups;
    private List<String> _depends_on;
    private List<String> _ports;

    public boolean hasAssume() {
        return assume != null;
    }

    public Assume getAssume() {
        return assume;
    }

    public boolean hasCommands() {
        return commands != null;
    }

    public CommandsYaml getCommands() {
        return commands;
    }

    public void setGroups(List<String> o) {
        if (o == null) {
            _groups = Collections.emptyList();
        } else {
            _groups = o;
        }
    }

    public boolean hasGroupsField() {
        return _groups != null;
    }

    @Override
    public List<String> getGroups() {
        if (_groups == null) return Collections.emptyList();
        return _groups;
    }

    public void setDig(List<String> o) {
        if (o == null) {
            _dig = Collections.emptyList();
        } else {
            _dig = o;
        }
    }

    public boolean hasDigField() {
        return _dig != null;
    }

    @Override
    public List<String> getDig() {
        if (_dig == null) return Collections.emptyList();
        return _dig;
    }

    public void setDepends_on(List<String> o) {
        if (o == null) {
            _depends_on = Collections.emptyList();
        } else {
            _depends_on = o;
        }
    }

    public boolean hasDependsOnField() {
        return _depends_on != null;
    }

    public List<String> getDependsOn() {
        if (_depends_on ==null) return Collections.emptyList();
        return _depends_on;
    }

    public boolean hasExposedPortsField() {
        return _ports != null;
    }

    public void setPorts(List<String> o) {
        if (o == null) {
            _ports = Collections.emptyList();
        } else {
            _ports = o;
        }
    }

    @Override
    public List<String> getExposedPorts() {
        if (_ports ==null) return Collections.emptyList();
        return _ports;
    }

}
