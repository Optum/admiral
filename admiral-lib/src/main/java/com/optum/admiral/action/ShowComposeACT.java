package com.optum.admiral.action;

import com.optum.admiral.Admiral;
import com.optum.admiral.config.AdmiralNetworkConfig;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.io.VariableWriterUtil;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.model.ExecuteHook;
import com.optum.admiral.model.HealthCheck;
import com.optum.admiral.model.NetworkRef;
import com.optum.admiral.model.URLAdmiralHealthCheck;
import com.optum.admiral.preferences.OutputPreferences;
import com.optum.admiral.type.Dependant;
import com.optum.admiral.type.Duration;
import com.optum.admiral.type.PortMap;
import com.optum.admiral.type.VariableSpec;
import com.optum.admiral.type.Volume;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ShowComposeACT {
    private final Admiral admiral;
    private final OutputStyler os;
    private final OutputWriter writer;
    private final ComposeConfig composeConfig;
    private final VariableWriterUtil variableWriterUtil;

    public ShowComposeACT(Admiral admiral, ComposeConfig composeConfig, OutputPreferences preferences, OutputStyler styler, OutputWriter writer) {
        this.admiral = admiral;
        this.composeConfig = composeConfig;
        this.os = styler;
        this.writer = writer;
        this.variableWriterUtil = new VariableWriterUtil(preferences, styler, writer);
    }

    public void perform(Collection<String> serviceNames) throws AdmiralServiceConfigNotFoundException {
        Collection<AdmiralServiceConfig> services = composeConfig.getServicesCopyOrAll(serviceNames);

        writer.outln(os.section.format("Services:"));

        boolean needsBlankLine = false;
        for (AdmiralServiceConfig admiralServiceConfig : services) {
            if (needsBlankLine) {
                writer.outln("");
            }
            needsBlankLine = true;

            writeName(admiralServiceConfig);
            writeFields(admiralServiceConfig);
            writeVolumesSection(admiralServiceConfig);
            writeVolumesFromSection(admiralServiceConfig);
            writeHealthCheckSection(admiralServiceConfig);
            writePostExecuteHookSection(admiralServiceConfig);
            writeEnvironmentVariableConstraintsSection(admiralServiceConfig);
        }

        Collection<AdmiralNetworkConfig> networks = composeConfig.getNetworks();

        writer.outln("");
        writer.outln(os.section.format("Networks:"));

        needsBlankLine = false;
        for (AdmiralNetworkConfig admiralNetworkConfig : networks) {
            if (needsBlankLine) {
                writer.outln("");
            }
            needsBlankLine = true;

            writer.outln(String.format("  %s:", os.networkHeading.format(admiralNetworkConfig.getName())));
            writeSubsection("Docker Name", admiralNetworkConfig.getDockerNetworkName());
            writeSubsection("External", os.formatValue(Boolean.toString(admiralNetworkConfig.isExternal())));
        }
    }

    private void writeName(AdmiralServiceConfig admiralServiceConfig) {
        writer.outln(String.format("  %s:", os.serviceHeading.format(admiralServiceConfig.getName())));
    }

    private void writeFields(AdmiralServiceConfig admiralServiceConfig) {
        writeServiceGroupsLine(admiralServiceConfig);

        writeDependsOnLine(admiralServiceConfig);

        writeSubsection("Image", admiralServiceConfig.getImage());
        if (!admiralServiceConfig.getPlatform().isEmpty()) {
            writeSubsection("Platform", admiralServiceConfig.getPlatform());
        }

        final List<String> command = admiralServiceConfig.getCommand();

        writePortsLine(admiralServiceConfig);

        if (!command.isEmpty()) {
            writeSubsection("Command", quotedList(command));
        }

        final List<String> entrypoint = admiralServiceConfig.getEntrypoint();
        if (!entrypoint.isEmpty()) {
            writeSubsection("Entrypoint", quotedList(entrypoint));
        }

        final Duration sgp = admiralServiceConfig.getStopGracePeriod();
        if (sgp.getMS() != 10000) {
            writeSubsection("Stop Grace Period", Duration.conciseMS(sgp.getMS(), true));
        }

        writeNetworksLine(admiralServiceConfig);
    }

    private void writeNetworksLine(AdmiralServiceConfig admiralServiceConfig) {
        if (!admiralServiceConfig.getNetworks().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean needsComma = false;
            for(Map.Entry<String, NetworkRef> entry : admiralServiceConfig.getNetworks().entrySet()) {
                if (needsComma) {
                    sb.append(", ");
                } else {
                    needsComma = true;
                }
                final String name = entry.getKey();
                final NetworkRef networkRef = entry.getValue();
                sb.append(os.network.format(name));
                if (!networkRef.getAliases().isEmpty()) {
                    sb.append(" (");
                    sb.append(networkRef.getAliases().stream()
                            .map(x -> os.url.format(x)).collect(Collectors.joining(", ")));
                    sb.append(")");
                }
            }
            writeSubsection("Networks", sb.toString());
        }
    }


    private void writeDependsOnLine(AdmiralServiceConfig admiralServiceConfig) {
        if (admiralServiceConfig.getDependsOnCount() > 0) {
            StringBuilder sb = new StringBuilder();
            boolean needsComma = false;
            for (Dependant dependant : admiralServiceConfig.getDependsOn()) {
                if (needsComma) {
                    sb.append(", ");
                } else {
                    needsComma = true;
                }
                sb.append(os.service.format(dependant.getServiceName()));
                sb.append(" (");
                sb.append(dependant.getCondition().toBrief());
                sb.append(")");
            }
            writeSubsection("Depends on", sb.toString());
        }
    }

    private void writeServiceGroupsLine(AdmiralServiceConfig admiralServiceConfig) {
        if (admiralServiceConfig.hasServiceGroups()) {
            StringBuilder sb = new StringBuilder();
            sb.append(admiralServiceConfig.getServiceGroups().stream()
                    .map(x -> os.group.format(x)).collect(Collectors.joining(", ")));
            writeSubsection("Groups", sb.toString());
        }

    }

    private void writePortsLine(AdmiralServiceConfig admiralServiceConfig) {
        if (admiralServiceConfig.getPortsCount() > 0) {
            StringBuilder sb = new StringBuilder();
            boolean needsComma = false;
            for (PortMap portMap : admiralServiceConfig.getPortMaps()) {
                if (needsComma) {
                    sb.append(", ");
                } else {
                    needsComma = true;
                }
                sb.append(portMap.getPublished());
                sb.append("->");
                sb.append(portMap.getTarget());
                sb.append("(");
                sb.append(portMap.getProtocol());
                sb.append(")");
            }
            writeSubsection("Ports", sb.toString());
        }
    }

    private void writeHealthCheckSection(AdmiralServiceConfig admiralServiceConfig) {
        for(URLAdmiralHealthCheck healthCheck : admiralServiceConfig.getAdmiralHealthChecks()) {
            writeSubsection("Admiral Executed Healthcheck");
            writeHeading("Id", healthCheck.id);
            writeHeading("Disabled", Boolean.toString(healthCheck.disabled));
            writeHeading("Admiral GETs URL", healthCheck.getTest());
            writeHeading("Then Searches For", healthCheck.getSearch());
            writeHeading("Start Period", Duration.conciseMS(healthCheck.start_period, true));
            writeHeading("Timeout", Duration.conciseMS(healthCheck.timeout, true));
            writeHeading("Retries", Integer.toString(healthCheck.retries));
            writeHeading("Interval", Duration.conciseMS(healthCheck.interval, true));
            writeHeading("Minimum Interval", Duration.conciseMS(healthCheck.minimum_interval));
            writeHeading("Rewait Start Period", Duration.conciseMS(healthCheck.rewait_period, true));
            writeHeading("Rewait Interval", Duration.conciseMS(healthCheck.rewait_interval, true));
        }
        HealthCheck healthCheck = admiralServiceConfig.getComposeHealthCheck();
        if (healthCheck!=null) {
            writeSubsection("Container Executed Healthcheck");
            writeHeading("Id", healthCheck.id);
            writeHeading("Disabled", Boolean.toString(healthCheck.disabled));
            writeHeading("Container Executes", healthCheck.getTest());
            writeHeading("Start Period", Duration.conciseMS(healthCheck.start_period, true));
            writeHeading("Timeout", Duration.conciseMS(healthCheck.timeout, true));
            writeHeading("Retries", Integer.toString(healthCheck.retries));
            writeHeading("Interval", Duration.conciseMS(healthCheck.interval, true));
            writeHeading("Minimum Interval", Duration.conciseMS(healthCheck.minimum_interval));
            writeHeading("Rewait Start Period", Duration.conciseMS(healthCheck.rewait_period, true));
            writeHeading("Rewait Interval", Duration.conciseMS(healthCheck.rewait_interval, true));
        }
    }

    private void writeEnvironmentVariableConstraintsSection(AdmiralServiceConfig admiralServiceConfig) {
        // NOTE: An empty block is DIFFERENT than no block.
        // An empty block means the service is not allowed to use any environment variables.
        // No block means nothing is validated and the service can do whatever it wants.
        if (admiralServiceConfig.getEnvironmentSpecification()!=null) {
            writeSubsection("Parameter Requirements (x-admiral_environment_specs)");
            for (VariableSpec variableSpec : admiralServiceConfig.getEnvironmentSpecification().getVariables()) {
                if (variableSpec.firstCodeVersion == null && variableSpec.lastCodeVersion == null) {
                    writer.outln(String.format("      - %s", variableSpec.name));
                } else {
                    writer.outln(String.format("      - name: %s", variableSpec.name));
                    if (variableSpec.firstCodeVersion != null)
                        writer.outln(String.format("        first_code_version: %s", variableSpec.firstCodeVersion));
                    if (variableSpec.lastCodeVersion != null)
                        writer.outln(String.format("        last_code_version: %s", variableSpec.lastCodeVersion));
                }
            }
        }
    }

    private void writeVolumesSection(AdmiralServiceConfig admiralServiceConfig) {
        Collection<Volume> volumes = admiralServiceConfig.getVolumes();
        if (!volumes.isEmpty()) {
            writeSubsection("Volumes");
            for(Volume volume : volumes) {
                volume.write(writer);
            }
        }
    }

    private void writeVolumesFromSection(AdmiralServiceConfig admiralServiceConfig) {
        Set<String> volumesFrom = admiralServiceConfig.getVolumesFrom();
        if (!volumesFrom.isEmpty()) {
            writeSubsection("Volumes From");
            for(String volume : volumesFrom) {
                writer.outln("     - " + volume);
            }
        }
    }

    private void writePostExecuteHookSection(AdmiralServiceConfig admiralServiceConfig) {
        List<ExecuteHook> executeHooks = admiralServiceConfig.getPostExecuteHooks();
        if (!executeHooks.isEmpty()) {
            writeSubsection("Post Execute Hook");
            for(ExecuteHook executeHook : executeHooks) {
                writer.outln(String.format("      - cmd: %s", executeHook.getCommand()));
                writer.outln(String.format("        working_dir: %s", executeHook.getWorkingDir()));
            }
        }
    }

    private void writeHeading(final String heading, final String value) {
        StringBuilder sb = new StringBuilder();
        sb.append("      ");
        sb.append(os.heading.format(heading));
        sb.append(": ");
        sb.append(value);
        writer.outln(sb.toString());
    }

    private void writeSubsection(final String subsection) {
        StringBuilder sb = new StringBuilder();
        sb.append("    ");
        sb.append(os.subsection.format(subsection));
        sb.append(":");
        writer.outln(sb.toString());
    }

    private void writeSubsection(final String subsection, final String value) {
        StringBuilder sb = new StringBuilder();
        sb.append("    ");
        sb.append(os.subsection.format(subsection));
        sb.append(": ");
        sb.append(value);
        writer.outln(sb.toString());
    }

    private String quotedList(final List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean notFirst = false;
        for (String value : values) {
            if (notFirst) {
                sb.append(", ");
            }
            notFirst = true;
            sb.append("\"" + value + "\"");
        }
        sb.append("]");
        return sb.toString();
    }


}
