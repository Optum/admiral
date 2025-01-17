package com.optum.admiral.util;

import com.optum.admiral.io.AdmiralFileException;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileService {
    private final File workingDirectory;

    public FileService(File directory) throws IOException {
        // Gather
        final File workingDirectory = directory.getCanonicalFile();

        // Guard
        if (!workingDirectory.exists()) {
            throw new IOException("Working directory does not exist: " + workingDirectory.getName());
        }

        if (!workingDirectory.isDirectory()) {
            throw new IOException("Working directory does not exist: " + workingDirectory.getName());
        }

        // Go
        this.workingDirectory = workingDirectory;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public File findOneOf(List<File> possibilities) throws MultipleFilesFoundException {
        // Optimize for the main path.
        // If we get more than 1, we have to grow the array, but we're throwing an exception so who cares.
        List<File> found = new ArrayList<>(1);
        for(File rawCandidate : possibilities) {
            File candidate = relativeFile(rawCandidate.getPath());
            if (isLegit(candidate)) {
                found.add(candidate);
            }
        }

        if (found.size()==0) {
            return null;
        } else if (found.size()==1) {
            return found.get(0);
        } else {
            throw new MultipleFilesFoundException(found);
        }
    }

    public List<File> findAllOf(List<File> possibilities) {
        List<File> found = new ArrayList<>(possibilities.size());
        for(File rawCandidate : possibilities) {
            File candidate = relativeFile(rawCandidate.getPath());
            if (isLegit(candidate)) {
                found.add(candidate);
            }
        }

        return found;
    }

    public File findFirst(List<File> possibilities) {
        for(File rawCandidate : possibilities) {
            File candidate = relativeFile(rawCandidate.getPath());
            if (isLegit(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean isLegit(File file) {
        return file.exists() && file.isFile();
    }

    public File relativeFile(String fileName) {
        String tildeSupportedFileName = fileName.replaceFirst("^~/", System.getProperty("user.home") + "/");
        return Paths.get(workingDirectory.toString()).resolve(tildeSupportedFileName).toFile();
    }

    public FileService relativeFileService(String pathName) throws IOException {
        String tildeSupportedPathName = pathName.replaceFirst("^~/", System.getProperty("user.home") + "/");
        return new FileService(Paths.get(workingDirectory.toString()).resolve(tildeSupportedPathName).toFile());
    }

    // Mechanism
    public static FileService getFileServiceForContainingDirectoryOf(File file)
            throws AdmiralFileException {
        Path path = file.toPath().toAbsolutePath();
        try {
            File directory = path.getParent().toFile();
            return new FileService(directory);
        } catch (IOException e) {
            throw new AdmiralFileException("IO Error setting working directory: " + e.getMessage(), path.toString());
        }
    }

    // Mechanism
    public static FileService getFileServiceForCurrentPath()
            throws AdmiralFileException {
        Path path = FileSystems.getDefault().getPath("").toAbsolutePath();
        try {
            return new FileService(path.toFile());
        } catch (IOException e) {
            throw new AdmiralFileException("IO Error setting current directory: " + e.getMessage(), path.toString());
        }
    }


}
