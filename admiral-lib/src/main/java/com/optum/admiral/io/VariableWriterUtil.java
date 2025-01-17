package com.optum.admiral.io;

import com.optum.admiral.Admiral;
import com.optum.admiral.ContainerParameterProcessor;
import com.optum.admiral.config.AdmiralContainerConfig;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.DockerModelController;
import com.optum.admiral.preferences.OutputPreferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class VariableWriterUtil {
    private final OutputPreferences preferences;
    private final OutputStyler styler;
    private final OutputWriter writer;

    public VariableWriterUtil(OutputPreferences preferences, OutputStyler styler, OutputWriter writer) {
        this.preferences = preferences;
        this.styler = styler;
        this.writer = writer;
    }

    /**
     * As an ACT, it will invoke other ACTs (which will connect as necessary).
     */
    public void writeInspectACT(Admiral admiral, ContainerParameterProcessor containerParameterProcessor, String serviceName, AdmiralServiceConfig admiralServiceConfig) {
        writer.outln(styler.serviceHeading.format(serviceName + ":"));

        Map<String, ContainerParameterProcessor.Entry> results = new TreeMap<>();
        Map<String, ContainerParameterProcessor.Entry> pending = new TreeMap<>();
        pending.putAll(admiralServiceConfig.getEnvironmentVariables());

        final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
        for (int i = 1; i <= replicas; i++) {
            final String containerName = admiral.calculateContainerName(serviceName, i);
            try {
                Map<String, String> containerVars = admiral.getContainerEnvironmentVariablesACT(containerName);

                writer.outln(" " + styler.container.format(containerName) + ":");

                for(Map.Entry<String, String> entry : containerVars.entrySet()) {
                    final String containerKey = entry.getKey();
                    final String containerValue = entry.getValue();
                    final ContainerParameterProcessor.Entry configurationEntry = pending.remove(containerKey);
                    final ContainerParameterProcessor.Entry resultEntry;
                    if (configurationEntry==null) {
                        // This container variable is not defined in the Service.  It's source is CONTAINER.
                        resultEntry = containerParameterProcessor.new Entry(containerKey, containerValue, styler.container.format(containerName));
                    } else {
                        if (containerValue.equals(configurationEntry.getValue())) {
                            // This container variable is defined in the Service and it matches.
                            resultEntry = configurationEntry;
                        } else {
                            // This container variable is define in the Service, but the value in the container does not match the value in the service.
                            resultEntry = containerParameterProcessor.new Entry(containerKey, containerValue, containerName, configurationEntry.value, configurationEntry.getSource());
                        }
                    }
                    results.put(containerKey, resultEntry);
                }

                for(ContainerParameterProcessor.Entry entry : results.values()) {
                    final String key = entry.getKey();
                    final String value = entry.getDisplayValue();
                    if (entry.hasExtra()) {
                        final String extraValue = entry.getDisplayExtraValue();
                        final String extraSource = entry.getExtraSource();
                        // Handle the case where the container doesn't match the config
                        writer.outln("  " + styler.heading.format(key) + "=" + styler.formatValue(value) +
                                " !!! " + styler.warning.format("WARNING - Value in container ") + styler.container.format(containerName) +
                                styler.warning.format(" does not match value in configuration: ") + styler.formatValue(extraValue) + " (" + styler.file.format(extraSource) + ") !!!" );
                    } else {
                        writeVariable(entry, INDENT);
                    }
                }

                if (!pending.isEmpty()) {
                    writer.outln("  " + styler.section.format("Extra Variables Defined by Service that are not in Container:") + " " + styler.container.format(containerName));
                    for (Map.Entry<String, ContainerParameterProcessor.Entry> mentry : pending.entrySet()) {
                        final ContainerParameterProcessor.Entry entry = mentry.getValue();
                        writeVariable(entry, INDENT);
                    }
                }
            } catch (AdmiralDockerException | InterruptedException e) {
                writer.outln(styler.warning.format("Container ") + styler.container.format(containerName) + styler.warning.format(" not processed: ") + e.getMessage());
            }
        }
    }

    public void writeDigACT(Admiral admiral, String serviceName, AdmiralServiceConfig admiralServiceConfig, boolean showAll) {
        writer.outln(styler.serviceHeading.format(serviceName + ":"));

        Map<String, Map<String, String>> containersVars = new HashMap<>();
        final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();

        for (int i = 1; i <= replicas; i++) {
            final String containerName = admiral.calculateContainerName(serviceName, i);

            try {
                Map<String, String> containerVars = admiral.getContainerEnvironmentVariablesACT(containerName);
                containersVars.put(containerName, containerVars);
            } catch (AdmiralDockerException | InterruptedException e) {
                // Eat it.  This is optional data.
            }
        }

        for(ContainerParameterProcessor.Entry entry : admiralServiceConfig.getEnvironmentVariables().values()) {
            final String key = entry.getKey();
            if (showAll || admiralServiceConfig.showVariable(key)) {
                final String value = entry.getDisplayValue();
                final String source = entry.getSource();
                if (containerValueMatchesConfig(containersVars, key, value, source)) {
                    writeVariable(entry, INDENT);
                }
            }
        }
    }

    public boolean containerValueMatchesConfig(Map<String, Map<String, String>> containersVars, String key, String configValue, String configSource) {
        boolean matches = true;
        for(Map.Entry<String, Map<String, String>> containerEntry : containersVars.entrySet()) {
            final String containerName = containerEntry.getKey();
            final Map<String, String> containerVars = containerEntry.getValue();
            final String containerValue = containerVars.get(key);
            if (containerValue != null) {
                if (!configValue.equals(containerValue)) {
                    // Handle the case where the container doesn't match the config
                    writer.outln("  " + styler.heading.format(key) + "=" + styler.formatValue(containerValue) + " !!! " + styler.warning.format("WARNING - Value in container ") + styler.container.format(containerName) + styler.warning.format(" does not match value in configuration: ") + styler.formatValue(configValue) + " (" + styler.file.format(configSource) + ") !!!" );
                    matches = false;
                }
            }
        }
        return matches;
    }

    public void writeEnvACT(AdmiralServiceConfig admiralServiceConfig) {
        writer.outln(styler.section.format(admiralServiceConfig.getName()+":"));
        for (Map.Entry<String, ContainerParameterProcessor.Entry> ev : admiralServiceConfig.getEnvironmentVariables().entrySet()) {
            final ContainerParameterProcessor.Entry entry = ev.getValue();
            writeVariable(entry);
        }
    }

    public void writeEnvACT(AdmiralServiceConfig admiralServiceConfig, String[] vars) {
        writer.outln(styler.section.format(admiralServiceConfig.getName()+":"));

        for(String var: vars) {
            final ContainerParameterProcessor.Entry entry = admiralServiceConfig.getEnvironmentVariables().get(var);
            writeVariable(entry);
        }
    }

    /**
     * This should only be invoked by event handlers, where we don't need to connect if necessary and don't have an Admiral context.
     */
    public void writeContainerVariables(DockerModelController dockerModelController, String containerName, AdmiralContainerConfig admiralContainerConfig) {
        Map<String, String> containerVars = Collections.EMPTY_MAP;
        try {
            containerVars = dockerModelController.getContainerEnvironmentVariables(containerName);
        } catch (AdmiralDockerException e) {
            // Eat it.  This is optional data.
        }
        boolean needHeader = true;
        for(String rule : admiralContainerConfig.getShowVariables()) {
            for(ContainerParameterProcessor.Entry entry : admiralContainerConfig.getEnvironmentVariables().values()) {
                final String key = entry.getKey();
                if (key.contains(rule)) {
                    if (needHeader) {
                        writer.outln(styler.section.format("Container Parameter Dig:"));
                        writer.outln(styler.heading.format("The following parameters will be used by ") + styler.container.format(containerName));
                        writer.outln(styler.log.format("(Only parameters listed in x-admiral_dig are shown here.  Other parameters may also be used.)"));
                        needHeader = false;
                    }
                    final String value = entry.getDisplayValue();
                    final String source = entry.getSource();
                    final String containerValue = containerVars.get(key);
                    if (containerValue != null) {
                        if (!value.equals(containerValue)) {
                            // Handle the case where the container doesn't match the config
                            writer.outln("  " + styler.warning.format("***********************************************************************"));
                            writer.outln("  " + styler.warning.format("*** WARNING - Value in container does not match value in configuration."));
                            writer.outln("  " + styler.warning.format("***   ") + styler.heading.format(key) + "=" + styler.formatValue(containerValue) + " (" + styler.container.format(containerName) + ")");
                            writer.outln("  " + styler.warning.format("***   ") + styler.heading.format(key) + "=" + styler.formatValue(value) + " (" + styler.file.format(source) + ")");
                            writer.outln("  " + styler.warning.format("***********************************************************************"));
                            continue;
                        }
                    }
                    writeVariable(entry, INDENT);
                }
            }
        }
    }

    public static final String INDENT = "  ";

    public void writeVariable(final ContainerParameterProcessor.Entry entry) {
        writeVariable(entry, INDENT);
    }

    /**
     * entry is null-safe
     */
    public void writeVariable(final ContainerParameterProcessor.Entry entry, final String indent) {
        if (entry==null)
            return;

        final String key = entry.getKey();
        final String value = entry.getDisplayValue();
        final String source = entry.getSource();
        final String optionalSource;
        if (preferences.showSource) {
            optionalSource = " (" + styler.file.format(source) + ")";
        } else {
            optionalSource = "";
        }
        writer.outln(indent + styler.heading.format(key) + "=" + styler.formatValue(value) + optionalSource);
    }
}
