package com.optum.admiral.type;

import com.optum.admiral.config.ActionMonitor;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LogMonitor {
    public final String filename;
    public final boolean deleteAtStart;

    private List<ActionMonitor> actionMonitors = new ArrayList<>();

    public LogMonitor(String filename, boolean deleteAtStart, List<File> actionMonitorFiles)
            throws AdmiralConfigurationException, InvalidBooleanException, PropertyNotFoundException, InvalidEnumException {
        this.filename = filename;
        this.deleteAtStart = deleteAtStart;
        for(File actionMonitorFile : actionMonitorFiles) {
            actionMonitors.add(new ActionMonitor(actionMonitorFile));
        }
    }

    public List<ActionMonitor> getActionMonitors() {
        return actionMonitors;
    }

}
