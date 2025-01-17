package com.optum.admiral.cli.command;

import com.optum.admiral.Admiral;
import com.optum.admiral.booter.AdmiralBootstrapper;
import com.optum.admiral.event.AdmiralEventListener;
import com.optum.admiral.event.SimpleAdmiralEventPublisher;
import com.optum.admiral.io.AdmiralExceptionContainmentField;
import com.optum.admiral.io.AdmiralExceptionContainmentField.AdmiralContainedException;
import com.optum.admiral.io.StyledAdmiralEventListener;
import com.optum.admiral.io.NoBarsProgressMessageRenderer;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.io.PrintStreamOutputWriter;
import com.optum.admiral.io.ProgressMessageRenderer;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.preferences.UXPreferences;

import java.io.IOException;
import java.util.List;

/**
 * This class builds and destroys an Admiral environment for running a single command implemented
 * by the derived class.  The run method of the derived class is executed in an exception containment
 * field.
 */
public abstract class AbstractAdmiralCommand extends AbstractCommand<UXPreferences> {
    protected AbstractAdmiralCommand(String command, String help) {
        super(command, help);
    }

    public abstract void executeCommand(UXPreferences uxPreferences, Admiral admiral, OutputWriter outputWriter, List<String> args)
            throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException;

    @Override
    public void run(UXPreferences uxPreferences, List<String> args) {
        try {
            new AdmiralExceptionContainmentField<>(uxPreferences.outputStyler)
                    .containExecution(() -> {
                        Admiral admiral = null;
                        try {
                            final SimpleAdmiralEventPublisher simpleAdmiralEventPublisher = new SimpleAdmiralEventPublisher();
                            final ProgressMessageRenderer progressMessageRenderer = new NoBarsProgressMessageRenderer(uxPreferences.outputStyler);
                            final PrintStreamOutputWriter printStreamOutputWriter = new PrintStreamOutputWriter(System.out, progressMessageRenderer);
                            final AdmiralEventListener admiralEventListener = new StyledAdmiralEventListener(uxPreferences.outputStyler,
                                    uxPreferences.outputPreferences, printStreamOutputWriter);
                            simpleAdmiralEventPublisher.setAdmiralEventListener(admiralEventListener);

                            final AdmiralBootstrapper admiralBootstrapper = new AdmiralBootstrapper(simpleAdmiralEventPublisher, uxPreferences.admiralOptions, uxPreferences.admiralBootOptions);
                            admiral = admiralBootstrapper.boot();

                            executeCommand(uxPreferences, admiral, printStreamOutputWriter, args);
                        } finally {
                            if (admiral != null) {
                                admiral.disconnectFromDockerEngineACT();
                            }
                        }
                    });
        } catch (AdmiralContainedException e) {
            for (String message : e.getMessages()) {
                System.out.println(message);
            }
        }
    }
}
