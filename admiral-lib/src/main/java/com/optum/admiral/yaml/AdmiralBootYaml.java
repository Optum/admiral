package com.optum.admiral.yaml;

import com.optum.admiral.io.AdmiralFileException;
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

public class AdmiralBootYaml {
    public String admiral_file;

    public static AdmiralBootYaml loadFromYamlFile(File yamlFile)
            throws
                AdmiralConfigurationException,
                AdmiralFileException,
            InvalidEnumException,
                InvalidBooleanException,
                PropertyNotFoundException {
        final Constructor constructor = new Constructor(AdmiralBootYaml.class, new LoaderOptions());

        final Yaml yaml = new Yaml(constructor);
        final AdmiralBootYaml admiralBootYaml;

        try (FileInputStream fis = new FileInputStream(yamlFile)){
            admiralBootYaml = yaml.load(fis);
            // If yamlFile is actually EMPTY, load returns null instead of an "empty" object.
            // So we have to detect that case and return the empty object.
            if (admiralBootYaml == null) {
                return new AdmiralBootYaml();
            }
        } catch (YAMLException e) {
            throw YamlParserHelper.rebuildYAMLException(yamlFile.getName(), e);
        } catch (FileNotFoundException e) {
            throw new AdmiralFileException("File not found", yamlFile.getName());
        } catch (IOException e) {
            throw new AdmiralFileException("Error reading file", yamlFile.getName());
        }

        return admiralBootYaml;
    }
}
