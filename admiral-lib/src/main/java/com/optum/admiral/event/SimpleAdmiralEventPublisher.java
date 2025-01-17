package com.optum.admiral.event;

import com.optum.admiral.io.StyledAdmiralEventListener;
import com.optum.admiral.io.NoBarsProgressMessageRenderer;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.PrintStreamOutputWriter;
import com.optum.admiral.io.ProgressMessageRenderer;
import com.optum.admiral.preferences.OutputPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * No reason for this code to be embedded in another business object.
 *
 * "Does one thing."
 */
public class SimpleAdmiralEventPublisher implements AdmiralEventPublisher {

    public SimpleAdmiralEventPublisher() {
        addDefaultListener();
    }

    private void addDefaultListener() {
        final OutputStyler outputStyler = new OutputStyler(true);
        final ProgressMessageRenderer progressMessageRenderer = new NoBarsProgressMessageRenderer(outputStyler);
        final PrintStreamOutputWriter printStreamOutputWriter = new PrintStreamOutputWriter(System.out, progressMessageRenderer);
        final OutputPreferences outputPreferences = OutputPreferences.getDefaultOutputPreferences();
        final AdmiralEventListener admiralEventListener = new StyledAdmiralEventListener(outputStyler, outputPreferences, printStreamOutputWriter);
        setAdmiralEventListener(admiralEventListener);
    }

    private final List<AdmiralEventListener> admiralEventListenerList = new ArrayList<>();

    public void setAdmiralEventListener(AdmiralEventListener admiralEventListener) {
        // Guard
        if (admiralEventListener == null) {
            throw new IllegalArgumentException("AdmiralEventListener is being set to null.");
        }

        // Go
        admiralEventListenerList.clear();
        admiralEventListenerList.add(admiralEventListener);
    }

    public void removeAdmiralEventListener(AdmiralEventListener admiralEventListener) {
        admiralEventListenerList.remove(admiralEventListener);
    }

    @Override
    public void publish(Consumer<AdmiralEventListener> event) {
        for (AdmiralEventListener admiralEventListener : admiralEventListenerList) {
            event.accept(admiralEventListener);
        }
    }

}
