package com.optum.admiral.action;

import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetworkSettings;
import com.optum.admiral.Admiral;
import com.optum.admiral.AdmiralFormatter;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.io.OutputStyle;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.model.Container;
import com.optum.admiral.model.DockerModelController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PsACT {
    private final Admiral admiral;
    private final DockerModelController dmc;
    private final OutputStyler styler;
    private final OutputWriter writer;

    public PsACT(Admiral admiral, DockerModelController dockerModelController, OutputStyler outputStyler, OutputWriter outputWriter) {
        this.admiral = admiral;
        this.dmc = dockerModelController;
        this.styler = outputStyler;
        this.writer = outputWriter;
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static class BufferedOutput {
        List<String> lines = new ArrayList<>();
        void add(String s) {
            lines.add(s);
        }
    }

    public void perform() throws AdmiralDockerException {
        List<Container> list = admiral.listContainersACT();
        Map<String, BufferedOutput> unmanaged = new TreeMap<>();
        Map<String, BufferedOutput> managed = new TreeMap<>();
        for(Container c : list) {
            final Map<String, BufferedOutput> here;
            String admiralContainerName = admiral.matchAdmiralContainerName(c);
            if (admiralContainerName==null) {
                here = unmanaged;
                admiralContainerName = simpleName(c);
            } else {
                here = managed;
            }
            BufferedOutput bufferedOutput = new BufferedOutput();
            here.put(admiralContainerName, bufferedOutput);
            outContainer(bufferedOutput, admiralContainerName, c);
        }
        if (!unmanaged.isEmpty()) {
            writer.outln(styler.section.format("Containers Not Managed by this Admiral Config:"));
            dumpBufferedOutput(unmanaged.values());
        }
        if (!managed.isEmpty()) {
            writer.outln(styler.section.format("Containers:"));
            dumpBufferedOutput(managed.values());
        }
    }

    private void dumpBufferedOutput(Collection<BufferedOutput> bufferedOutputs) {
        for (BufferedOutput bufferedOutput : bufferedOutputs) {
            for (String s : bufferedOutput.lines) {
                writer.outln("  " + s);
            }
            writer.outln("");
        }
    }

    private void outContainer(BufferedOutput bufferedOutput, String admiralContainerName, Container c) {
        final String state = c.getState();
        final String status = c.getStatus();
        if ("exited".equals(state)) {
            bufferedOutput.add(styler.container.format(admiralContainerName) + ": " + styler.formatState(state) + " (" + status + ")");
            bufferedOutput.add(styler.heading.format("       image: ") + c.getImage());
        } else {
            bufferedOutput.add(styler.container.format(admiralContainerName) + ": " + styler.formatState(state) + " (" + status + ")");
            bufferedOutput.add(styler.heading.format(" containerId: ") + c.getId());
            bufferedOutput.add(styler.heading.format("     created: ") + dateFormat.format(new Date(c.getCreated() * 1000L)));
            bufferedOutput.add(styler.heading.format("       image: ") + c.getImage());
            bufferedOutput.add(styler.heading.format("     imageId: ") + c.getImageId());
            bufferedOutput.add(styler.heading.format("     command: ") + c.getCommand());
            if (c.getPorts()!=null)
                bufferedOutput.add(styler.heading.format("       ports: ") + AdmiralFormatter.prettyPorts(c.getPorts()));
            if (c.getSizeRootFs()!=null)
                bufferedOutput.add(styler.heading.format("  sizeRootFs: ") + c.getSizeRootFs());
            if (c.getSizeRw()!=null)
                bufferedOutput.add(styler.heading.format("      sizeRw: ") + c.getSizeRw());
            if (!c.getLabels().isEmpty()) {
                bufferedOutput.add(styler.heading.format("      labels: "));
                for(Map.Entry<String, String> label : c.getLabels().entrySet()) {
                    bufferedOutput.add(styler.heading.format("              " + label.getKey() + ": " ) + label.getValue());
                }
            }
            outMounts(bufferedOutput, c.getMounts());
            bufferedOutput.add(styler.heading.format("       names: ") + String.join(", ", c.getNames()));
            outNetworks(bufferedOutput, c.getNetworkSettings());
        }

    }
    private static final String indent = "              | ";

    private void outMounts(BufferedOutput bufferedOutput, List<com.github.dockerjava.api.model.ContainerMount> mounts) {
        if (!mounts.isEmpty()) {
            bufferedOutput.add(styler.heading.format("      mounts: "));
            for(com.github.dockerjava.api.model.ContainerMount containerMount : mounts) {
                bufferedOutput.add("              /=============");
                line(bufferedOutput, "       name: ", containerMount.getName(), OutputStyle.unformatted);
                line(bufferedOutput, "     source: ", containerMount.getSource(), styler.file);
                line(bufferedOutput, "destination: ", containerMount.getDestination(), styler.file);
                line(bufferedOutput, "       mode: ", containerMount.getMode(), OutputStyle.unformatted);
                line(bufferedOutput, "     driver: ", containerMount.getDriver(), OutputStyle.unformatted);
                line(bufferedOutput, "         rw: ", nullBoolean(containerMount.getRw()), OutputStyle.unformatted);
                line(bufferedOutput, "propagation: ", containerMount.getPropagation(), OutputStyle.unformatted);
                bufferedOutput.add("              \\-------------");
            }
        }
    }

    private void outNetworks(BufferedOutput bufferedOutput, ContainerNetworkSettings networkSettings) {
        bufferedOutput.add(styler.heading.format("     networks: "));
        for(Map.Entry<String, ContainerNetwork> entry : networkSettings.getNetworks().entrySet()) {
            final String name = entry.getKey();
            final ContainerNetwork attachedNetwork = entry.getValue();
            bufferedOutput.add("              /=============");
            line(bufferedOutput, "       name: ", name, OutputStyle.unformatted);
            line(bufferedOutput, "  networkId: ", attachedNetwork.getNetworkID(), OutputStyle.unformatted);
            line(bufferedOutput, " endpointId: ", attachedNetwork.getEndpointId(), OutputStyle.unformatted);
            line(bufferedOutput, "    gateway: ", attachedNetwork.getGateway(), OutputStyle.unformatted);
            line(bufferedOutput, "  ipAddress: ", attachedNetwork.getIpAddress(), OutputStyle.unformatted);
            line(bufferedOutput, "ipv6Gateway: ", attachedNetwork.getIpV6Gateway(), OutputStyle.unformatted);
            line(bufferedOutput, "ipv6Address: ", attachedNetwork.getGlobalIPv6Address(), OutputStyle.unformatted);
            line(bufferedOutput, " macAddress: ", attachedNetwork.getMacAddress(), OutputStyle.unformatted);
            line(bufferedOutput, "    aliases: ", nullJoin(", ", attachedNetwork.getAliases()), OutputStyle.unformatted);
            line(bufferedOutput, "ipPrefixLen: ", nullToString(attachedNetwork.getIpPrefixLen()), OutputStyle.unformatted);
            bufferedOutput.add("              \\-------------");
        }
    }
    private String nullJoin(CharSequence delimiter, Iterable<? extends CharSequence> values) {
        if (values==null) {
            return null;
        } else {
            return String.join(delimiter, values);
        }
    }
    private String nullToString(Object value) {
        if (value==null) {
            return null;
        } else {
            return value.toString();
        }
    }
    private String nullBoolean(Boolean value) {
        if (value==null) {
            return null;
        } else {
            return Boolean.toString(value);
        }
    }

    private void line(BufferedOutput bufferedOutput, String prompt, String data, OutputStyle outputStyle) {
        if (data!=null && (!data.isEmpty())) {
            bufferedOutput.add(indent + styler.heading.format(prompt) + outputStyle.format(data));
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
