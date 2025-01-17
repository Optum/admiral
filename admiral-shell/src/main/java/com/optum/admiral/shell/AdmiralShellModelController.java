package com.optum.admiral.shell;

import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Version;
import com.optum.admiral.Admiral;
import com.optum.admiral.AdmiralFormatter;
import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.console.Console;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.model.Container;
import com.optum.admiral.model.Image;
import com.optum.admiral.preferences.OutputPreferences;
import com.optum.admiral.preferences.UXPreferences;
import org.jline.builtins.Options;
import org.jline.console.CommandInput;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.optum.admiral.shell.AdmiralShellModelController.Units.B;
import static com.optum.admiral.shell.AdmiralShellModelController.Units.GB;
import static com.optum.admiral.shell.AdmiralShellModelController.Units.KB;
import static com.optum.admiral.shell.AdmiralShellModelController.Units.MB;
import static com.optum.admiral.shell.AdmiralShellModelController.Units.TB;

/**
 * This is the Business Logic class for Docker Admiral Shell for things that aren't implemented directly by
 * Docker Admiral.
 *
 * This class *MUST NOT* talk directly to the Docker Java library.  That is the role of Admiral.
 */
public class AdmiralShellModelController {
    private final Admiral admiral;
    private final UXPreferences uxPreferences;
    private final AdmiralOptions admiralOptions;
    private final OutputPreferences preferences;
    private final OutputStyler styler;
    private final OutputWriter writer;
    private final Console console;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public AdmiralShellModelController(Admiral admiral, UXPreferences uxPreferences, OutputWriter outputWriter, Console console) {
        this.admiral = admiral;
        this.uxPreferences = uxPreferences;
        this.admiralOptions = uxPreferences.admiralOptions;;
        this.preferences = uxPreferences.outputPreferences;
        this.styler = uxPreferences.outputStyler;
        this.writer = outputWriter;
        this.console = console;
    }

    public void networkACT(String[] args) throws AdmiralDockerException {
        if (args.length==1) {
            String cmd = args[0];
            if ("create".equals(cmd)) {
                admiral.networkCreateACT();
                return;
            } else if ("rm".equals(cmd)) {
                admiral.networkRmACT();
                return;
            } else {
                console.outln(styler.error.format("Invalid network command argument: " + cmd + "."));
            }
        }
        console.outln(styler.error.format("The network command must specify create or rm."));
    }

    public void clearACT() {
        console.clearScreen();
    }

    public void pingACT() throws AdmiralDockerException {
        String ping = admiral.pingACT();
        writer.outln(styler.command.format("ping") + ": " + ping);
    }

    public void commandsACT() {
        admiral.commandsACT(styler, writer);
    }

    public void configACT() {
        admiral.configACT(writer);
    }

    public void groupsACT() {
        admiral.groupsACT(styler, writer);
    }

    public void showPreferencesACT() {
        admiral.showPreferencesACT(uxPreferences, writer);
    }

    public void envACT(String [] args) {
        if (args.length==0) {
            admiral.envACT(preferences, styler, writer);
        } else if (args.length==1) {
            String serviceName = args[0];
            admiral.envACT(preferences, styler, writer, serviceName);
        } else {
            String serviceName = args[0];
            String [] vars = Arrays.copyOfRange(args, 1, args.length);
            admiral.envACT(preferences, styler, writer, serviceName, vars);
        }
    }

    public void digACT(Collection<String> args, boolean showAll) throws AdmiralServiceConfigNotFoundException {
        admiral.digACT(preferences, styler, writer, args, showAll);
    }

    public void setACT() {
        admiral.setACT(preferences, styler, writer);
    }

    public void setACT(String [] vars) {
        for(String var : vars) {
            admiral.setACT(preferences, styler, writer, var);
        }
    }

    // This is an ugly, brute-force "oh snap it finally works, ship it" implementation.
    // Needs to be better understood, refined, then implemented for all the other commands.
    final String[] composeUSAGE = {
            "showcompose shows compose",
            "Usage: showcompose",
            "  -? --help Displays help"
    };

    static final String[] composeHELP = {
        "Usage: showcompose",
        "",
        "Shows the completed assembly of compose files that represent the current Admiral Configuration.",
        "",
        "Services and Networks are shown, but not the parameters section (\"environment:\") of services, since that section is frequently large and can be distracting when you are wanting to debug the rest of the compose.",
        "",
        "If you want to show the parameters of your services, use the \"showparameters\" command, which shows just service parameters."
    };

    public void showcomposeACT(List<String> args) throws AdmiralServiceConfigNotFoundException {
        Options opt;
        try {
            opt = Options.compile(composeUSAGE).parse(args);
        } catch (IllegalArgumentException e) {
            // JLine throws an IllegalArgumentException if the usage is wrong.
            writer.outln(styler.error.format(e.getMessage()));
            return;
        }

        if (opt.isSet("help")) {
            for (String s  : composeHELP) {
                writer.outln(styler.help.format(s));
            }
            return;
        }

        admiral.showcomposeACT(preferences, styler, writer, args);
    }

