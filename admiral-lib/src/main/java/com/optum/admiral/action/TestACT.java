package com.optum.admiral.action;

import com.optum.admiral.Admiral;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.exception.AdmiralDockerException;

import java.util.Collection;
import java.util.List;

public class TestACT {
    private final Admiral admiral;
    private final OutputWriter writer;
    private final ComposeConfig composeConfig;

    public TestACT(Admiral admiral, ComposeConfig composeConfig, OutputWriter writer) {
        this.admiral = admiral;
        this.composeConfig = composeConfig;
        this.writer = writer;
    }

    public void perform(List<String> args) throws AdmiralDockerException, InterruptedException {
        if (args.size()>0) {
            final String command = args.get(0);

            if ("allRunning".equals(command)) {
                allRunning();
            } else if ("noneRunning".equals(command)) {
                noneRunning();
            } else if ("someRunning".equals(command)) {
                someRunning();
            } else if ("whichRunning".equals(command)) {
                whichRunning();
            } else {
                writer.outln("test " + command + " not found.");
            }
        } else {
            writer.outln("Currently defined tests:");
            writer.outln("  allRunning");
            writer.outln("  noneRunning");
            writer.outln("  someRunning");
            writer.outln("  whichRunning");
        }
    }

    /**
     * False if anything is not running, otherwise true.
     */
    private void allRunning() throws AdmiralDockerException  {
        Collection<String> runningContainers = admiral.getRunningContainerNameListACT();

        for(AdmiralServiceConfig admiralServiceConfig : composeConfig.getServicesOrEmpty()) {
            final String serviceName = admiralServiceConfig.getName();
            final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
            for (int i = 1; i <= replicas; i++) {
                final String containerName = admiral.calculateContainerName(serviceName, i);
                if (!runningContainers.contains(containerName)) {
                    writer.outln(FALSE);
                    return;
                }
            }
        }
        writer.outln(TRUE);
    }

    /**
     * True if anything is running, otherwise false.
     */
    private void someRunning() throws AdmiralDockerException  {
        Collection<String> runningContainers = admiral.getRunningContainerNameListACT();

        for(AdmiralServiceConfig admiralServiceConfig : composeConfig.getServicesOrEmpty()) {
            final String serviceName = admiralServiceConfig.getName();
            final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
            for (int i = 1; i <= replicas; i++) {
                final String containerName = admiral.calculateContainerName(serviceName, i);
                if (runningContainers.contains(containerName)) {
                    writer.outln(TRUE);
                    return;
                }
            }
        }
        writer.outln(FALSE);
    }

    /**
     * True if nothing is running, otherwise false.
     */
    private void noneRunning() throws AdmiralDockerException  {
        Collection<String> runningContainers = admiral.getRunningContainerNameListACT();

        for(AdmiralServiceConfig admiralServiceConfig : composeConfig.getServicesOrEmpty()) {
            final String serviceName = admiralServiceConfig.getName();
            final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
            for (int i = 1; i <= replicas; i++) {
                final String containerName = admiral.calculateContainerName(serviceName, i);
                if (runningContainers.contains(containerName)) {
                    writer.outln(FALSE);
                    return;
                }
            }
        }
        writer.outln(TRUE);
    }

    private void whichRunning() throws AdmiralDockerException  {
        Collection<String> runningContainers = admiral.getRunningContainerNameListACT();

        for(AdmiralServiceConfig admiralServiceConfig : composeConfig.getServicesOrEmpty()) {
            final String serviceName = admiralServiceConfig.getName();
            final int replicas = admiralServiceConfig.getDeployConfig().getReplicas();
            for (int i = 1; i <= replicas; i++) {
                final String containerName = admiral.calculateContainerName(serviceName, i);
                if (runningContainers.contains(containerName)) {
                    writer.outln(containerName);
                }
            }
        }
    }

    private static final String TRUE = "true";
    private static final String FALSE = "false";
}
