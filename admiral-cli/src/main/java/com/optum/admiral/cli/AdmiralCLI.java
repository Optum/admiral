package com.optum.admiral.cli;

import com.optum.admiral.CleanExit;
import com.optum.admiral.HelpExit;
import com.optum.admiral.cli.command.BounceCMD;
import com.optum.admiral.cli.command.CommandsCMD;
import com.optum.admiral.cli.command.ConfigCMD;
import com.optum.admiral.cli.command.CreateCMD;
import com.optum.admiral.cli.command.DigallCMD;
import com.optum.admiral.cli.command.DownCMD;
import com.optum.admiral.cli.command.EnvCMD;
import com.optum.admiral.cli.command.GroupsCMD;
import com.optum.admiral.cli.command.HelpCMD;
import com.optum.admiral.cli.command.ListCMD;
import com.optum.admiral.cli.command.ShowpreferencesCMD;
import com.optum.admiral.cli.command.PsCMD;
import com.optum.admiral.cli.command.RestartCMD;
import com.optum.admiral.cli.command.RmCMD;
import com.optum.admiral.cli.command.ServicesCMD;
import com.optum.admiral.cli.command.SetCMD;
import com.optum.admiral.cli.command.DigCMD;
import com.optum.admiral.cli.command.DigdeepCMD;
import com.optum.admiral.cli.command.ShowcomposeCMD;
import com.optum.admiral.cli.command.ShowconfigCMD;
import com.optum.admiral.cli.command.ShowparametersCMD;
import com.optum.admiral.cli.command.StartCMD;
import com.optum.admiral.cli.command.StopCMD;
import com.optum.admiral.cli.command.TestCMD;
import com.optum.admiral.cli.command.UpCMD;
import com.optum.admiral.cli.command.VersionCMD;
import com.optum.admiral.event.SimpleAdmiralEventPublisher;
import com.optum.admiral.io.AdmiralExceptionContainmentField;
import com.optum.admiral.io.AdmiralExceptionContainmentField.AdmiralContainedException;
import com.optum.admiral.preferences.UXPreferences;

public class AdmiralCLI {
    private final SimpleAdmiralEventPublisher simpleAdmiralEventPublisher = new SimpleAdmiralEventPublisher();

    private static HelpCMD helpCommand;

    public static void main(String[] args) {
        // Eliminate the chatty logger.
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");

        try {
            AdmiralCLI admiralCLI = new AdmiralCLI();
            admiralCLI.run(args);
        } catch (AdmiralContainedException e) {
            for(String message : e.getMessages()) {
                System.out.println(message);
            }
        } catch (CleanExit e) {
            System.out.println(e.getCleanMessage());
        } catch (HelpExit e) {
            helpCommand.run();
        }
    }

    private void run(String[] args) throws AdmiralContainedException {
        final UXPreferences.Builder builder = UXPreferences.Builder.createBuilder("da");

        final OptionerProcessorAndExecutor<UXPreferences, UXPreferences.Builder> results =
            new AdmiralExceptionContainmentField<OptionerProcessorAndExecutor<UXPreferences, UXPreferences.Builder>>().containInitialization(() -> {

                final OptionerProcessorAndExecutor<UXPreferences, UXPreferences.Builder> opae = configureOptionerProcessor(builder, args);

                // Phase 1 - Check for any preprocessed options
                opae.preprocess();

                // Phase 2 - load Yaml from config file (or create defaults)
                final String preferencesFilename = builder.admiralBootOptions.getPreferencesFilename();
                final UXPreferences.Yaml yaml = new UXPreferences.Loader(simpleAdmiralEventPublisher, "da").loadOrDefault(preferencesFilename);

                // Phase 3 - update Builder with Yaml
                yaml.updateBuilder(builder);

                return opae;
            });

        // Phase 3 - override Builder values with command line values and then Execute Command
        results.processAndExecute(builder);
    }

    private static OptionerProcessorAndExecutor<UXPreferences, UXPreferences.Builder> configureOptionerProcessor(OptionerSource builder, String[] args) {
        OptionerProcessorAndExecutor<UXPreferences, UXPreferences.Builder> op = new OptionerProcessorAndExecutor<>(builder, args);
        helpCommand = new HelpCMD(op);
        op.setDefaultCommand(helpCommand);
        op.addCommand(new BounceCMD());
        op.addCommand(new CommandsCMD());
        op.addCommand(new ConfigCMD());
        op.addCommand(new CreateCMD());
        op.addCommand(new DigallCMD());
        op.addCommand(new DigCMD());
        op.addCommand(new DigdeepCMD());
        op.addCommand(new DownCMD());
        op.addCommand(new EnvCMD());
        op.addCommand(new GroupsCMD());
        op.addCommand(new ListCMD());
        op.addCommand(new PsCMD());
        op.addCommand(new RestartCMD());
        op.addCommand(new RmCMD());
        op.addCommand(new ServicesCMD());
        op.addCommand(new SetCMD());
        op.addCommand(new ShowcomposeCMD());
        op.addCommand(new ShowconfigCMD());
        op.addCommand(new ShowparametersCMD());
        op.addCommand(new ShowpreferencesCMD());
        op.addCommand(new StartCMD());
        op.addCommand(new StopCMD());
        op.addCommand(new TestCMD());
        op.addCommand(new UpCMD());
        op.addCommand(new VersionCMD());
        return op;
    }
}
