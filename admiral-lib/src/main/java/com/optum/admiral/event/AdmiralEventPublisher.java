package com.optum.admiral.event;

import java.util.function.Consumer;

public interface AdmiralEventPublisher {
    void publish(Consumer<AdmiralEventListener> event);
}
