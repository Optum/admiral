package com.optum.admiral.yaml;

import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class EnvironmentSpecificationYaml {
    private List<VariableYaml> variables = Collections.emptyList();

    public List<VariableYaml> getVariables() {
        return variables;
    }

    public void setVariables(List<VariableYaml> variables) {
        if (variables==null) {
            // SnakeYaml sets a null list when the section is empty.  I don't think so.
            return;
        } else {
            this.variables = variables;
        }
    }

    public static EnvironmentSpecificationYaml loadFromYamlURL(URL yamlURL)
            throws
                AdmiralConfigurationException,
            InvalidEnumException,
                InvalidBooleanException,
                PropertyNotFoundException {
        final Constructor constructor = new Constructor(EnvironmentSpecificationYaml.class, new LoaderOptions());

        // This magic converts "x-admiral_post_create_execute" into "x_admiral_post_create_execute" since "-"
        // is not a valid character in a field name, so SnakeYaml can match the newName to the field name.
        constructor.setPropertyUtils(new PropertyUtils() {
            @Override
            public Property getProperty(Class<?> type, String name) {
                final String newName = name.replaceAll("-", "_");
                return super.getProperty(type, newName);
            }
        });

        final Yaml yaml = new Yaml(constructor);
        EnvironmentSpecificationYaml environmentSpecificationYaml;
        try {
            environmentSpecificationYaml = yaml.load(yamlURL.openStream());
        } catch (YAMLException e) {
            throw YamlParserHelper.rebuildYAMLException(yamlURL.toString(), e);
        } catch (IOException e) {
            throw new AdmiralConfigurationException(yamlURL.toString(), "Unable to load URL");
        }

        return environmentSpecificationYaml;
    }

}
