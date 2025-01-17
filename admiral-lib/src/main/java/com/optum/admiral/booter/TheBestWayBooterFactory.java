package com.optum.admiral.booter;

import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.event.AdmiralEventPublisher;

public class TheBestWayBooterFactory implements BooterFactory {

    @Override
    public Booter createBooter(AdmiralOptions admiralOptions, AdmiralEventPublisher admiralEventPublisher) {
        return new TheBestWayBooter(admiralOptions, admiralEventPublisher);
    }

}
