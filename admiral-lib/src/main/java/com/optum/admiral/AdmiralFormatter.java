package com.optum.admiral;

import com.github.dockerjava.api.model.ContainerPort;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.io.OutputStyler;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.stream.Collectors;

public class AdmiralFormatter {
    private final OutputStyler os;

    public AdmiralFormatter(OutputStyler os){
        this.os = os;
    }

    public String getServiceConciseHeading(AdmiralServiceConfig admiralServiceConfig) {
        final StringBuilder sb = new StringBuilder();
        final String serviceName = admiralServiceConfig.getName();

        sb.append(os.serviceHeading.format(serviceName)+ ":");
        if (admiralServiceConfig.hasServiceGroups()) {
            sb.append(" (");
            sb.append(os.subsection.format("Groups"));
            sb.append(": ");
            sb.append(admiralServiceConfig.getServiceGroups().stream()
                    .map(x -> os.group.format(x)).collect(Collectors.joining(", ")));
            sb.append(")");
        }
        if (admiralServiceConfig.shouldAssume()) {
            sb.append(" (Assume: ");
            sb.append(os.formatAssume(admiralServiceConfig.getAssume()));
            sb.append(")");
        }
        return sb.toString();
    }

    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

    public static String prettyPorts(ContainerPort[] portMappingList) {
        StringBuilder sb = new StringBuilder();
        boolean delimiterNeeded = false;
        for(ContainerPort portMapping : portMappingList) {
            if (delimiterNeeded) {
                sb.append(", ");
            } else {
                delimiterNeeded = true;
            }
            final String ip = portMapping.getIp();
            if (ip!=null) {
                sb.append(ip);
                sb.append(":");
            }
            final Integer hostPort = portMapping.getPublicPort();
            final Integer containerPort = portMapping.getPrivatePort();
            if (hostPort!=null) {
                sb.append(hostPort);
                sb.append("->");
            }
            sb.append(containerPort);
            sb.append("/");
            sb.append(portMapping.getType());
        }
        return sb.toString();
    }

}
