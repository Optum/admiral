package com.optum.admiral.booter;

import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.event.AdmiralEventPublisher;

import java.util.List;

public class TheDockerComposeWayBooterFactory implements BooterFactory {

    private final List<String> dockerComposeFiles;

    public TheDockerComposeWayBooterFactory(List<String> dockerComposeFiles) {
        this.dockerComposeFiles = dockerComposeFiles;
    }

    @Override
    public Booter createBooter(AdmiralOptions admiralOptions, AdmiralEventPublisher admiralEventPublisher) {
        return new TheDockerComposeWayBooter(admiralOptions, admiralEventPublisher, dockerComposeFiles);
    }
}
