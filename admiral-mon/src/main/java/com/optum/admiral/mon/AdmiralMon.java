package com.optum.admiral.mon;

import com.optum.admiral.Admiral;
import com.optum.admiral.CleanExit;
import com.optum.admiral.HelpExit;
import com.optum.admiral.booter.AdmiralBootstrapper;
import com.optum.admiral.cli.OptionerProcessor;
import com.optum.admiral.event.SimpleAdmiralEventPublisher;
import com.optum.admiral.gui.AdmiralMonGUI;
import com.optum.admiral.io.AdmiralExceptionContainmentField;
import com.optum.admiral.io.AdmiralExceptionContainmentField.AdmiralContainedException;

import java.util.Collections;

public class AdmiralMon {
    private final SimpleAdmiralEventPublisher simpleAdmiralEventPublisher = new SimpleAdmiralEventPublisher();

    public static void main(String[] args) {
        // Eliminate the chatty logger.
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");

        try {
            AdmiralMon admiralMon = new AdmiralMon();
            admiralMon.run(args);
        } catch (AdmiralContainedException e) {
            for(String message : e.getMessages()) {
                System.out.println(message);
            }
        } catch (CleanExit e) {
            System.out.println(e.getCleanMessage());
        } catch (HelpExit e) {
            System.out.println("help goes here");
        }
    }

    public void run(String[] args) throws AdmiralContainedException {
        final AdmiralMonPreferences.Builder builder = AdmiralMonPreferences.Builder.createBuilder();

        final AdmiralMonPreferences data =
            new AdmiralExceptionContainmentField<AdmiralMonPreferences>().containInitialization( () -> {

                OptionerProcessor op = new OptionerProcessor(builder, args);

                op.preprocess();

                // Phase 1 - load Yaml from config file (or create defaults)
                final String preferencesFilename = builder.admiralBootOptions.getPreferencesFilename();
                final AdmiralMonPreferences.Yaml yaml = new AdmiralMonPreferences.Loader(simpleAdmiralEventPublisher).loadOrDefault(preferencesFilename);

                // Phase 2 - update Builder with Yaml
                yaml.updateBuilder(builder);

                // Phase 3 - override Builder values with command line values
                op.process();

                // Phase 4 - convert from Builder to Data
                return builder.getData();
            });

        new AdmiralExceptionContainmentField<>(data.outputStyler).containExecution( () -> {
            // Phase 5 - Used Data to Boot
            final AdmiralBootstrapper admiralBootstrapper = new AdmiralBootstrapper(simpleAdmiralEventPublisher, data.admiralOptions, data.admiralBootOptions);

            final AdmiralMonGUI admiralMonGUI = new AdmiralMonGUI(data.monitorPreferences);
            simpleAdmiralEventPublisher.setAdmiralEventListener(admiralMonGUI);

            final Admiral admiral = admiralBootstrapper.boot();

            // Go
            admiral.joinServicesACT(Collections.emptyList());
        });
    }
}
