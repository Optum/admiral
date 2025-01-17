package com.optum.admiral.config;

import com.optum.admiral.ConfigVariableProcessor;
import com.optum.admiral.ContainerNamingConvention;
import com.optum.admiral.GroupEngine;
import com.optum.admiral.io.AdmiralFileException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.model.NetworkRef;
import com.optum.admiral.type.Dependant;
import com.optum.admiral.type.exception.InvalidSemanticVersion;
import com.optum.admiral.type.exception.VariableSpecContraint;
import com.optum.admiral.util.FileService;
import com.optum.admiral.yaml.ComposeYaml;
import com.optum.admiral.yaml.NetworkYaml;
import com.optum.admiral.yaml.ServiceYaml;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class ComposeConfig implements ContainerNamingConvention {
    private static final String DEFAULT = "default";

    private final String sourceType;
    private final String sourceFilenames;
    private final String projectName;
    private ContainerNamingConvention containerNamingConvention;
    private final ConfigVariableProcessor configVariableProcessor;

    private final Map<String, AdmiralServiceConfig> services = new TreeMap<>();
    private Collection<AdmiralServiceConfig> servicesReadOnlyValues;
    private final Map<String, AdmiralNetworkConfig> networks = new TreeMap<>();

    private final GroupEngine groupEngine = new GroupEngine(this);

    public GroupEngine getGroupEngine() {
        return groupEngine;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceFilenames() {
        return sourceFilenames;
    }

    public String getProjectName() {
        return projectName;
    }

    /**
     * TODO:  Only used by Spec.  Needs to switch to applyServiceYaml
     */
    @Deprecated
    public void addServiceConfig(AdmiralServiceConfig admiralServiceConfig) {
        services.put(admiralServiceConfig.getName(), admiralServiceConfig);
    }

    public boolean containsService(String serviceName) {
        return services.containsKey(serviceName);
    }

    public Collection<AdmiralServiceConfig> getServices() throws AdmiralServiceConfigNotFoundException {
        if (servicesReadOnlyValues.isEmpty()) {
            throw new AdmiralServiceConfigNotFoundException(null);
        }
        return servicesReadOnlyValues;
    }

    public Collection<AdmiralServiceConfig> getServicesCopy() throws AdmiralServiceConfigNotFoundException {
        if (services.isEmpty()) {
            throw new AdmiralServiceConfigNotFoundException(null);
        }
        Collection<AdmiralServiceConfig> servicesCopy = new TreeSet<>();
        servicesCopy.addAll(services.values());
        return servicesCopy;
    }

    public Collection<AdmiralServiceConfig> getServicesOrEmpty() {
        return services.values();
    }

    public Collection<AdmiralServiceConfig> getServicesCopyOrAll(Collection<String> serviceNames) throws AdmiralServiceConfigNotFoundException {
        if (serviceNames.isEmpty()) {
            return getServicesCopy();
        } else {
            Collection<AdmiralServiceConfig> services = new ArrayList<>();
            for (String serviceName : serviceNames) {
                AdmiralServiceConfig admiralServiceConfig = getServiceConfig(serviceName);
                if (admiralServiceConfig != null) {
                    services.add(admiralServiceConfig);
                } else {
                    throw new AdmiralServiceConfigNotFoundException(serviceName);
                }
            }
            return services;
        }
    }

    public Collection<String> getServiceNames() {
        return services.keySet();
    }

    public Collection<AdmiralNetworkConfig> getNetworks() {
        return networks.values();
    }

    public AdmiralNetworkConfig getNetwork(String composeNetworkReferenceName) {
        return networks.get(composeNetworkReferenceName);
    }

    public AdmiralServiceConfig getServiceConfig(String serviceName) {
        return services.get(serviceName);
    }

    /**
     * ComposeConfig is constructed in the context provided by a ConfigVariableProcessor.  The job
     * of the ConfigVariableProcessor is to hold environment variables (config scope, not container scope).
     * @param configVariableProcessor
     */
    public ComposeConfig(String sourceType, String sourceFilenames, String projectName, ConfigVariableProcessor configVariableProcessor, ContainerNamingConvention containerNamingConvention) {
        this.sourceType = sourceType;
        this.sourceFilenames = sourceFilenames;
        this.projectName = projectName;
        this.configVariableProcessor = configVariableProcessor;
        this.containerNamingConvention = containerNamingConvention;
    }

    @Override
    public String calculateContainerName(String serviceName, int replicaInstance) {
        return containerNamingConvention.calculateContainerName(serviceName, replicaInstance);
    }

    public Map<String, ConfigVariableProcessor.Entry> getData() {
        return configVariableProcessor.getData();
    }

    public void applyServiceYaml(FileService fileService, ConfigVariableProcessor variableProcessor, String serviceName, ServiceYaml serviceYaml)
            throws AdmiralConfigurationException, InvalidSemanticVersion, VariableSpecContraint, InvalidBooleanException, PropertyNotFoundException, InvalidEnumException {
        AdmiralServiceConfig admiralServiceConfig = services.computeIfAbsent(serviceName,
            k -> new AdmiralServiceConfig(serviceName));
        admiralServiceConfig.applyServiceYaml(fileService, serviceYaml, variableProcessor);
    }

    public void applyNetworkYaml(ConfigVariableProcessor variableProcessor, String networkName, NetworkYaml networkYaml)
            throws AdmiralConfigurationException {
        AdmiralNetworkConfig admiralNetworkConfig = networks.computeIfAbsent(networkName,
            k -> new AdmiralNetworkConfig(projectName, networkName));
        admiralNetworkConfig.applyNetworkYaml(networkYaml, variableProcessor);
    }

    public void load(File composeFile)
            throws
                AdmiralConfigurationException,
                AdmiralFileException,
            InvalidEnumException,
                InvalidBooleanException,
                InvalidSemanticVersion,
                PropertyNotFoundException,
                VariableSpecContraint {
        // Go
        final ComposeYaml composeYaml = ComposeYaml.loadFromYamlFile(composeFile);
        final FileService fileService = FileService.getFileServiceForContainingDirectoryOf(composeFile);

        for (Map.Entry<String, ServiceYaml> service : composeYaml.services.entrySet()) {
            // Gather
            final String serviceName = service.getKey();
            final ServiceYaml serviceYaml = service.getValue();
            // Go
            applyServiceYaml(fileService, configVariableProcessor, serviceName, serviceYaml);
        }

        // Guard
        if (composeYaml.networks != null) {
            for (Map.Entry<String, NetworkYaml> network : composeYaml.networks.entrySet()) {
                // Gather
                final String networkName = network.getKey();
                final NetworkYaml networkYaml = network.getValue();
                // Go
                applyNetworkYaml(configVariableProcessor, networkName, networkYaml);
            }
        }
    }

    public void verifyServiceDependsOn() throws InvalidDependsOnException {
        List<InvalidDependsOnException.InvalidDependsOn> errors = new ArrayList<>();
        for(AdmiralServiceConfig admiralServiceConfig : services.values()) {
            String serviceName = admiralServiceConfig.getName();
            for(Dependant dependant : admiralServiceConfig.getDependsOn()) {
                String dependsOn = dependant.getServiceName();
                if (!services.containsKey(dependsOn)) {
                    InvalidDependsOnException.InvalidDependsOn invalidDependsOn = new InvalidDependsOnException.InvalidDependsOn(serviceName, dependsOn);
                    errors.add(invalidDependsOn);
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new InvalidDependsOnException(errors);
        }
    }

    /**
     * The following conditions are checked:
     * For each service:
     *   1) If it had zero networks, "default" is created.
     *   2) For each network (which now includes the possibility of "default" form above), the network exists in root networks:
     *   3) If the above processing detects that the service is using the "default" network, confirm it exists or create it.
     */
    public void verifyNetworks() throws AdmiralConfigurationException {
        for(AdmiralServiceConfig admiralServiceConfig : getServicesOrEmpty()) {
            boolean theDefaultNetworkWasExplicitlyReferenced = false;
            boolean aUserDefinedNetworkWasExplicitlyReferenced = false;
            for(NetworkRef networkRef : admiralServiceConfig.getNetworks().values()) {
                final String networkName = networkRef.getName();
                if (DEFAULT.equals(networkName))
                    theDefaultNetworkWasExplicitlyReferenced = true;
                else {
                    aUserDefinedNetworkWasExplicitlyReferenced = true;
                    if (!networks.containsKey(networkName)) {
                        throw new AdmiralConfigurationException("Network " + networkName + " is used by " + admiralServiceConfig.getName() + " but is not defined.");
                    }
                }
            }
            if (aUserDefinedNetworkWasExplicitlyReferenced)
                continue;
            if (theDefaultNetworkWasExplicitlyReferenced) {
                createDefaultNetworkIfNecessary(projectName);
            }
        }
    }

    private void createDefaultNetworkIfNecessary(String projectName) {
        if (networks.containsKey(DEFAULT))
            return;

        networks.put(DEFAULT, new AdmiralNetworkConfig(projectName, DEFAULT));
    }

    public void doFinalConstructionProcessing() {
        for(AdmiralServiceConfig admiralServiceConfig : services.values()) {
            for (String serviceGroupName : admiralServiceConfig.getServiceGroups()) {
                groupEngine.associate(serviceGroupName, admiralServiceConfig.getName());
            }
        }
        servicesReadOnlyValues = Collections.unmodifiableMap(services).values();
    }

}