    public void showconfigACT() {
        admiral.showconfigACT(preferences, styler, writer);
    }

    public void showparametersACT(List<String> args) throws AdmiralServiceConfigNotFoundException {
        admiral.showparametersACT(preferences, styler, writer, args);
    }

    public void versionACT() throws AdmiralDockerException {
        Version version = admiral.versionACT();
        writer.outln(styler.section.format("DASH:"));
        writer.outln("  " + styler.heading.format("Version") + " = " + com.optum.admiral.Version.VERSION);
        writer.outln("");
        writer.outln(styler.section.format("Docker Engine:"));
        writer.outln("  " + styler.heading.format("API Version") + " = " + version.getApiVersion());
        writer.outln("  " + styler.heading.format("Arch") + " = " + version.getArch());
        writer.outln("  " + styler.heading.format("Build Time") + " = " + version.getBuildTime());
        writer.outln("  " + styler.heading.format("Git Commit") + " = " + version.getGitCommit());
        writer.outln("  " + styler.heading.format("Go Version") + " = " + version.getGoVersion());
        writer.outln("  " + styler.heading.format("Kernel Version") + " = " + version.getKernelVersion());
        writer.outln("  " + styler.heading.format("OS") + " = " + version.getOperatingSystem());
        writer.outln("  " + styler.heading.format("Version") + " = " + version.getVersion());
        writer.outln("");
        writer.outln(styler.section.format("Java:"));
        writer.outln("  " + styler.heading.format("Version") + " = " + System.getProperty("java.version"));
    }

    public void infoACT(CommandInput input) throws AdmiralDockerException {
        Info info = admiral.infoACT();
        writer.outln(styler.section.format("Docker Engine:"));
        writer.outln("  " + styler.heading.format("Containers Running") + " = " + info.getContainersRunning());
        writer.outln("  " + styler.heading.format("Containers Paused") + " = " + info.getContainersPaused());
        writer.outln("  " + styler.heading.format("Containers Stopped") +" = " + info.getContainersStopped());
        writer.outln("  " + styler.heading.format("Containers") + " = " + info.getContainers());
        writer.outln("  " + styler.heading.format("Images") + " = " + info.getImages());
        writer.outln("  " + styler.heading.format("Memory") + " = " + AdmiralFormatter.humanReadableByteCountBin(info.getMemTotal()));
        writer.outln("  " + styler.heading.format("CPUs") + " = " + info.getNCPU());
    }

