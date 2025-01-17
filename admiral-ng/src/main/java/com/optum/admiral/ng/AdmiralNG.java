package com.optum.admiral.ng;

import com.optum.admiral.Admiral;
import com.optum.admiral.CleanExit;
import com.optum.admiral.HelpExit;
import com.optum.admiral.booter.AdmiralBootstrapper;
import com.optum.admiral.cli.OptionerProcessor;
import com.optum.admiral.event.SimpleAdmiralEventPublisher;
import com.optum.admiral.gui.ControllableFrame;
import com.optum.admiral.io.AdmiralExceptionContainmentField;
import com.optum.admiral.io.AdmiralExceptionContainmentField.AdmiralContainedException;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collections;
import java.util.prefs.Preferences;

public class AdmiralNG {
    private final SimpleAdmiralEventPublisher simpleAdmiralEventPublisher = new SimpleAdmiralEventPublisher();
    private final Preferences localStorage = Preferences.userNodeForPackage(AdmiralNG.class);

    public static void main(String[] args) {
        try {
            AdmiralNG admiralNG = new AdmiralNG();
            admiralNG.run(args);
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
        final AdmiralNGPreferences.Builder builder = AdmiralNGPreferences.Builder.createBuilder();

        final AdmiralNGPreferences data = new AdmiralExceptionContainmentField<AdmiralNGPreferences>().containInitialization( () -> {
            OptionerProcessor op = new OptionerProcessor(builder, args);

            op.preprocess();

            // Phase 1 - load Yaml from config file (or create defaults)
            final String preferencesFilename = builder.admiralBootOptions.getPreferencesFilename();
            final AdmiralNGPreferences.Yaml yaml = new AdmiralNGPreferences.Loader(simpleAdmiralEventPublisher).loadOrDefault(preferencesFilename);

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

            final Admiral admiral = admiralBootstrapper.boot();

            // Go
            ControllableFrame controllableFrame = makeGUI(data, admiral);
            controllableFrame.showGUI();
        });
    }

    private ControllableFrame makeGUI(AdmiralNGPreferences admiralNGPreferences, Admiral admiral) {
        JPanel contents = new JPanel();
        contents.setLayout(new BorderLayout());

        JPanel services = new JPanel();
        services.setLayout(new BoxLayout(services, BoxLayout.Y_AXIS));
        for(String serviceName : admiral.getServiceNames()) {
            services.add(new JLabel(serviceName));
        }
        contents.add(services, BorderLayout.CENTER);

        JButton up = new JButton("Up");
        up.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    admiral.upACT(Collections.emptyList());
                } catch (AdmiralDockerException | AdmiralServiceConfigNotFoundException | InterruptedException | IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        JButton down = new JButton("Down");
        down.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    admiral.downACT(Collections.emptyList());
                } catch (AdmiralServiceConfigNotFoundException | InterruptedException ex) {
                    ex.printStackTrace();
                } catch (AdmiralDockerException ex) {
                    ex.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        JPanel buttonBar = new JPanel();
        buttonBar.setLayout(new BoxLayout(buttonBar, BoxLayout.X_AXIS));
        buttonBar.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        buttonBar.add(Box.createHorizontalGlue());
        buttonBar.add(up);
        buttonBar.add(Box.createRigidArea(new Dimension(5,0)));
        buttonBar.add(down);
        buttonBar.add(Box.createHorizontalGlue());
        contents.add(buttonBar, BorderLayout.PAGE_END);

        ControllableFrame controllableFrame = new ControllableFrame("DaNG", localStorage, contents);
        return controllableFrame;
    }

}
