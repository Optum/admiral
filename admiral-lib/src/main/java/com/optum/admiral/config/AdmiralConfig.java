package com.optum.admiral.config;

import com.optum.admiral.event.AdmiralEventListener;
import com.optum.admiral.event.AdmiralEventPublisher;
import com.optum.admiral.io.AdmiralFileException;
import com.optum.admiral.type.CommandVariable;
import com.optum.admiral.type.DateVariable;
import com.optum.admiral.type.VerifiedPathVariable;
import com.optum.admiral.type.UserProvidedVariable;
import com.optum.admiral.util.FileService;
import com.optum.admiral.yaml.TweaksYaml;
import com.optum.admiral.yaml.AdmiralYaml;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.optum.admiral.yaml.YamlParserHelper.verifyNoneEmpty;

public class AdmiralConfig {
    static public class RelativeFilename {
        final public FileService fileService;
        final public String filename;
        private RelativeFilename(final FileService fileService, final String filename) {
            this.fileService = fileService;
            this.filename = filename;
        }
    }
    private final String projectName;
    private final String projectDirectory;
    private final List<String> systemEnvironmentVariables = new ArrayList<>();
    private final List<RelativeFilename> configFiles = new ArrayList<>();
    private final List<RelativeFilename> composeFiles = new ArrayList<>();
    private final List<DateVariable> dateVariables = new ArrayList<>();
    private final List<CommandVariable> commandVariables = new ArrayList<>();
    private final List<UserProvidedVariable> userProvidedVariables = new ArrayList<>();
    private final List<VerifiedPathVariable> verifiedPathVariables = new ArrayList<>();
    private final List<String> secretVariablePatterns = new ArrayList<>();

    // TODO Replace with a real impl after proof of testing...  (In other words a Tweaks class, not the TweaksYaml class).
    public Map<String, TweaksYaml> tweaks = new HashMap<>();

    private AdmiralConfig(String projectName, String projectDirectory) {
        this.projectName = projectName;
        this.projectDirectory = projectDirectory;
    }

