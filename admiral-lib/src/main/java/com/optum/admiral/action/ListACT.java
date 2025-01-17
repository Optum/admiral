package com.optum.admiral.action;

import com.optum.admiral.Admiral;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.model.Container;
import com.optum.admiral.model.DockerModelController;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ListACT {
    private final Admiral admiral;
    private final DockerModelController dmc;
    private final OutputStyler os;
    private final OutputWriter writer;

    public ListACT(Admiral admiral, DockerModelController dockerModelController, OutputStyler outputStyler, OutputWriter outputWriter) {
        this.admiral = admiral;
        this.dmc = dockerModelController;
        this.os = outputStyler;
        this.writer = outputWriter;
    }

    public void perform() throws AdmiralDockerException {
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
            here.put(os.container.format(admiralContainerName) + ": " + os.formatState(state) + " (" + status + ")", null);
        }
        if (!unmanaged.isEmpty()) {
            writer.outln(os.section.format("Containers Not Managed by this Admiral Config:"));
            for (String s : unmanaged.keySet()) {
                writer.outln("  " + s);
            }
            if (!managed.isEmpty()) {
                writer.outln("");
            }
        }
        if (!managed.isEmpty()) {
            writer.outln(os.section.format("Containers:"));
            for (String s : managed.keySet()) {
                writer.outln("  " + s);
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
