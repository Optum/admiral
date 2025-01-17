package com.optum.admiral.type;

import com.optum.admiral.yaml.exception.AdmiralConfigurationException;

public class PortMap {
    private final AdmiralExposedPort admiralExposedPort;
    private final String host;
    private final int published;

    // TODO - don't just save shortSyntax, actually build it from members - will need for long syntax.
    private final String asString;

    public String getHost() {
        return host;
    }

    public AdmiralExposedPort getAdmiralExposedPort() {
        return admiralExposedPort;
    }

    public IPProtocol getProtocol() {
        return admiralExposedPort.protocol;
    }

    public int getPublished() {
        return published;
    }

    public int getTarget() {
        return admiralExposedPort.port;
    }

    public PortMap(String shortSyntax) throws AdmiralConfigurationException {
        final String[] protocolStrip = shortSyntax.split("/");
        IPProtocol protocol;
        if (protocolStrip.length==2) {
            protocol = IPProtocol.valueOf(protocolStrip[1]);
        } else {
            protocol = IPProtocol.tcp;
        }
        final String[] ipHostCont = protocolStrip[0].split(":");

        if (ipHostCont.length==3) {
            this.host = ipHostCont[0];
            this.published = portAsInt("published", ipHostCont[1], true);
            this.admiralExposedPort = new AdmiralExposedPort(protocol, portAsInt("target", ipHostCont[2], false));
        } else if (ipHostCont.length==2) {
            this.host = null;
            this.published = portAsInt("published", ipHostCont[0], true);
            this.admiralExposedPort = new AdmiralExposedPort(protocol, portAsInt("target", ipHostCont[1], false));
        } else if (ipHostCont.length==1) {
            this.host = null;
            this.published = 0;
            this.admiralExposedPort = new AdmiralExposedPort(protocol, portAsInt("target", ipHostCont[0], false));
        } else {
            throw new IllegalArgumentException("Bad short syntax for ports: \"" + shortSyntax + "\"");
        }
        this.asString = shortSyntax;
    }

    // TODO - Parse and bounds error checking
    private int portAsInt(String key, String portAsString, boolean allowZero) throws AdmiralConfigurationException {
        final int port;
        try {
            port = Integer.parseInt(portAsString);
        } catch (NumberFormatException e) {
            String msg = String.format("The port map \"%s\" value of \"%s\" is not a valid integer.", key, portAsString);
            throw new AdmiralConfigurationException(key, msg);
        }
        if (port==0 && (!allowZero)) {
            String msg = String.format("The port map \"%s\" does not allow zero.", key);
            throw new AdmiralConfigurationException(key, msg);
        }
        return port;
    }

    @Override
    public String toString() {
        return "PortMap{" +
                "protocol=" + admiralExposedPort.protocol +
                ", host='" + host + '\'' +
                ", hostport=" + published +
                ", containerport=" + admiralExposedPort.port +
                ", asString='" + asString + '\'' +
                '}';
    }
}
