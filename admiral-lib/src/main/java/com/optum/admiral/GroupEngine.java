package com.optum.admiral;

import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class GroupEngine {
    private final ComposeConfig composeConfig;

    private final Map<String, Set<String>> groups = new TreeMap<>();

    public GroupEngine(ComposeConfig composeConfig) {
        this.composeConfig = composeConfig;
    }

    public void associate(String groupName, String serviceName) {
        groups.computeIfAbsent(groupName, k -> new TreeSet<>()).add(serviceName);
    }

    public Collection<String> getServiceGroupNames() {
        return groups.keySet();
    }

    public Set<String> getServiceGroupList(String serviceGroupName) {
        return groups.get(serviceGroupName);
    }

    public List<AdmiralServiceConfig> expandGroups(List<String> serviceOrGroupNames) throws AdmiralServiceConfigNotFoundException {
        // Since order is important, the results need to be in a list, not a set.  But don't add it twice.
        List<String> serviceNames = new ArrayList<>();
        for(String name : serviceOrGroupNames) {
            Collection<String> groupServiceNames = expandServiceNames(name);
            for(String service : groupServiceNames) {
                if (!serviceNames.contains(service)) {
                    serviceNames.add(service);
                }
            }
        }
        List<AdmiralServiceConfig> services = new ArrayList<>();
        for(String serviceName : serviceNames) {
            AdmiralServiceConfig admiralServiceConfig = composeConfig.getServiceConfig(serviceName);

            if (admiralServiceConfig == null) {
                throw new AdmiralServiceConfigNotFoundException(serviceName);
            }

            services.add(admiralServiceConfig);
        }
        return services;
    }

    /**
     * This is a "magic" method that takes vague input (a service name or a group name) and
     * returns a collection of true service names.  In the case the vague input is a service name,
     * we simply return that name.  If it is a group name, we return the service names of that group.
     * If neither, we return the original vague input, which will then throw an error later.
     * @param serviceOrGroupName
     * @return
     */
    public Collection<String> expandServiceNames(String serviceOrGroupName) {
        if (composeConfig.containsService(serviceOrGroupName)) {
            return Collections.singleton(serviceOrGroupName);
        } else {
            Collection<String> group = groups.get(serviceOrGroupName);
            if (group!=null)
                return group;
        }
        return Collections.singleton(serviceOrGroupName);
    }

}
