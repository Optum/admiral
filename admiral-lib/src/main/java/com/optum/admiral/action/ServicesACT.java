package com.optum.admiral.action;

import com.optum.admiral.Admiral;
import com.optum.admiral.AdmiralFormatter;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.model.Container;
import com.optum.admiral.model.DockerModelController;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class ServicesACT {
    private final Admiral admiral;
    private final DockerModelController dmc;
    private final OutputStyler os;
    private final OutputWriter writer;

    public ServicesACT(Admiral admiral, DockerModelController dockerModelController, OutputStyler outputStyler, OutputWriter outputWriter) {
        this.admiral = admiral;
        this.dmc = dockerModelController;
        this.os = outputStyler;
        this.writer = outputWriter;
    }

    public void perform() throws AdmiralServiceConfigNotFoundException, AdmiralDockerException {
        List<Container> list = dmc.listContainers();
        Map<String, String> unmanaged = new TreeMap<>();
        Map<String, String> managed = new TreeMap<>();
        for(Container c : list) {
            final Map<String, String> here;
            String admiralContainerName = admiral.matchAdmiralContainerName(c);
            if (admiralContainerName==null) {
                here = unmanaged;
                admiralContainerName = simpleName(c);
            } else {
                here = managed;
            }
            final String state = c.getState();
            final String status = c.getStatus();
            here.put(admiralContainerName, os.formatState(state) + " (" + status + ")");
        }

        Collection<AdmiralServiceConfig> serviceConfigs = admiral.getAdmiralServiceConfigs();

        final AdmiralFormatter admiralFormatter = new AdmiralFormatter(os);
        boolean needsBlankLine = false;
        for(AdmiralServiceConfig admiralServiceConfig : serviceConfigs) {
            if (needsBlankLine)
                writer.outln("");
            needsBlankLine = true;
            final String serviceName = admiralServiceConfig.getName();
            writer.outln(admiralFormatter.getServiceConciseHeading(admiralServiceConfig));
            final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
            for (int i = 1; i <= replicas; i++) {
                final String containerName = admiral.calculateContainerName(serviceName, i);
                String status = managed.get(containerName);
                if (status==null) {
                    status = os.formatState("not created");
                }
                writer.outln("  " + os.container.format(containerName) + ": " + status);
            }
        }
    }

    private String simpleName(Container c) {
        for(String name : c.getNames()) {
            if (name.startsWith("/")) {
                String clipped = name.substring(1);
                if (!clipped.contains("/"))
                    return clipped;
            }
        }
        // Well bummer. I couldn't find what I thought was the simple Name.  You get all of them.
        return String.join(",", c.getNames());
    }

}
