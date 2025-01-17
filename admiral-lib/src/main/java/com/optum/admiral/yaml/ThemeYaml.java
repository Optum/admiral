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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public class ThemeYaml {

    public Map<String, ThemeEntryYaml> theme;

    public ThemeEntryYaml get(String name) {
        return theme.get(name);
    }

    public static ThemeYaml loadFromYamlFile(File yamlFile) throws AdmiralConfigurationException, InvalidBooleanException, PropertyNotFoundException, InvalidEnumException {
        final Constructor constructor = new Constructor(ThemeYaml.class, new LoaderOptions());

        final Yaml yaml = new Yaml(constructor);
        ThemeYaml themeYaml;
        try (FileInputStream fis = new FileInputStream(yamlFile)){
            themeYaml = yaml.load(fis);
        } catch (YAMLException e) {
            throw YamlParserHelper.rebuildYAMLException(yamlFile.getName(), e);
        } catch (FileNotFoundException e) {
            throw new AdmiralConfigurationException("Theme file " + yamlFile, "File not found");
        } catch (IOException e) {
            throw new AdmiralConfigurationException("Error reading file", yamlFile.getName());
        }

        return themeYaml;
    }

}
