package com.optum.admiral.yaml;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ServiceYaml implements ServiceYamlTweakableSupplier {
    private ComposeYaml parent;

    public ServiceYaml() {
    }

    public ComposeYaml getParent() {
        return parent;
    }

    public void setParent(ComposeYaml parent) {
        this.parent = parent;
    }

    public File getRelativePathForEnvFile(String envFile) {
        return parent.getRelativePathForEnvFile(envFile);
    }

    public String image;
    public String platform;
    public HealthCheckYaml healthcheck;
    public String stop_grace_period;
    public String x_admiral_code_version;
    public List<String> x_admiral_action_monitors = Collections.emptyList();
    public List<String> x_admiral_dig = Collections.emptyList();
    public List<String> x_admiral_environment_specs = Collections.emptyList();
    public List<String> x_admiral_groups = Collections.emptyList();
    public List<LogMonitorYaml> x_admiral_log_monitors = Collections.emptyList();
    public List<CopyHookYaml> x_admiral_post_create_copy = Collections.emptyList();
    public List<ExecuteHookYaml> x_admiral_post_create_execute = Collections.emptyList();
    public List<WaitHookYaml> x_admiral_post_start_wait = Collections.emptyList();
    public DeployYaml deploy = new DeployYaml();
    public List<String> depends_on = Collections.emptyList();
    /**
     * In the "config" feature of docker-compose, ports are listed in the order they were defined.
     */
    public List<String> ports = new ArrayList<>();
    public List<VolumeYaml> volumes = new ArrayList<>();
    public List<String> extra_hosts = new ArrayList<>();
    public List<String> volumes_from = new ArrayList<>();
    public List<String> tmpfs = new ArrayList<>();

    public Map<String, NetworkRefYaml> networks = new TreeMap<>();

    // ***
    // *** These MUST BE private.  If they were public, SnakeYaml would treat them as valid fields in the yaml.
    // ***

    // If this is named "env_file", SnakeYaml ignores calling setEnv_file.
    private List<String> env_fileDynamicType = Collections.emptyList();

    // If this is named "environment", SnakeYaml ignores calling setEnvironment.
    private Map<String, String> environmentDynamicType = new HashMap<>();

    // If this is named "command", SnakeYaml ignores calling setCommand.
    private List<String> commandDynamicType = Collections.emptyList();

    // If this is named "entrypoint", SnakeYaml ignores calling setCommand.
    private List<String> entrypointDynamicType = Collections.emptyList();

    // ***
    // *** End of private fields.
    // ***

    public String getImage() {
        return image;
    }

    public String getPlatform() {
        return platform;
    }

    /**
     * This is invoked by SnakeParser with an Object so that we can handle both type cases.
     */
    public void setEnv_file(Object o) {
        if (o instanceof String) {
            env_fileDynamicType = new ArrayList(1);
            env_fileDynamicType.add((String)o);
        } else {
            env_fileDynamicType = (List)o;
        }
    }

    public List<String> getEnvFiles() {
        return env_fileDynamicType;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentDynamicType;
    }

    /**
     * This is invoked by SnakeParser with an Object so that we can handle both type cases.
     */
    public void setCommand(Object o) {
        if (o instanceof String) {
            final String commandString = (String)o;
            commandDynamicType = Arrays.asList(commandString.split(" "));
        } else {
            commandDynamicType = (List)o;
        }
    }

    /**
     * This can't be called getCommand() or SnakeParser complains.
     * @return
     */
    public List<String> getCommandPieces() {
        return commandDynamicType;
    }

    /**
     * This is invoked by SnakeParser with an Object so that we can handle both type cases.
     */
    public void setEntrypoint(Object o) {
        if (o instanceof String) {
            final String entrypointString = (String)o;
            entrypointDynamicType = Arrays.asList(entrypointString.split(" "));
        } else {
            entrypointDynamicType = (List)o;
        }
    }

    /**
     * This can't be called getEntrypoint() or SnakeParser complains.
     */
    public List<String> getEntrypointPieces() {
        return entrypointDynamicType;
    }

    private String safeAdd(String key, Object possibleValue, String defaultValue) {
        if (possibleValue==null) {
            if (defaultValue!=null) {
                return defaultValue;
            } else {
                throw new IllegalArgumentException("Required value for " + key + " is missing.");
            }
        }
        if (possibleValue instanceof String) {
            return (String) possibleValue;
        } else {
            throw new IllegalArgumentException("PossibleValue is not a String.  It is a " + possibleValue.getClass() + " with value " + possibleValue);
        }
    }

    /**
     * This is invoked by SnakeParser with an Object so that we can handle both type cases.
     * @return
     */
    public void setEnvironment(Object o) {
        if (o instanceof List) {
            for(String line : (List<String>)o) {
                String pieces[] = line.split("=", 2);
                if (pieces.length==2) {
                    environmentDynamicType.put(pieces[0], pieces[1]);
                } else if (pieces.length==1) {
                    environmentDynamicType.put(pieces[0], null);
                }
            }
        } else if (o instanceof Map) {
            final Map<Object, Object> asMap = (Map)o;
            for(Map.Entry entry : asMap.entrySet()) {
                final Object key = entry.getKey();
                final Object value = entry.getValue();
                if (value!=null) {
                    environmentDynamicType.put(key.toString(), value.toString());
                } else {
                    environmentDynamicType.put(key.toString(), null);
                }
            }
        } else {
            throw new IllegalStateException("Environment read by SnakeParser was neither a List nor a Map.  It was an unexpected " + o.getClass());
        }
    }

    public HealthCheckYaml getHealthCheck() {
        return healthcheck;
    }

    @Override
    public List<String> getGroups() {
        return x_admiral_groups;
    }

    @Override
    public List<String> getDependsOn() {
        return depends_on;
    }

    @Override
    public List<String> getDig() {
        return x_admiral_dig;
    }

    @Override
    public List<String> getExposedPorts() {
        if (ports==null) return Collections.emptyList();
        return ports;
    }

    public List<VolumeYaml> getVolumes() {
        return volumes;
    }

    public void setImage(String image) {
        this.image = image;
    }

}
