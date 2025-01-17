package com.optum.admiral.booter;

import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.ConfigVariableProcessor;
import com.optum.admiral.DockerComposeContainerNamingConvention;
import com.optum.admiral.config.AdmiralConfig;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.event.AdmiralEventPublisher;
import com.optum.admiral.io.AdmiralFileException;
import com.optum.admiral.io.AdmiralURLException;
import com.optum.admiral.io.StreamGobbler;
import com.optum.admiral.type.CommandVariable;
import com.optum.admiral.type.DateVariable;
import com.optum.admiral.type.VerifiedPathVariable;
import com.optum.admiral.type.UserProvidedVariable;
import com.optum.admiral.type.exception.InvalidSemanticVersion;
import com.optum.admiral.type.exception.VariableSpecContraint;
import com.optum.admiral.util.FileSearcher;
import com.optum.admiral.util.FileSearcher.SearchResult;
import com.optum.admiral.util.FileSearcher.SourceCandidate;
import com.optum.admiral.util.FileService;
import com.optum.admiral.util.MultipleFilesFoundException;
import com.optum.admiral.yaml.AdmiralBootYaml;
import com.optum.admiral.yaml.TweaksYaml;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class TheAdmiralWayBooter extends VerifiedBooter {
    // Specification
    // These are files defined by Admiral.
    static final List<SourceCandidate> dotAdmiralBootSourceCandidates = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
            new SourceCandidate("CURRENT", new ArrayList<>(Arrays.asList(new File("local-admiral.boot")))),
            new SourceCandidate("HOME", new ArrayList<>(Arrays.asList(new File("local-admiral.boot")))),
            new SourceCandidate("HOME", new ArrayList<>(Arrays.asList(new File(".admiral.boot")))),
            new SourceCandidate("CURRENT", new ArrayList<>(Arrays.asList(new File(".admiral.boot"))))
    )));

    // Specification
    // These are files defined by Admiral that follow a docker-compose inspired naming convention.
    static final List<File> admiralFiles = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
            new File("admiral.a"),
            new File("docker-admiral.yml"),
            new File("docker-admiral.yaml"),
            new File("admiral.yml"),
            new File("admiral.yaml"))));

    private final String admiralFilename;

    public TheAdmiralWayBooter(AdmiralOptions admiralOptions, AdmiralEventPublisher admiralEventPublisher, String admiralFilename) {
        super(admiralOptions, admiralEventPublisher);
        this.admiralFilename = admiralFilename;
    }

    @Override
    public ComposeConfig createComposeConfig()
            throws
            AdmiralConfigurationException,
            AdmiralFileException,
            AdmiralURLException,
            InvalidBooleanException,
            InvalidSemanticVersion,
            IOException,
            PropertyNotFoundException,
            VariableSpecContraint, InvalidEnumException {
        final File admiralFile = new File(admiralFilename).getCanonicalFile();
//        publish(l -> l.loadingAdmiralConfiguration(admiralFile));
        final AdmiralConfig admiralConfig = AdmiralConfig.loadFromYamlFile(admiralEventPublisher, admiralFile);
//        publish(l -> l.loadedAdmiralConfiguration(admiralFile));

        String projectName = admiralConfig.getProjectName();

        final ConfigVariableProcessor configVariableProcessor = new ConfigVariableProcessor();

        loadSecretVariablePatterns(configVariableProcessor, admiralConfig.getSecretVariablePatterns());

        if (!admiralConfig.getSystemEnvironmentVariables().isEmpty()) {
            publish(l -> l.loadingSystemConfigurationVariables());
            configVariableProcessor.initWithEnvironmentVariablesFromSystem(admiralConfig.getSystemEnvironmentVariables());
            publish(l -> l.loadedSystemConfigurationVariables());
        }

        loadDateVariables(configVariableProcessor, admiralConfig.getDateVariables());

        loadCommandVariables(configVariableProcessor, admiralConfig.getCommandVariables());

        loadUserProvidedVariables(configVariableProcessor, admiralConfig.getUserProvidedVariables());

        // Now that all the built-in variables have been loaded, we can use them to expand our working directory.
        final FileService fileService;

        final String projectDirectoryTemplate = admiralConfig.getProjectDirectory();
        if (projectDirectoryTemplate!=null && !projectDirectoryTemplate.isEmpty()) {
            publish(l -> l.verbose("Got a supplied Project Directory: " + projectDirectoryTemplate));
            // Evaluate to determine the actual working directory
            final String projectDirectory = configVariableProcessor.eval(projectDirectoryTemplate);
            publish(l -> l.verbose("Resolves to: " + projectDirectory));
            fileService = FileService.getFileServiceForContainingDirectoryOf(admiralFile).relativeFileService(projectDirectory);
        } else {
            fileService = FileService.getFileServiceForContainingDirectoryOf(admiralFile);
        }
        publish(l -> l.verbose("Project Directory: " + fileService.getWorkingDirectory().getPath()));

        for (AdmiralConfig.RelativeFilename rawConfigFileName : admiralConfig.getConfigFiles()) {
            loadConfigFile(configVariableProcessor, rawConfigFileName.fileService, rawConfigFileName.filename);
        }

        if (projectName==null || projectName.isEmpty()) {
            projectName = fileService.getWorkingDirectory().getName();
        }

        ComposeConfig composeConfig = new ComposeConfig("admiral", admiralFile.getCanonicalPath(), projectName, configVariableProcessor, new DockerComposeContainerNamingConvention(projectName));

        for (AdmiralConfig.RelativeFilename rawComposeFileName : admiralConfig.getComposeFiles()) {
            loadComposeFile(composeConfig, rawComposeFileName.fileService, rawComposeFileName.filename);
        }

        // Finally, load tweaks from the admiral config.
        for(Map.Entry<String, TweaksYaml> entry : admiralConfig.tweaks.entrySet()) {
            String serviceName = entry.getKey();
            TweaksYaml tweaksYaml = entry.getValue();
            AdmiralServiceConfig serviceConfig = composeConfig.getServiceConfig(serviceName);
            if (serviceConfig==null) {
                throw new AdmiralConfigurationException(admiralFile.getName(), "Service " + serviceName + " does not exist.");
            }
            serviceConfig.applyTweaksYaml(tweaksYaml, configVariableProcessor);
        }

        verifyPathVariables(configVariableProcessor, admiralConfig.getValidatedPathVariables());

        composeConfig.doFinalConstructionProcessing();

        return composeConfig;
    }

    /**
     * The master Admiral File comes from several sources - this is the algorithm to figure out which one to use.
     */
    public static File findAdmiralConfigurationFile()
            throws
                AdmiralConfigurationException,
                AdmiralFileException,
            InvalidEnumException,
                InvalidBooleanException,
                IOException,
                MultipleFilesFoundException,
                PropertyNotFoundException {

        // CASE 1: We find an .admiral.boot or local-admiral.boot
        final File case1File = lookForDotAdmiralBoot();
        if (case1File != null) {
            return case1File;
        }

        // CASE 2: We find one of the admiralFiles in the current directory
        final Path currentDirectory = FileSystems.getDefault().getPath("").toAbsolutePath();
        final FileService currentDirectoryFileService = new FileService(currentDirectory.toFile());
        final File case2File = currentDirectoryFileService.findOneOf(admiralFiles);
        // This might be null.
        return case2File;
    }

    private static File lookForDotAdmiralBoot()
            throws
                AdmiralConfigurationException,
                AdmiralFileException,
                InvalidBooleanException,
                InvalidEnumException,
                IOException,
                PropertyNotFoundException {
        final FileSearcher fileSearcher = new FileSearcher();
        final SearchResult dotAdmiralBootFile = fileSearcher.findFirst(dotAdmiralBootSourceCandidates);
        if (dotAdmiralBootFile != null) {
            AdmiralBootYaml admiralBootYaml = AdmiralBootYaml.loadFromYamlFile(dotAdmiralBootFile.file);
            return dotAdmiralBootFile.fileService.relativeFile(admiralBootYaml.admiral_file);
        }
        return null;
    }

    private void loadSecretVariablePatterns(ConfigVariableProcessor configVariableProcessor, List<String> secretVariablePatterns) {
        for(String secretVariablePattern : secretVariablePatterns) {
            configVariableProcessor.addSecretVariablePattern(secretVariablePattern);
        }
    }

    private void loadDateVariables(ConfigVariableProcessor configVariableProcessor, List<DateVariable> dateVariables)
            throws AdmiralConfigurationException {
        Date now = new Date();
        for (DateVariable dateVariable : dateVariables) {
            String nowValue = new SimpleDateFormat(dateVariable.format).format(now);
            configVariableProcessor.addKeyValueEnvironmentVariable(dateVariable.name, nowValue, "date_variables:", ConfigVariableProcessor.EntrySourceType.BUILTIN);
        }
    }

    private void loadUserProvidedVariables(ConfigVariableProcessor configVariableProcessor, List<UserProvidedVariable> userProvidedVariables)
            throws AdmiralConfigurationException {
        for(UserProvidedVariable userProvidedVariable : userProvidedVariables) {
            final String promptToUse;
            if (userProvidedVariable.prompt == null || userProvidedVariable.prompt.isEmpty()) {
                promptToUse = "Please enter " + userProvidedVariable.name;
            } else {
                promptToUse = userProvidedVariable.prompt;
            }
            final String defaultValue = configVariableProcessor.eval(userProvidedVariable._default);
            final String defaultPrompt;
            if (defaultValue!=null && (!defaultValue.isEmpty())) {
                defaultPrompt = " (" + defaultValue + "): ";
            } else {
                defaultPrompt = ": ";
            }
            final String format = promptToUse + defaultPrompt;
            final String userProvidedValue;
            if ("true".equals(userProvidedVariable.hidden)) {
                userProvidedValue = new String(System.console().readPassword(format));
            } else {
                userProvidedValue = System.console().readLine(format);
            }
            final String valueToUse;
            if (userProvidedValue.isEmpty() && (defaultValue!=null) && !defaultValue.isEmpty()) {
                valueToUse = defaultValue;
            } else {
                valueToUse = userProvidedValue;
            }
            configVariableProcessor.addKeyValueEnvironmentVariable(userProvidedVariable.name, valueToUse, "user_provided_variables:", ConfigVariableProcessor.EntrySourceType.USERPROVIDED);
        }
    }

    private void verifyPathVariables(ConfigVariableProcessor configVariableProcessor, List<VerifiedPathVariable> verifiedPathVariables)
            throws AdmiralConfigurationException {
        for(VerifiedPathVariable verifiedPathVariable : verifiedPathVariables) {
            String value = configVariableProcessor.get(verifiedPathVariable.name);
            File markerFile;
            if (value==null) {
                throw new AdmiralConfigurationException(verifiedPathVariable.name, "Missing verified path variable: " + verifiedPathVariable.description);
            }
            if (verifiedPathVariable.trailing_file_separator == VerifiedPathVariable.ENFORCEMENT.REQUIRED) {
                if (!value.endsWith(File.separator)) {
                    throw new AdmiralConfigurationException(verifiedPathVariable.name, "Missing trailing file separator: "  + verifiedPathVariable.description);
                }
            } else {
                if (value.endsWith(File.separator)) {
                    throw new AdmiralConfigurationException(verifiedPathVariable.name, "Encountered a forbidden trailing file separator: " + verifiedPathVariable.description);
                }
            }

            markerFile = new File(value + verifiedPathVariable.markerfile_name);

            if (!markerFile.exists()) {
                throw new AdmiralConfigurationException(verifiedPathVariable.name, "Required markerfile not found: " + verifiedPathVariable.description);
            }

            try {
                byte[] b = Files.readAllBytes(Paths.get(markerFile.getPath()));
                byte[] hash = MessageDigest.getInstance("MD5").digest(b);
                String actual = DatatypeConverter.printHexBinary(hash);
                if (!actual.equalsIgnoreCase(verifiedPathVariable.markerfile_md5)) {
                    throw new AdmiralConfigurationException(verifiedPathVariable.name, "Required markerfile md5 does not match: " +  verifiedPathVariable.description + " Expected md5: " + verifiedPathVariable.markerfile_md5 + "  Found md5: " + actual);
                } else {
                    publish(l -> l.verifiedMarkerFile(verifiedPathVariable.name, markerFile));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void loadCommandVariables(ConfigVariableProcessor configVariableProcessor, List<CommandVariable> commandVariables)
            throws AdmiralConfigurationException, IOException {
        for (CommandVariable commandVariable : commandVariables) {
            ProcessBuilder builder = new ProcessBuilder();
            if (commandVariable.commands != null && (!commandVariable.commands.isEmpty())) {
                builder.command(commandVariable.commands);
            } else {
                if (commandVariable.shell != null && (!commandVariable.shell.isEmpty())) {
                    List<String> shellCmds = new ArrayList<>();
                    shellCmds.addAll(Arrays.asList(commandVariable.shell.split(" ")));
                    shellCmds.add(commandVariable.command);
                    builder.command(shellCmds);
                } else {
                    builder.command(commandVariable.command.split(" "));
                }
            }
            if (commandVariable.working_dir != null && (!commandVariable.working_dir.isEmpty())) {
                builder.directory(new File(commandVariable.working_dir));
            }
            Process process = builder.start();
            StringBuilder sb = new StringBuilder();
            StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), (sb::append));
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            String commandValue = sb.toString();
            configVariableProcessor.addKeyValueEnvironmentVariable(commandVariable.name, commandValue, "CommandVariable", ConfigVariableProcessor.EntrySourceType.BUILTIN);
        }
    }

    private void loadConfigFile(ConfigVariableProcessor configVariableProcessor, FileService fileService, String rawConfigFileName)
            throws AdmiralConfigurationException, AdmiralFileException, AdmiralURLException {
        // Optional Files only allow one condition: when !configFile.exists().
        // All other errors are processed as normal.
        // (AKA: An optional file or URL that exists but can't be read is still an error.)
        // To make this clear, optional will not be permitted on URLs.
        final boolean optionalFile;
        final String configFileName;
        if (rawConfigFileName.startsWith("?")) {
            optionalFile = true;
            configFileName = rawConfigFileName.substring(1);
        } else {
            optionalFile = false;
            configFileName = rawConfigFileName;
        }

        if (isURL(configFileName)) {
            if (optionalFile) {
                throw new AdmiralConfigurationException("URL config files are not allowed to be optional: " + rawConfigFileName);
            } else {
                try {
                    final URL configFile = new URL(configFileName);
                    publish(l -> l.loadingConfigurationVariables(configFile));
                    configVariableProcessor.addEnvironmentVariablesFromURL(configFile);
                    publish(l -> l.loadedConfigurationVariables(configFile));
                } catch (IOException e) {
                    throw new AdmiralURLException("IO Error reading URL", configFileName);
                }
            }
        } else {
            try {
                final File configFile = fileService.relativeFile(configFileName).getCanonicalFile();
                if (configFile.exists()) {
                    if (configFile.isFile()) {
                        publish(l -> l.loadingConfigurationVariables(configFile));
                        configVariableProcessor.addEnvironmentVariablesFromFile(configFile);
                        publish(l -> l.loadedConfigurationVariables(configFile));
                    } else {
                        throw new AdmiralFileException("Config File is not a file", configFileName);
                    }
                } else {
                    if (!optionalFile) {
                        throw new AdmiralFileException("Required Config File", configFileName);
                    }
                }
            } catch (IOException e) {
                throw new AdmiralFileException("Error Reading Config File", configFileName);
            }
        }
    }

    private void loadComposeFile(ComposeConfig composeConfig, FileService fileService, String rawComposeFileName)
            throws
                AdmiralConfigurationException,
                AdmiralFileException,
            InvalidEnumException,
                InvalidBooleanException,
                InvalidSemanticVersion,
                PropertyNotFoundException,
                VariableSpecContraint {
        // Optional Files only allow one condition: when !composeFile.exists().
        // All other errors are processed as normal.
        // (AKA: An optional file or URL that exists but can't be read is still an error.)
        // To make this clear, optional will not be permitted on URLs.
        final boolean optionalFile;
        final String composeFileName;
        if (rawComposeFileName.startsWith("?")) {
            optionalFile = true;
            composeFileName = rawComposeFileName.substring(1);
        } else {
            optionalFile = false;
            composeFileName = rawComposeFileName;
        }

        final File composeFile = fileService.relativeFile(composeFileName);
        // (Trying to write this expression !() to appease SONAR is impossible to read.)
        if (optionalFile && !composeFile.exists()) {
            // This is unnecessary, but SONAR complains otherwise.
            return;
        } else {
            publish(l -> l.loadingComposeConfiguration(composeFile));
            composeConfig.load(composeFile);
            publish(l -> l.loadedComposeConfiguration(composeFile));
        }
    }

    private static boolean isURL(String s) {
        return (s.startsWith("http:") || s.startsWith("https:"));
    }

}
