package com.optum.admiral.config;

import com.optum.admiral.type.ActionMonitorMarker;
import com.optum.admiral.yaml.ActionMonitorFileYaml;
import com.optum.admiral.yaml.ActionMonitorMarkerYaml;
import com.optum.admiral.yaml.ActionMonitorYaml;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ActionMonitor {
    public final String name;
    public final List<ActionMonitorMarker> actionMonitorMarkers = new ArrayList<>();

    public long firstMsMarker = Long.MAX_VALUE;
    public long lastMsMarker = Long.MIN_VALUE;
    public ActionMonitorMarker lastActionMonitorMarker;

    private static final SimpleDateFormat reltimeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public ActionMonitor(File actionProgressFile) throws AdmiralConfigurationException, InvalidBooleanException, PropertyNotFoundException, InvalidEnumException {
        ActionMonitorFileYaml actionMonitorFileYaml = ActionMonitorFileYaml.loadFromYamlURL(actionProgressFile);
        ActionMonitorYaml actionMonitorYaml = actionMonitorFileYaml.action_monitor;
        this.name = actionMonitorYaml.name;
        for (ActionMonitorMarkerYaml actionMonitorMarkerYaml : actionMonitorYaml.markers) {
            final String reltime = actionMonitorMarkerYaml.reltime;
            try {
                final Date reltimeDate = reltimeFormat.parse(reltime);
                final long reltimeMS = reltimeDate.getTime();
                final String regex = actionMonitorMarkerYaml.regex;
                final String desc = actionMonitorMarkerYaml.desc;
                ActionMonitorMarker actionMonitorMarker = new ActionMonitorMarker(reltimeMS, regex, desc);
                add(actionMonitorMarker);
            } catch (ParseException e) {
                throw new AdmiralConfigurationException(actionProgressFile.toString(), "Bad reltime format: " + reltime);
            }
        }
    }

    private void add(ActionMonitorMarker actionMonitorMarker) {
        actionMonitorMarkers.add(actionMonitorMarker);
        firstMsMarker = Math.min(firstMsMarker, actionMonitorMarker.msFromStart);
        if (actionMonitorMarker.msFromStart >= lastMsMarker) {
            lastMsMarker = actionMonitorMarker.msFromStart;
            lastActionMonitorMarker = actionMonitorMarker;
        }
    }

    public ActionProgress checkForProgress(String logLine) {
        for(ActionMonitorMarker actionMonitorMarker : actionMonitorMarkers) {
            if (logLine.contains(actionMonitorMarker.regex)) {
                return progressAt(actionMonitorMarker);
            }
        }
        return null;
    }

    public long firstMonitorTime = Long.MAX_VALUE;

    private ActionProgress progressAt(ActionMonitorMarker actionMonitorMarker) {
        long nowTime = new Date().getTime();
        // If we don't have a firstMonitorTime, or if we just detected the first ActionMonitorMarker, (re)set.
        if (firstMonitorTime==Long.MAX_VALUE || actionMonitorMarker.msFromStart==firstMsMarker) {
            firstMonitorTime = nowTime;
        }
        ActionProgress actionProgress = new ActionProgress();
        actionProgress.actionName = name;
        actionProgress.matched = actionMonitorMarker.regex;
        actionProgress.desc = actionMonitorMarker.desc;
        actionProgress.msRemaining = lastMsMarker - actionMonitorMarker.msFromStart;
        actionProgress.percentageTimeComplete = 100.0f -(actionProgress.msRemaining * 100 / (lastMsMarker - firstMsMarker));
        actionProgress.isLast = (actionMonitorMarker==lastActionMonitorMarker);
        actionProgress.estCompletionTime = new Date(nowTime + actionProgress.msRemaining);
        // The adjusted Completion Time factors our rate of completion vs the standard rate.  If we are running
        // faster than standard, our adjustedCompletionTime will be sooner; if we are running slower than standard, our
        // adjustedCompletionTime will be later.
        long ourElapsedMs = nowTime - firstMonitorTime;
        long standardElapsedMs = actionMonitorMarker.msFromStart - firstMsMarker;
        float ourRate = (standardElapsedMs==0) ? 1.0f : ((float)ourElapsedMs) / ((float)standardElapsedMs);
        actionProgress.rate = ourRate;
        actionProgress.adjustedCompletionTime = new Date(nowTime + Math.round(actionProgress.msRemaining * ourRate));
        return actionProgress;
    }
}