    private void includeAdmiralConfig(FileService fileService, AdmiralEventPublisher admiralEventPublisher, File yamlFile, AdmiralYaml admiralYaml, boolean rootConfig) throws AdmiralConfigurationException {
        verifyNoneEmpty(admiralYaml._system_environment_variables, yamlFile, "Blank system_environment_variables: lines are not allowed.");
        this.systemEnvironmentVariables.addAll(admiralYaml._system_environment_variables);

        verifyNoneEmpty(admiralYaml._config_files,  yamlFile, "Blank config_files: lines are not allowed.");
        for(String configFile : admiralYaml._config_files) {
            this.configFiles.add(new RelativeFilename(fileService, configFile));
        }

        verifyNoneEmpty(admiralYaml._compose_files,  yamlFile, "Blank compose_files: lines are not allowed.");
        for(String composeFile : admiralYaml._compose_files) {
            this.composeFiles.add(new RelativeFilename(fileService, composeFile));
        }

        this.dateVariables.addAll(admiralYaml.date_variables);
        this.commandVariables.addAll(admiralYaml.command_variables);
        this.userProvidedVariables.addAll(admiralYaml.user_provided_variables);
        this.verifiedPathVariables.addAll(admiralYaml.verified_path_variables);
        this.secretVariablePatterns.addAll(admiralYaml._secret_variable_patterns);
        if (rootConfig) {
            if (admiralYaml.tweaks != null) {
                tweaks = admiralYaml.tweaks;
            }
        } else {
            if (admiralYaml.tweaks != null) {
                publish(admiralEventPublisher, l -> l.warning(String.format("Tweaks from included Admiral file %s have been ignored.  (Tweaks are only honored from the root Admiral file.)", yamlFile.getName())));
            }
        }
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectDirectory() {
        return projectDirectory;
    }

    public List<String> getSystemEnvironmentVariables() {
        return systemEnvironmentVariables;
    }

    public List<RelativeFilename> getConfigFiles() {
        return configFiles;
    }

    public List<RelativeFilename> getComposeFiles() {
        return composeFiles;
    }

    public List<DateVariable> getDateVariables() {
        return dateVariables;
    }

    public List<CommandVariable> getCommandVariables() {
        return commandVariables;
    }

    public List<UserProvidedVariable> getUserProvidedVariables() {
        return userProvidedVariables;
    }

    public  List<VerifiedPathVariable> getValidatedPathVariables() {
        return verifiedPathVariables;
    }

    public List<String> getSecretVariablePatterns() {
        return secretVariablePatterns;
    }

    public static AdmiralConfig loadFromYamlFile(AdmiralEventPublisher admiralEventPublisher, File yamlFile)
            throws
            AdmiralConfigurationException,
            AdmiralFileException,
            InvalidEnumException,
            InvalidBooleanException,
            IOException,
            PropertyNotFoundException {
        AdmiralConfig admiralConfig = loadFromYamlFile(admiralEventPublisher, yamlFile, null, true);
        return admiralConfig;
    }

    /**
     * This method is kind of a big deal.  It implements the inheritance/scope policy of included Admiral files.
     *
     * IncludED Admiral file contents is loaded into AdmiralConfig BEFORE contents of the includING Admiral file.
     * BUT since project_name, project_directory and fileService are not included/inherited, we actually create the
     * AdmiralConfig first.
     */
    private static AdmiralConfig loadFromYamlFile(AdmiralEventPublisher admiralEventPublisher, File yamlFile, AdmiralConfig admiralConfig, boolean rootConfig)
            throws
            AdmiralConfigurationException,
            AdmiralFileException,
            InvalidEnumException,
            InvalidBooleanException,
            IOException,
            PropertyNotFoundException {
        publish(admiralEventPublisher, l -> l.loadingAdmiralConfiguration(yamlFile));

        // This is the Yaml raw data that we want to parse and load into an AdmiralConfig.
        AdmiralYaml admiralYaml = AdmiralYaml.loadFromYamlFile(yamlFile);

        // All path references in an AdmiralYaml file are relative to that AdmiralYaml's containing directory ...
        FileService fileService = FileService.getFileServiceForContainingDirectoryOf(yamlFile);

        // ... UNLESS the AdmiralYaml specifies a project_directory.  Then the path references are relative to
        // the containing directory with the project_directory "relative offset" applied.
        if ( (admiralYaml.project_directory != null) && (!admiralYaml.project_directory.isEmpty()) ) {
            fileService = fileService.relativeFileService(admiralYaml.project_directory);
        }

        // If admiralConfig is null, that means we were not passed one in, which means we are processing the ROOT.
        if (admiralConfig == null) {
            admiralConfig = new AdmiralConfig(admiralYaml.project_name, admiralYaml.project_directory);
        }

        // At this point we know we have an AdmiralConfig.

        // Before loading the references in our AdmiralYaml, we include other AdmiralYamls.
        if ( (admiralYaml._admiral_includes != null) && (!admiralYaml._admiral_includes.isEmpty()) ) {
            for(String includeFilename : admiralYaml._admiral_includes) {
                // Here let me be extra clear about fileService.relativeFile usage:
                // The full path of the admiral_includes are resolved based on the FileService of the AdmiralYaml that contains the admiral_includes.
                // But the contents INSIDE those admiral_includes are relative to the project_directory of those AdmiralYaml files.
                // This fileService only loads AdmiralYaml files, not the contents of that AdmiralYaml.
                File includeYamlFile = fileService.relativeFile(includeFilename);
                admiralConfig = loadFromYamlFile(admiralEventPublisher, includeYamlFile, admiralConfig, false);
            }
        }
        admiralConfig.includeAdmiralConfig(fileService, admiralEventPublisher, yamlFile, admiralYaml, rootConfig);
        publish(admiralEventPublisher, l -> l.loadedAdmiralConfiguration(yamlFile));
        return admiralConfig;
    }

    private static void publish(AdmiralEventPublisher admiralEventPublisher, Consumer<AdmiralEventListener> event) {
        admiralEventPublisher.publish(event);
    }

}
