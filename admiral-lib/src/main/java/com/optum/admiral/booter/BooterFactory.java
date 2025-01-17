package com.optum.admiral.booter;

import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.event.AdmiralEventPublisher;

/**
 * We need a factory pattern because choosing which Booter happens before we have a AdmiralBootOptions.
 */
public interface BooterFactory {
    Booter createBooter(AdmiralOptions admiralOptions, AdmiralEventPublisher admiralEventPublisher);
}
