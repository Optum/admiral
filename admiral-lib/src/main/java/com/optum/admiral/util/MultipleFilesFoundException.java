package com.optum.admiral.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultipleFilesFoundException extends Exception {
    final public List<File> files;

    public MultipleFilesFoundException(List<File> files) {
        final List<File> copy = new ArrayList<>(files.size());
        copy.addAll(files);
        this.files = Collections.unmodifiableList(copy);
    }
}