    public void jvmACT() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Map<String, Thread> sorted = new TreeMap<>();
        for(Thread thread : threadSet) {
            sorted.put(thread.getName(), thread);
        }
        writer.outln(styler.section.format("Threads:"));
        for(Thread thread : sorted.values()) {
            writer.outln("  " + styler.heading.format(thread.getName()) + (thread.isAlive() ? " [Alive]":"") + (thread.isDaemon()?" [Daemon]":"") + (thread.isInterrupted()?" [Interrupted]":""));
        }
        writer.outln("");
        writer.outln(styler.section.format("Runtime:"));
        writer.outln("  " + styler.heading.format("java.version") + " = " + System.getProperty("java.version"));
        writer.outln("  " + styler.heading.format("Free Memory") + " = " + prettyMem(Runtime.getRuntime().freeMemory()));
        writer.outln("  " + styler.heading.format("Total Memory") + " = " + prettyMem(Runtime.getRuntime().totalMemory()));
        writer.outln("  " + styler.heading.format("Max Memory") + " = " + prettyMem(Runtime.getRuntime().maxMemory()));
        writer.outln("  " + styler.heading.format("Available Processors") + " = " + Runtime.getRuntime().availableProcessors());
    }

    public void listImagesACT() throws AdmiralDockerException {
        List<Image> list = admiral.listImagesACT();
        for(Image i : list) {
            writer.outln(styler.image.format(i.getId()) + ": " + styler.log.format("created: " + dateFormat.format(new Date(i.getCreated() * 1000L))));
            for(String tag : i.getRepoTags()) {
                writer.outln(styler.log.format(" tag:" + tag));
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

    enum Units {
        B("B", 1),
        KB("KB", 1000),
        MB("MB", 1000000),
        GB("GB", 1000000000),
        TB("TB", 1000000000000L);

        final String suffix;
        final long divisor;
        Units(String s, long d) {
            suffix = s;
            divisor = d;
        }
        public boolean isSuitable(long value) {
            return ((double)value/(double)divisor)>1.0d;
        }
        public String pretty(long value) {
            return String.format("%.2f",((double)value/(double)divisor)) + suffix;
        }
    }

    private String prettyMem(long size) {
        if (TB.isSuitable(size)) {
            return TB.pretty(size);
        } else if (GB.isSuitable(size)) {
            return GB.pretty(size);
        } else if (MB.isSuitable(size)) {
            return MB.pretty(size);
        } else if (KB.isSuitable(size)) {
            return KB.pretty(size);
        } else {
            return B.pretty(size);
        }
    }

    public void listACT() throws AdmiralDockerException {
        admiral.listACT(styler, writer);
    }

    public void servicesACT() throws AdmiralServiceConfigNotFoundException, AdmiralDockerException {
        admiral.servicesACT(styler, writer);
    }

    public void inspectACT(Collection<String> args) throws AdmiralServiceConfigNotFoundException {
        admiral.inspectACT(preferences, styler, writer, args);
    }

    public void psACT() throws AdmiralDockerException {
        admiral.psACT(styler, writer);
    }

    static class FoundPort implements Comparable<FoundPort> {
        int port;
        String containerName;
        String state;
        String ip;
        String type;
        public FoundPort(int port, String containerName, String state, String ip, String type) {
            this.port = port;
            this.containerName = containerName;
            this.state = state;
            this.ip = ip;
            this.type = type;
        }

        public boolean ipv6() {
            return ip.contains(":");
        }

        /**
         * This just returns the padding, not the port!
         */
        public String portPadding() {
            String p = Integer.toString(port);
            StringBuilder sb = new StringBuilder();
            for(int i=p.length(); i<5; i++) {
                sb.append(" ");
            }
            return sb.toString();
        }

        @Override
        public int compareTo(FoundPort other) {
            if (port!=other.port)
                return port - other.port;

            if (!type.equals(other.type))
                return type.compareTo(other.type);

            if (!ip.equals(other.ip))
                return ip.compareTo(other.ip);

            return containerName.compareTo(other.containerName);
        }

        @Override
        // Not too important for our expected smallish sets.
        // Just has to be consistent with equals. (Can't use fields not in equals).
        public int hashCode() {
            return port + type.hashCode() + ip.hashCode() + containerName.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o==null)
                return false;

            if (!(o instanceof FoundPort))
                return false;

            FoundPort other = (FoundPort) o;
            if (port!=other.port)
                return false;

            if (!type.equals(other.type))
                return false;

            if (!ip.equals(other.ip))
                return false;

            return containerName.equals(other.containerName);
        }
    }

    /**
     * Since we're showing all ports, we display the port first, since that's really what we're wanting to show.
     */
    public void showPortUseACT() throws AdmiralDockerException {
        List<Container> list = admiral.listContainersACT();
        Map<FoundPort, Object> found = new TreeMap<>();
        for(Container c : list) {
            for(com.github.dockerjava.api.model.ContainerPort portMapping : c.getPorts()) {
                if (portMapping.getPublicPort() != null) {
                    found.put(new FoundPort(portMapping.getPublicPort(), simpleName(c), c.getState(), portMapping.getIp(), portMapping.getType()), null);
                }
            }
        }
        if (found.isEmpty()) {
            console.outln(styler.warning.format("No containers found using ports"));
        } else {
            console.outln(styler.section.format("Ports:"));
            for(FoundPort foundPort : found.keySet()) {
                console.outln("  " + foundPort.portPadding() + styler.port.format(foundPort.port + "/"+ foundPort.type ) +
                        " is mapped by " + styler.container.format(foundPort.containerName) + " " + styler.formatState(foundPort.state) +
                        " on " + (foundPort.ipv6() ? "IPV6 " : "IP ") + foundPort.ip );
            }
        }
    }

    /**
     * Since we're only looking at a single port, we display container name first.  (We know the port... duh.)
     */
    public void showPortUseACT(int port) throws AdmiralDockerException {
        if (port < 1) {
            console.outln(styler.error.format(port + " is an invalid port number."));
            return;
        }

        List<Container> list = admiral.listContainersACT();
        Map<FoundPort, Object> found = new TreeMap<>();
        for(Container c : list) {
            for(com.github.dockerjava.api.model.ContainerPort portMapping : c.getPorts()) {
                if (portMapping.getPublicPort() == port) {
                    found.put(new FoundPort (port, simpleName(c), c.getState(), portMapping.getIp(),portMapping.getType()), null);
                }
            }
        }
        if (found.isEmpty()) {
            console.outln(styler.warning.format("No containers found mapping port " + port));
        } else {
            console.outln(styler.section.format("Containers:"));
            for(FoundPort foundPort : found.keySet()) {
                console.outln("  " + styler.container.format(foundPort.containerName) + " " + styler.formatState(foundPort.state) +
                        " is mapping " + styler.port.format(foundPort.port + "/"+ foundPort.type ) + " on " + (foundPort.ipv6() ? "IPV6 " : "IP ") + foundPort.ip);
            }
        }
    }
}
