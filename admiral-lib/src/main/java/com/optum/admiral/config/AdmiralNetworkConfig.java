package com.optum.admiral.config;

import com.optum.admiral.ConfigVariableProcessor;
import com.optum.admiral.Version;
import com.optum.admiral.type.DockerComposeNetworkName;
import com.optum.admiral.yaml.NetworkYaml;
import com.optum.admiral.yaml.YamlParserHelper;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;

import java.util.HashMap;
import java.util.Map;

/**
 * There are lots of configuration scenarios.  LIBRARY code JUST WORKS using the correct name of either
 * DockerNetworkName or ComposeNetworkReferenceName and isExternal depending on what it is doing.
 *
 * AKA:
 *   *** No IF checks in LIBRARY code!!!
 *   *** No "default".equals(name) in LIBRARY code!!!!
 *
 * It is the CONFIGURATION code that has to parse and understand
 * the YAML Config to know how to set up the appropriate AdmiralNetworkConfig implementation.
 *
 * For the default network of a compose project "demo":
 *   DockerNetworkName = "demo_default"
 *   ComposeNetworkReferenceName = "default"
 *   isExternal = false
 *
 * For an external network called "notmynetwork" used as the default network:
 *   DockerNetworkName = "notmynetwork"
 *   ComposeNetworkReferenceName = "default"
 *   isExternal = true
 *
 * For an external network called "notmynetwork" used as an explicit network:
 *   DockerNetworkName = "notmynetwork"
 *   ComposeNetworkReferenceName = "notmynetwork"
 *   isExternal = true
 */
public class AdmiralNetworkConfig {
    private final DockerComposeNetworkName dockerComposeNetworkName;

    private Map<String, String> labels = new HashMap<>();
    private boolean external = false;

    public AdmiralNetworkConfig(String projectName, String networkName) {
        this.dockerComposeNetworkName = new DockerComposeNetworkName(projectName, networkName);

        labels.put("com.docker.compose.network", networkName);
        labels.put("com.docker.compose.project", projectName);
        labels.put("com.docker.compose.version", "dash " + Version.VERSION);
    }

    public void applyNetworkYaml(NetworkYaml networkYaml, ConfigVariableProcessor configVariableProcessor) throws AdmiralConfigurationException {
        // Yes, null is expected.
        // If the root networks: section lists a network, but doesn't specify any attributes, that network is created
        // with default values and this applyNetworkYaml no-ops.
        if (networkYaml==null)
            return;

        YamlParserHelper yph = new YamlParserHelper(configVariableProcessor);

        this.external = yph.getB("external", networkYaml.external);
    }

    /**
     * This is the name of the network according to the Docker Engine.
     * Typing "docker network ls" will show this value in the NAME column.
     * It may or may not be the same value as the Compose Network Reference Name.
     * @return
     */
    public String getDockerNetworkName() {
        return dockerComposeNetworkName.getName();
    }

    /**
     * This is the name of the network according to the Compose YAML file.
     * Blocks under the main "networks:" section use this name.
     * @return
     */
    public String getComposeNetworkReferenceName() {
        return dockerComposeNetworkName.getNetworkLogicalName();
    }

    /**
     * The ComposeNetworkReferenceName is our Name.
     */
    public String getName() {
        return getComposeNetworkReferenceName();
    }

    public boolean isExternal() {
        return external;
    }

    public String getProject() {
        return dockerComposeNetworkName.getProjectName();
    }

    public Map<String, String> getLabels() {
        return labels;
    }
}
