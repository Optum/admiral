package com.optum.admiral.preferences;

import com.optum.admiral.event.AdmiralEventPublisher;
import com.optum.admiral.util.FileService;
import com.optum.admiral.yaml.YamlParserHelper;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Here is the shared logic for loading various Admiral preference files.
 */
public abstract class PreferenceYamlLoader<T extends PreferenceYaml> {
    private final Constructor constructor;
    private final AdmiralEventPublisher admiralEventPublisher;

    public PreferenceYamlLoader(Constructor constructor, AdmiralEventPublisher admiralEventPublisher) {
        this.constructor = constructor;
        this.admiralEventPublisher = admiralEventPublisher;
    }

    protected abstract T createDefault();

    protected void loadReferencedFilesFrom(T outputYaml, File sourceYamlFile)
            throws
                AdmiralConfigurationException,
            InvalidEnumException,
                InvalidBooleanException,
                PropertyNotFoundException {
        // Default is nothing to load.
    }

    protected List<PreferenceSourceCandidate> getPossiblePreferenceFiles() {
        return Collections.EMPTY_LIST;
    }

    public T loadOrDefault(String prefererencesFilename) throws AdmiralConfigurationException, InvalidBooleanException, PropertyNotFoundException, InvalidEnumException {
        final File yamlFile = findAPreferencesFile(prefererencesFilename);
        final Yaml yaml = new Yaml(constructor);

        // The new .admiral.preferences search now delivers a null yamlFile for an unfound file.
        if (yamlFile == null) {
            return createDefault();
        }

        try (FileInputStream fis = new FileInputStream(yamlFile)){
            final T outputYaml = yaml.load(fis);
            // If yamlFile exists but is actually EMPTY, SnakeParser load returns null instead of an "empty" object.
            // So we have to detect that case and return the empty object.
            if (outputYaml == null) {
                return createDefault();
            }

            outputYaml.setSource(yamlFile);
            loadReferencedFilesFrom(outputYaml, yamlFile);
            // If we get here it is from actually loading the file.

            // Publishing the loadedAdmiralPreferences event just causes too many problems...
            //   you can't turn it off if you want to...
            //   admiral can't turn it off if it needs to...
            //   so removing now until I have a chance to redesign the flow without creating a mess.
            // admiralEventPublisher.publish(l -> l.loadedAdmiralPreferences(yamlFile));
            return outputYaml;
        } catch (YAMLException e) {
            throw YamlParserHelper.rebuildYAMLException(yamlFile.getName(), e);
        } catch (FileNotFoundException e) {
            // Not a problem - use defaults.
            return createDefault();
        } catch (IOException e) {
            throw new AdmiralConfigurationException("Error reading file", yamlFile.getName());
        }

    }

    public enum Source {
        HOME,
        CURRENT
    }

    public static class PreferenceSourceCandidate {
        public final Source directory;
        public final List<File> files;
        public PreferenceSourceCandidate(Source directory, List<File> files) {
            this.directory = directory;
            this.files = files;
        }
    }

    private File findAPreferencesFile(String preferencesFilename)
            throws AdmiralConfigurationException {
        try {
            List<File> filesSearched = new ArrayList<>();
            final Path currentDirectory = FileSystems.getDefault().getPath("").toAbsolutePath();
            final Path homeDirectory = Paths.get(FileUtils.getUserDirectoryPath()).toAbsolutePath();
            if (preferencesFilename!=null) {
                final List<File> files = new ArrayList<>(Collections.singletonList(new File(preferencesFilename)));
                final File specificFile = lookForAPreferencesFileHere(currentDirectory, files);
                if (specificFile != null) {
                    return specificFile.getCanonicalFile();
                } else {
                    filesSearched.addAll(buildFilesSearched(currentDirectory, files));
                }
            } else {
                final List<PreferenceSourceCandidate> possiblePreferenceFiles = getPossiblePreferenceFiles();
                for (PreferenceSourceCandidate preferenceSourceCandidate : possiblePreferenceFiles) {
                    final Path source = (preferenceSourceCandidate.directory == Source.HOME) ? homeDirectory : currentDirectory;
                    final File caseFile = lookForAPreferencesFileHere(source, preferenceSourceCandidate.files);
                    if (caseFile != null) {
                        return caseFile.getCanonicalFile();
                    } else {
                        filesSearched.addAll(buildFilesSearched(source, preferenceSourceCandidate.files));
                    }
                }
            }
            admiralEventPublisher.publish(l -> l.noAdmiralPreferencesFileFound(filesSearched));
        } catch (IOException e) {
            throw new AdmiralConfigurationException("Preferences File", "IO Error: " + e.getMessage());
        }
        return null;
    }

    private File lookForAPreferencesFileHere(Path here, List<File> possiblePreferenceFiles) throws IOException {
        final FileService fileService = new FileService(here.toFile());
        return fileService.findFirst(possiblePreferenceFiles);
    }

    private List<File> buildFilesSearched(Path here, List<File> possiblePreferenceFiles) throws IOException {
        List<File> searchedFiles = new ArrayList<>();
        final FileService fileService = new FileService(here.toFile());
        for(File file : possiblePreferenceFiles) {
            searchedFiles.add(fileService.relativeFile(file.getPath()));
        }
        return searchedFiles;
    }


}
