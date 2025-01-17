package com.optum.admiral.config;

import com.optum.admiral.type.SemanticVersion;
import com.optum.admiral.type.VariableSpec;
import com.optum.admiral.type.exception.InvalidSemanticVersion;
import com.optum.admiral.type.exception.VariableSpecContraint;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.VariableYaml;
import com.optum.admiral.yaml.YamlParserHelper;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class EnvironmentSpecification {
    private final Map<String, VariableSpec> variables = new TreeMap<>();

    public boolean hasCodeVersionConstraints() {
        for(VariableSpec variableSpec : variables.values()) {
            if (variableSpec.hasCodeVersionConstraints())
                return true;
        }
        return false;
    }

    public Collection<VariableSpec> getVariables() {
        return variables.values();
    }

    public String validateUse(SemanticVersion currentVersion, String variable) {
        VariableSpec variableSpec = variables.get(variable);

        if (variableSpec==null)
            return "Variable \"" + variable + "\" is not allowed per Environment Specification.";

        if (!variableSpec.isAllowedFor(currentVersion))
            return "Variable \"" + variable + "\" is not allowed for code version \""+ currentVersion +"\" per Environment Specification.";

        return null;
    }

    public void addVariables(YamlParserHelper yph, Collection<VariableYaml> variableYamls)
            throws InvalidSemanticVersion, VariableSpecContraint, AdmiralConfigurationException {
        for(VariableYaml variableYaml : variableYamls) {
            final String key = yph.getS(variableYaml.name);
            final String firstCodeVersionString = yph.getS(variableYaml.first_code_version);
            final String lastCodeVersionString = yph.getS(variableYaml.last_code_version);

            VariableSpec variableSpec = variables.get(key);
            if (variableSpec==null) {
                // This is a create.
                SemanticVersion firstCodeVersion = SemanticVersion.parse(firstCodeVersionString);
                SemanticVersion lastCodeVersion = SemanticVersion.parse(lastCodeVersionString);
                variableSpec = new VariableSpec(key, firstCodeVersion, lastCodeVersion);
                variables.put(key, variableSpec);
            } else {
                // This is an update.
                if ((variableSpec.firstCodeVersion!=null) && (firstCodeVersionString!=null)) {
                    throw new VariableSpecContraint("Variable " + key + " already has a first_code_version constraint.  That field is unmodifiable.");
                }
                SemanticVersion firstCodeVersion = SemanticVersion.parse(firstCodeVersionString);
                SemanticVersion lastCodeVersion = SemanticVersion.parse(lastCodeVersionString);
                variableSpec.firstCodeVersion = firstCodeVersion;
                variableSpec.lastCodeVersion = lastCodeVersion;
            }
        }
    }
}
