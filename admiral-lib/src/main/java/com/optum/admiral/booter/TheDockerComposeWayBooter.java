package com.optum.admiral.booter;

import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.ConfigVariableProcessor;
import com.optum.admiral.DockerComposeContainerNamingConvention;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.event.AdmiralEventPublisher;
import com.optum.admiral.io.AdmiralFileException;
import com.optum.admiral.type.exception.InvalidSemanticVersion;
import com.optum.admiral.type.exception.VariableSpecContraint;
import com.optum.admiral.util.FileService;
import com.optum.admiral.util.MultipleFilesFoundException;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TheDockerComposeWayBooter extends VerifiedBooter {
    // Specification
    // These are files defined by docker-compose.
    final List<File> composeFiles = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
            new File("docker-compose.yml"),
            new File("docker-compose.yaml"),
            new File("compose.yml"),
            new File("compose.yaml"))));

    // Specification
    // These are files defined by docker-compose.
    final List<File> composeOverrideFiles = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
            new File("docker-compose.override.yml"),
            new File("docker-compose.override.yaml"),
            new File("compose.override.yml"),
            new File("compose.override.yaml"))));

    private final List<String> dockerComposeFilenames;

    public TheDockerComposeWayBooter(AdmiralOptions admiralOptions,
                                     AdmiralEventPublisher admiralEventPublisher,
                                     List<String> dockerComposeFiles) {
        super(admiralOptions, admiralEventPublisher);
        this.dockerComposeFilenames = (dockerComposeFiles==null)?Collections.emptyList():dockerComposeFiles;
    }

    /**
     * This method performs a variation of "docker-compose" (or "docker compose") loading of configuration files.
     * Since "docker-compose" and "docker compose" behave slightly differently, this method, rather than choosing one way,
     * does it a third way that I think is more appropriate.  Rather than warning about ambiguity and picking one, this method
     * fails with a MultipleFilesFoundException if more than one compatible file is found.
     * It also does not search parent directories.
     */
    @Override
    public ComposeConfig createComposeConfig()
            throws
                AdmiralConfigurationException,
                AdmiralFileException,
            InvalidEnumException,
                InvalidBooleanException,
                InvalidSemanticVersion,
                IOException,
                MultipleFilesFoundException,
                PropertyNotFoundException,
                VariableSpecContraint {
        final ConfigVariableProcessor configVariableProcessor = new ConfigVariableProcessor();
        final FileService fileService = FileService.getFileServiceForCurrentPath();

        // The Docker Compose Way always loads system environment variables.
        publish(l -> l.loadingSystemConfigurationVariables());
        configVariableProcessor.initWithEnvironmentVariablesFromSystem(null);
        publish(l -> l.loadedSystemConfigurationVariables());

        String projectName = fileService.getWorkingDirectory().getName();

        // Then it loads from .env, but only if found.
        File dotEnvFile = new File(".env").getCanonicalFile();
        if (dotEnvFile.exists()) {
            publish(l -> l.loadingConfigurationVariables(dotEnvFile));
            configVariableProcessor.addEnvironmentVariablesFromFile(dotEnvFile);
        }

        // The COMPOSE_PROJECT_NAME is typically defined in .env, but technically could be pulled from system environment
        // variables.  So move the check out of the above condition so it is always checked, not just when an .env file is processed.
        String composeProjectName = configVariableProcessor.get("COMPOSE_PROJECT_NAME");
        if (composeProjectName!=null) {
            projectName = composeProjectName;
        }

        ComposeConfig composeConfig = new ComposeConfig("compose", String.join(",", dockerComposeFilenames), projectName, configVariableProcessor, new DockerComposeContainerNamingConvention(projectName));
        if (dockerComposeFilenames==null || dockerComposeFilenames.isEmpty()) {
            bootTheDockerComposeWay_DefaultFiles(fileService, composeConfig);
        } else {
            List<File> dockerComposeFiles = new ArrayList<>();
            for(String dockerComposeFilename : dockerComposeFilenames) {
                dockerComposeFiles.add(new File(dockerComposeFilename).getCanonicalFile());
            }
            bootTheDockerComposeWay_SpecifiedFiles(composeConfig, dockerComposeFiles);
        }

        composeConfig.doFinalConstructionProcessing();

        return composeConfig;
    }

    private void bootTheDockerComposeWay_SpecifiedFiles(ComposeConfig composeConfig, List<File> dockerComposeFiles)
            throws
                AdmiralConfigurationException,
                AdmiralFileException,
            InvalidEnumException,
                InvalidBooleanException,
                InvalidSemanticVersion,
                PropertyNotFoundException,
                VariableSpecContraint {
        for(File dockerComposeFile : dockerComposeFiles) {
            publish(l -> l.loadingComposeConfiguration(dockerComposeFile));
            composeConfig.load(dockerComposeFile);
        }
    }

    private void bootTheDockerComposeWay_DefaultFiles(FileService fileService, ComposeConfig composeConfig)
            throws
                AdmiralConfigurationException,
                AdmiralFileException,
            InvalidEnumException,
                InvalidBooleanException,
                InvalidSemanticVersion,
                MultipleFilesFoundException,
                PropertyNotFoundException,
                VariableSpecContraint {
        final File composeFile = fileService.findOneOf(composeFiles);
        if (composeFile == null) {
            throw new AdmiralConfigurationException("No Configuration File Found", "Are you in the right directory?\n" +
                    "  Supported Admiral filenames: docker-admiral.yml, docker-admiral.yaml, admiral.yml, admiral.yaml\n" +
                    "  Supported docker-compose filenames: docker-compose.yml, docker-compose.yaml, compose.yml, compose.yaml\n" +
                    "    (Provide your own Admiral filename in a .admiral.boot file, or specify one on the command line.)");
        } else {
            publish(l -> l.loadingComposeConfiguration(composeFile));
            composeConfig.load(composeFile);

            final File composeOverrideFile = fileService.findOneOf(composeOverrideFiles);
            if (composeOverrideFile != null) {
                publish(l -> l.loadingComposeConfiguration(composeOverrideFile));
                composeConfig.load(composeOverrideFile);
            }
        }
    }

}
