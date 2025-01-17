package com.optum.admiral.yaml;

import com.optum.admiral.io.AdmiralFileException;
import com.optum.admiral.type.CommandVariable;
import com.optum.admiral.type.DateVariable;
import com.optum.admiral.type.VerifiedPathVariable;
import com.optum.admiral.type.UserProvidedVariable;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This inner class is used by SnakeYaml Parser and therefore must follow necessary conventions needed to make
 * that work.  No code should use AdmiralYaml directly; instead use AdmiralConfig. But AdmiralYaml must be
 * public for SnakeYaml Parser to construct one.
 */
public class AdmiralYaml {
    public String project_name;
    public String project_directory;
    public List<String> _admiral_includes = Collections.emptyList();
    public List<String> _system_environment_variables = Collections.emptyList();
    public List<DateVariable> date_variables = Collections.emptyList();
    public List<CommandVariable> command_variables = Collections.emptyList();
    public List<UserProvidedVariable> user_provided_variables = Collections.emptyList();
    public List<VerifiedPathVariable> verified_path_variables = Collections.emptyList();
    public List<String> _secret_variable_patterns = Collections.emptyList();
    public List<String> _config_files = Collections.emptyList();
    public List<String> _compose_files = Collections.emptyList();

    // Set by SnakeYaml parsing
    public Map<String, TweaksYaml> tweaks;

    // Invoked by SnakeYaml Parser
    public void setAdmiral_includes(List<String> admiral_includes) {
        if (admiral_includes != null)
            _admiral_includes = admiral_includes;
    }

    // Invoked by SnakeYaml Parser
    public void setSystem_environment_variables(List<String> system_environment_variables) {
        if (system_environment_variables != null)
            _system_environment_variables = system_environment_variables;
    }

    // Invoked by SnakeYaml Parser
    public void setConfig_files(Object o) {
        if (o instanceof String) {
            _config_files = new ArrayList<>(1);
            _config_files.add((String) o);
        } else {
            _config_files = (List) o;
        }
    }

    // Invoked by SnakeYaml Parser
    public void setCompose_files(Object o) {
        if (o instanceof String) {
            _compose_files = new ArrayList<>(1);
            _compose_files.add((String) o);
        } else {
            _compose_files = (List) o;
        }
    }

    // Invoked by SnakeYaml Parser
    public void setSecret_variable_patterns(Object o) {
        if (o instanceof String) {
            _secret_variable_patterns = new ArrayList<>(1);
            _secret_variable_patterns.add((String) o);
        } else {
            _secret_variable_patterns = (List) o;
        }
    }

    public static AdmiralYaml loadFromYamlFile(File yamlFile)
            throws
                AdmiralConfigurationException,
                AdmiralFileException,
            InvalidEnumException,
                InvalidBooleanException,
                PropertyNotFoundException {
        final Constructor constructor = new Constructor(AdmiralYaml.class, new LoaderOptions());

        final Yaml yaml = new Yaml(constructor);
        final AdmiralYaml admiralYaml;

        try (FileInputStream fis = new FileInputStream(yamlFile)){
            admiralYaml = yaml.load(fis);
            // If yamlFile is actually EMPTY, load returns null instead of an "empty" object.
            // So we have to detect that case and return the empty object.
            if (admiralYaml == null) {
                return new AdmiralYaml();
            }
        } catch (YAMLException e) {
            throw YamlParserHelper.rebuildYAMLException(yamlFile.getName(), e);
        } catch (FileNotFoundException e) {
            throw new AdmiralFileException("File not found", yamlFile.getName());
        } catch (IOException e) {
            throw new AdmiralFileException("Error reading file", yamlFile.getName());
        }

        return admiralYaml;
    }
}
