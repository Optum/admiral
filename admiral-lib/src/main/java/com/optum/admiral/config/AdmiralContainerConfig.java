package com.optum.admiral.config;

import com.optum.admiral.ContainerParameterProcessor;
import com.optum.admiral.model.ExecuteHook;
import com.optum.admiral.model.HealthCheck;
import com.optum.admiral.model.NetworkRef;
import com.optum.admiral.model.StartWait;
import com.optum.admiral.model.URLAdmiralHealthCheck;
import com.optum.admiral.type.CopyHook;
import com.optum.admiral.type.Dependant;
import com.optum.admiral.type.Duration;
import com.optum.admiral.type.PortMap;
import com.optum.admiral.type.Volume;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This interface describes the read-only information necessary to create a Container.  It is, by tradition and
 * convention, defined by a Docker Compose Service.  But rather than exposing Service to DockerModelController, we
 * create this interface.  If a command-line implementation want to create a container without a service - this would
 * be the way.
 */
public interface AdmiralContainerConfig {
    String getName();
    String getImage();
    String getPlatform();
    HealthCheck getComposeHealthCheck();
    List<URLAdmiralHealthCheck> getAdmiralHealthChecks();
    List<String> getCommand();
    List<String> getEntrypoint();
    List<StartWait> getPostStartWaits();
    List<ExecuteHook> getPostExecuteHooks();
    List<CopyHook> getPostCopyHooks();
    Duration getStopGracePeriod();
    Collection<Dependant> getDependsOn();
    Set<String> getExtraHosts();
    Set<String> getVolumesFrom();
    List<PortMap> getPortMaps();
    Collection<Volume> getVolumes();
    List<String> getEnvironentVariablesAsStrings();
    Map<String, NetworkRef> getNetworks();
    Set<String> getShowVariables();
    Map<String, ContainerParameterProcessor.Entry> getEnvironmentVariables();
}
