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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public class ComposeYaml {
    public String version;

    // Set by SnakeYaml parsing
    public Map<String, ServiceYaml> services;

    // Set by SnakeYaml parsing
    public Map<String, NetworkYaml> networks;

// Okay, I'm not sure if this is still used.

//    public void setServices(Map<String, ServiceYaml> services) {
//        System.out.println("IN SET SERVICES");
//        for(Map.Entry<String, ServiceYaml> entry : this.services.entrySet()) {
//            final String name = entry.getKey();
//            final ServiceYaml serviceYaml = entry.getValue();
//            serviceYaml.setName(name);
//        }
//        this.services = services;
//    }

    private File sourceFile;
    private File sourceFileDirectory;

    public File getSourceFile() {
        return sourceFile;
    }

    public File getSourceFileDirectory() {
        return sourceFileDirectory;
    }

    public void setSourceFileDirectory(File source, File directory) {
        this.sourceFile = source;
        this.sourceFileDirectory = directory;
        for(ServiceYaml serviceYaml : this.services.values()) {
            serviceYaml.setParent(this);
        }
    }

    public File getRelativePathForEnvFile(String envFileName) {
        return new File(sourceFileDirectory + File.separator + envFileName);
    }

    public static ComposeYaml loadFromYamlFile(File yamlFile) throws AdmiralConfigurationException, InvalidBooleanException, PropertyNotFoundException, InvalidEnumException {
        File can;
        try {
            can = yamlFile.getCanonicalFile();
        } catch (IOException e) {
            throw new AdmiralConfigurationException(yamlFile.toString(), String.format("IO Error reading Docker Compose file: %s", yamlFile));
        }

        final File containingDirectory = can.getParentFile();
        final Constructor constructor = new Constructor(ComposeYaml.class, new LoaderOptions());

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
        ComposeYaml composeYaml;
        try (FileInputStream fis = new FileInputStream(yamlFile)){
            composeYaml = yaml.load(fis);
        } catch (YAMLException e) {
            throw YamlParserHelper.rebuildYAMLException(yamlFile.getName(), e);
        } catch (FileNotFoundException e) {
            throw new AdmiralConfigurationException(yamlFile.toString(), "File not found: " + yamlFile);
        } catch (IOException e) {
            throw new AdmiralConfigurationException("Error reading file", yamlFile.getName());
        }

        composeYaml.setSourceFileDirectory(can, containingDirectory);
        return composeYaml;
    }
}
