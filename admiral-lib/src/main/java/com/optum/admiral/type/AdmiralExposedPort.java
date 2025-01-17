package com.optum.admiral.type;

/**
 * Admiral version of an exposed port.
 */
public class AdmiralExposedPort {
    public final IPProtocol protocol;
    public final int port;

    public AdmiralExposedPort(final IPProtocol protocol, final int port) {
        this.protocol = protocol;
        this.port = port;
    }
}
