package com.optum.admiral.action;

import com.optum.admiral.ContainerParameterProcessor;
import com.optum.admiral.config.AdmiralNetworkConfig;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.model.ExecuteHook;
import com.optum.admiral.model.HealthCheck;
import com.optum.admiral.model.NetworkRef;
import com.optum.admiral.type.Dependant;
import com.optum.admiral.type.Duration;
import com.optum.admiral.type.PortMap;
import com.optum.admiral.type.VariableSpec;
import com.optum.admiral.type.Volume;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigACT {
    private final ComposeConfig composeConfig;
    private final OutputWriter writer;

    public ConfigACT(ComposeConfig composeConfig, OutputWriter outputWriter) {
        this.composeConfig = composeConfig;
        this.writer = outputWriter;
    }

    public void perform() {
        if (!composeConfig.getServicesOrEmpty().isEmpty()) {
            writer.outln("services:");
            for (AdmiralServiceConfig admiralServiceConfig : composeConfig.getServicesOrEmpty()) {
                write(admiralServiceConfig);
            }
        }
        if (!composeConfig.getNetworks().isEmpty()) {
            writer.outln("networks:");
            for (AdmiralNetworkConfig admiralNetworkConfig : composeConfig.getNetworks()) {
                write(admiralNetworkConfig);
            }
        }
    }

    private void write(AdmiralNetworkConfig admiralNetworkConfig) {
        writeName(admiralNetworkConfig);
        writeFields(admiralNetworkConfig);
    }

    private void writeName(AdmiralNetworkConfig admiralNetworkConfig) {
        writer.outln(String.format("  %s:", admiralNetworkConfig.getName()));
    }

    private void writeFields(AdmiralNetworkConfig admiralNetworkConfig) {
        if (admiralNetworkConfig.isExternal())
            writer.outln("    external: true");
        writer.outln(String.format("    name: %s", admiralNetworkConfig.getDockerNetworkName()));
    }

    private void write(AdmiralServiceConfig admiralServiceConfig) {
        writeName(admiralServiceConfig);
        writeFields(admiralServiceConfig);
        writeDependsOn(admiralServiceConfig);
        writeHealthCheck(admiralServiceConfig);
        writeEnvironmentVariableConstraints(admiralServiceConfig);
        writeEnvironment(admiralServiceConfig);
        writePorts(admiralServiceConfig);
        writeVolumes(admiralServiceConfig);
        writeVolumesFrom(admiralServiceConfig);
        writeNetworks(admiralServiceConfig);
        writePostExecuteHook(admiralServiceConfig);
        writeServiceGroups(admiralServiceConfig);
    }

    private void writeFields(AdmiralServiceConfig admiralServiceConfig) {
        writer.outln(String.format("    image: %s", admiralServiceConfig.getImage()));
        if (!admiralServiceConfig.getPlatform().isEmpty()) {
            writer.outln(String.format("    platform: %s", admiralServiceConfig.getPlatform()));
        }
        final List<String> command = admiralServiceConfig.getCommand();
        if (!command.isEmpty()) {
            boolean notFirst = false;
            StringBuilder sb = new StringBuilder();
            for(String arg : command) {
                if (notFirst) {
                    sb.append(", ");
                }
                notFirst = true;
                sb.append("\"" + arg + "\"");
            }
            writer.outln(String.format("    command: [" + sb + "]"));
        }
        final List<String> entrypoint = admiralServiceConfig.getEntrypoint();
        if (!entrypoint.isEmpty()) {
            boolean notFirst = false;
            StringBuilder sb = new StringBuilder();
            for(String arg : entrypoint) {
                if (notFirst) {
                    sb.append(", ");
                }
                notFirst = true;
                sb.append("\"" + arg + "\"");
            }
            writer.outln(String.format("    entrypoint: [" + sb + "]"));
        }
        final Duration sgp = admiralServiceConfig.getStopGracePeriod();
        if (sgp.getMS() != 10000) {
            writer.outln(String.format("    stop_grace_period: %s", Duration.conciseMS(sgp.getMS(), true)));
        }
    }

    private void writeName(AdmiralServiceConfig admiralServiceConfig) {
        writer.outln(String.format("  %s:", admiralServiceConfig.getName()));
    }

    private void writeDependsOn(AdmiralServiceConfig admiralServiceConfig) {
        if (admiralServiceConfig.getDependsOnCount() > 0) {
            writer.outln("    depends_on:");
            for (Dependant dependant : admiralServiceConfig.getDependsOn()) {
                writer.outln(String.format("      %s:", dependant.getServiceName()));
                writer.outln(String.format("        condition: %s", dependant.getCondition()));
            }
        }
    }

    private void writeServiceGroups(AdmiralServiceConfig admiralServiceConfig) {
        if (admiralServiceConfig.hasServiceGroups()) {
            writer.outln("    x-admiral_service_groups:");
            for (String serviceGroup : admiralServiceConfig.getServiceGroups()) {
                writer.outln(String.format("      - \"%s\"", serviceGroup));
            }
        }
    }

    private void writeHealthCheck(AdmiralServiceConfig admiralServiceConfig) {
        HealthCheck healthCheck = admiralServiceConfig.getComposeHealthCheck();
        if (healthCheck!=null) {
            writer.outln("    healthcheck:");
            writer.outln(String.format("      test: %s", healthCheck.getTest()));
            writer.outln(String.format("      interval: %s", Duration.conciseMS(healthCheck.interval)));
            writer.outln(String.format("      timeout: %s", Duration.conciseMS(healthCheck.timeout)));
            writer.outln(String.format("      retries: %s", healthCheck.retries));
            writer.outln(String.format("      start_period: %s", Duration.conciseMS(healthCheck.start_period)));
            writer.outln(String.format("      disabled: %s", healthCheck.disabled));
            writer.outln(String.format("      x-admiral_id: %s", healthCheck.id));
            writer.outln(String.format("      x-admiral_minimum_interval: %s", healthCheck.minimum_interval));
        }
    }

    private void writeEnvironmentVariableConstraints(AdmiralServiceConfig admiralServiceConfig) {
        // NOTE: An empty block is DIFFERENT than no block.
        // An empty block means the service is not allowed to use any environment variables.
        // No block means nothing is validated and the service can do whatever it wants.
        if (admiralServiceConfig.getEnvironmentSpecification()!=null) {
            writer.outln("    x-admiral_environment_variable_constraints:\n");
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

    private void writeEnvironment(AdmiralServiceConfig admiralServiceConfig) {
        if (admiralServiceConfig.getEnvironmentVariableCount() > 0) {
            writer.outln("    environment:");
            for (Map.Entry<String, ContainerParameterProcessor.Entry> ev : admiralServiceConfig.getEnvironmentVariables().entrySet()) {
                final String key = ev.getKey();
                final String value = ev.getValue().getDisplayValue();
                writer.outln(String.format("      %s: %s", key, value));
            }
        }
    }

    private void writePorts(AdmiralServiceConfig admiralServiceConfig) {
        if (admiralServiceConfig.getPortsCount() > 0) {
            writer.outln("    ports:");
            for (PortMap portMap : admiralServiceConfig.getPortMaps()) {
                writer.outln(String.format("      - protocol: %s", portMap.getProtocol()));
                writer.outln(String.format("        published: %s", portMap.getPublished()));
                writer.outln(String.format("        target: %s", portMap.getTarget()));
            }
        }

    }

    private void writeVolumes(AdmiralServiceConfig admiralServiceConfig) {
        Collection<Volume> volumes = admiralServiceConfig.getVolumes();
        if (!volumes.isEmpty()) {
            writer.outln("   volumes:");
            for(Volume volume : volumes) {
                volume.write(writer);
            }
        }
    }

    private void writeVolumesFrom(AdmiralServiceConfig admiralServiceConfig) {
        Set<String> volumesFrom = admiralServiceConfig.getVolumesFrom();
        if (!volumesFrom.isEmpty()) {
            writer.outln("   volumes_from:");
            for(String volume : volumesFrom) {
                writer.outln("     - " + volume);
            }
        }
    }

    private void writeNetworks(AdmiralServiceConfig admiralServiceConfig) {
        if (!admiralServiceConfig.getNetworks().isEmpty()) {
            writer.outln("    networks:");
            for(Map.Entry<String, NetworkRef> entry : admiralServiceConfig.getNetworks().entrySet()) {
                final String name = entry.getKey();
                final NetworkRef networkRef = entry.getValue();
                writer.outln(String.format("      %s:", name));
                if (!networkRef.getAliases().isEmpty()) {
                    writer.outln("        aliases:");
                    for (String alias : networkRef.getAliases()) {
                        writer.outln(String.format("          - %s", alias));
                    }
                }
            }
        }
    }

    private void writePostExecuteHook(AdmiralServiceConfig admiralServiceConfig) {
        List<ExecuteHook> executeHooks = admiralServiceConfig.getPostExecuteHooks();
        if (!executeHooks.isEmpty()) {
            writer.outln("   x-post_execute_hook:");
            for(ExecuteHook executeHook : executeHooks) {
                writer.outln(String.format("      - cmd: %s", executeHook.getCommand()));
                writer.outln(String.format("        working_dir: %s", executeHook.getWorkingDir()));
            }
        }
    }
}
