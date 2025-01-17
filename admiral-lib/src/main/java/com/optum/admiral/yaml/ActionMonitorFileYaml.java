package com.optum.admiral.yaml;

import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ActionMonitorFileYaml {
    public ActionMonitorYaml action_monitor;

    public static ActionMonitorFileYaml loadFromYamlURL(File yamlFile)
            throws
                AdmiralConfigurationException,
            InvalidEnumException,
                InvalidBooleanException,
                PropertyNotFoundException {
        final Constructor constructor = new Constructor(ActionMonitorFileYaml.class, new LoaderOptions());

        final Yaml yaml = new Yaml(constructor);
        ActionMonitorFileYaml actionMonitorFileYaml;
        try (FileInputStream fis = new FileInputStream(yamlFile)){
            actionMonitorFileYaml = yaml.load(fis);
        } catch (YAMLException e) {
            throw YamlParserHelper.rebuildYAMLException(yamlFile.toString(), e);
        } catch (IOException e) {
            throw new AdmiralConfigurationException(yamlFile.toString(), "Unable to load URL");
        }

        return actionMonitorFileYaml;
    }
}
