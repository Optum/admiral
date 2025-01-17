package com.optum.admiral.booter;

import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.event.AdmiralEventPublisher;

public class TheAdmiralWayBooterFactory implements BooterFactory {

    private final String admiralFilename;

    public TheAdmiralWayBooterFactory(String admiralFilename) {
        this.admiralFilename = admiralFilename;
    }

    @Override
    public Booter createBooter(AdmiralOptions admiralOptions, AdmiralEventPublisher admiralEventPublisher) {
        return new TheAdmiralWayBooter(admiralOptions, admiralEventPublisher, admiralFilename);
    }
}
