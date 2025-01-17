package com.optum.admiral.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class manages multiple FileService instances to search across multiple
 * locations.
 *
 * Creating this class keeps FileService thin for cases where multi-directory
 * sources are not needed.
 */
public class FileSearcher {
    public static final String CURRENT = "CURRENT";
    public static final String HOME = "HOME";

    public static class SourceCandidate {
        public final String directoryKey;
        public final List<File> files;
        public SourceCandidate(String directoryKey, List<File> files) {
            this.directoryKey = directoryKey;
            this.files = files;
        }
    }

    public static class SearchResult {
        public final File file;
        public final FileService fileService;
        private SearchResult(File file, FileService fileService) {
            this.file = file;
            this.fileService = fileService;
        }
    }

    private final Map<String, Path> sourceMap = new HashMap<>();

    public FileSearcher() {
        sourceMap.put(CURRENT, FileSystems.getDefault().getPath("").toAbsolutePath());
        sourceMap.put(HOME, Paths.get(FileUtils.getUserDirectoryPath()).toAbsolutePath());
    }

    public FileSearcher(Map<String, Path> extraLocationPaths) {
        this();
        sourceMap.putAll(extraLocationPaths);
    }

    public SearchResult findFirst(List<SourceCandidate> possibleFiles)
            throws IOException {
        for (SourceCandidate sourceCandidate : possibleFiles) {
            final Path source = sourceMap.get(sourceCandidate.directoryKey);
            final FileService fileService = new FileService(source.toFile());
            final File caseFile = fileService.findFirst(sourceCandidate.files);
            if (caseFile != null) {
                return new SearchResult(caseFile.getCanonicalFile(), fileService);
            }
        }
        return null;
    }

}
