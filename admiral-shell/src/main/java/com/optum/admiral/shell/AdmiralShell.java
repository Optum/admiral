package com.optum.admiral.shell;

import com.optum.admiral.Admiral;
import com.optum.admiral.CleanExit;
import com.optum.admiral.HelpExit;
import com.optum.admiral.Version;
import com.optum.admiral.booter.AdmiralBootstrapper;
import com.optum.admiral.cli.OptionerProcessor;
import com.optum.admiral.console.Console;
import com.optum.admiral.console.ConsoleOutputWriter;
import com.optum.admiral.console.ConsoleRenderWidthProvider;
import com.optum.admiral.event.AdmiralEventListener;
import com.optum.admiral.event.SimpleAdmiralEventPublisher;
import com.optum.admiral.io.AdmiralExceptionContainmentField;
import com.optum.admiral.io.AdmiralExceptionContainmentField.AdmiralContainedException;
import com.optum.admiral.io.BarsProgressMessageRenderer;
import com.optum.admiral.io.StyledAdmiralEventListener;
import com.optum.admiral.io.NoBarsProgressMessageRenderer;
import com.optum.admiral.io.OutputStyler;
import com.optum.admiral.io.OutputWriter;
import com.optum.admiral.io.ProgressMessageRenderer;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.preferences.OutputPreferences;
import com.optum.admiral.preferences.UXPreferences;
import com.optum.admiral.shell.commandregistry.DigCommandRegistry;
import com.optum.admiral.shell.commandregistry.LegacyCommandRegistry;
import com.optum.admiral.shell.commandregistry.ShowCommandRegistry;
import com.optum.admiral.shell.commandregistry.DASHCommandRegistry;
import com.optum.admiral.shell.commandregistry.DockerEngineCommandRegistry;
import com.optum.admiral.shell.commandregistry.ImageCommandRegistry;
import com.optum.admiral.shell.commandregistry.InternalCommandRegistry;
import com.optum.admiral.shell.commandregistry.ServiceControlCommandRegistry;
import com.optum.admiral.shell.commandregistry.StatusCommandRegistry;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdmiralShell {
    private final SimpleAdmiralEventPublisher simpleAdmiralEventPublisher = new SimpleAdmiralEventPublisher();

    private final Console console;
    private final Terminal terminal;

    private LineReader reader;

    public AdmiralShell() {
        try {
            terminal = TerminalBuilder.builder().system(true).build();
        } catch (IOException e) {
            throw new CleanExit("Unable to create terminal: " + e.getMessage());
        }
        console = new Console(terminal, System.out);
    }

    public static void main(String[] args) {
        // Eliminate the chatty logger.  Logs are for servers, not shells.
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");

        AdmiralShell admiralShell = new AdmiralShell();
        admiralShell.runWithConsole(args);

        // This is the fool-proof way of shutting down all threads and ending.
        System.exit(0);
    }

    public void runWithConsole(String[] args) {
        try {
            run(args);
        } catch (AdmiralContainedException e) {
            for (String message : e.getMessages()) {
                console.outln(message);
            }
        } catch (CleanExit e) {
            console.outln(e.getCleanMessage());
        } catch (HelpExit e) {
            // TODO - use the real help system.  This for now.
            console.outln("For help on commands: run dash, then type help.");
            console.outln("");
            console.outln("Command line options:");
            console.outln("  -a <admiral file>");
            console.outln("  -f <docker-compose file> (multiple allowed)");
            console.outln("  -p <preferences file>");
            console.outln("");
            console.outln("Command line commands:");
            console.outln("  --help --version");
        } catch (Exception e) {
            // For exceptions like NPE that we don't explicitly throw or catch.
            console.outStackTrace(e);
        }
    }

    private UXPreferences materializeUXPreferences(String[] args) throws AdmiralContainedException {
        return new AdmiralExceptionContainmentField<UXPreferences>().containInitialization(() -> {
            final UXPreferences.Builder builder = UXPreferences.Builder.createBuilder("dash");
            OptionerProcessor op = new OptionerProcessor(builder, args);

            // Step 1 - Check for any preprocessed options
            op.preprocess();

            // Step 2 - load Yaml from config file (or create defaults)
            final String preferencesFilename = builder.admiralBootOptions.getPreferencesFilename();
            final UXPreferences.Yaml yaml = new UXPreferences.Loader(simpleAdmiralEventPublisher, "dash").loadOrDefault(preferencesFilename);

            // Step 3 - update Builder with Yaml
            yaml.updateBuilder(builder);

            // Step 4 - override Builder values with command line values
            op.process();

            // Step 5 - convert from Builder to Data
            return builder.getData();
        });
    }

    /**
     *
     * Gathering our preferences data can be considered the first of two parts of running.
     * Since preferences include output style choices, we can't really show messages or errors using the requested
     * output style while we're trying to load and build the output style.
     * So once we are done loading and get "data" - we can proceed with The Rest Of The Show.
     * It also means we should do as little as possible before getting "data" - since that work can't be
     * guarded with output style.
     */
    private UXPreferences runPhaseOne(String [] args) throws AdmiralContainedException {
        return materializeUXPreferences(args);
    }

    /**
     * Now that we have "data" - do the real work of Admiral Shell.
     */
    private void runPhaseTwo(UXPreferences uxPreferences) throws AdmiralContainedException {
        new AdmiralExceptionContainmentField<>(uxPreferences.outputStyler).containExecution(() -> {
            // Step 1 - Use Data to initialize output subsystem
            final ConsoleOutputWriter consoleOutputWriter = initializeShellRuntime(uxPreferences);

            // Step 2 - Used Data to Boot
            final AdmiralBootstrapper admiralBootstrapper = new AdmiralBootstrapper(simpleAdmiralEventPublisher, uxPreferences.admiralOptions, uxPreferences.admiralBootOptions);

            final Admiral admiral = admiralBootstrapper.boot();

            // Step 3 : Load Everything Else
            final SystemRegistry systemRegistry = updateShellRuntime(uxPreferences, admiral, consoleOutputWriter);

            // Welcome
            showWelcome(uxPreferences, admiral, consoleOutputWriter);

            // Go
            commandLoop(uxPreferences, admiral, systemRegistry);
        });
    }

    /**
     * This makes it easier to throw out of this method and catch in run() and log to the console rather
     * than having to catch in main() which doesn't have a console for formatted output.
     */
    private void run(String[] args) throws AdmiralContainedException {
        // Phase One feeds Phase Two
        runPhaseTwo(runPhaseOne(args));
    }

    private ConsoleOutputWriter initializeShellRuntime(UXPreferences uxPreferences) {
        final OutputPreferences outputPreferences = uxPreferences.outputPreferences;
        final OutputStyler outputStyler = uxPreferences.outputStyler;

        // JLine Functionality - Reader
        reader = LineReaderBuilder.builder().terminal(terminal).completer(systemCompleter).build();

        // Show Admiral Event results on the JLine Console.
        final ProgressMessageRenderer progressMessageRenderer;
        if (outputPreferences.showExecuteProgressBars) {
            progressMessageRenderer = new BarsProgressMessageRenderer(outputStyler, new ConsoleRenderWidthProvider(console));
        } else {
            progressMessageRenderer = new NoBarsProgressMessageRenderer(outputStyler);
        }

        final ConsoleOutputWriter consoleOutputWriter = new ConsoleOutputWriter(console, reader, outputPreferences, outputStyler, progressMessageRenderer);

        final AdmiralEventListener admiralEventListener = new StyledAdmiralEventListener(outputStyler, outputPreferences, consoleOutputWriter);

        simpleAdmiralEventPublisher.setAdmiralEventListener(admiralEventListener);

        return consoleOutputWriter;
    }

    final SystemCompleter systemCompleter = new SystemCompleter();

    private SystemRegistry updateShellRuntime(UXPreferences uxPreferences, Admiral admiral, OutputWriter outputWriter) {
        final List<DASHCommandRegistry> dashCommandRegistries = new ArrayList<>();
        final AdmiralShellModelController admiralShellModelController = new AdmiralShellModelController(admiral, uxPreferences, outputWriter, console);

        // Shell Bridge to JLine
        dashCommandRegistries.add(new LegacyCommandRegistry(admiral, admiralShellModelController, uxPreferences, console));
        dashCommandRegistries.add(new DigCommandRegistry(admiral, admiralShellModelController, uxPreferences, console));
        dashCommandRegistries.add(new ShowCommandRegistry(admiral, admiralShellModelController, uxPreferences, console));
        dashCommandRegistries.add(new DockerEngineCommandRegistry(admiral, admiralShellModelController, uxPreferences, console));
        dashCommandRegistries.add(new ImageCommandRegistry(admiral, admiralShellModelController, uxPreferences, console));
        dashCommandRegistries.add(new InternalCommandRegistry(admiral, admiralShellModelController, uxPreferences, console));
        dashCommandRegistries.add(new ServiceControlCommandRegistry(admiral, admiralShellModelController, uxPreferences, console));
        dashCommandRegistries.add(new StatusCommandRegistry(admiral, admiralShellModelController, uxPreferences, console));

        // JLine Functionality - Reader
        StringsCompleter networkCommandsCompleter = new StringsCompleter("create", "rm");
        StringsCompleter contCompleter = new StringsCompleter(admiral.getServiceAndServiceGroupNames());
        ArgumentCompleter serviceArgumentCompleter = new ArgumentCompleter(contCompleter);
        StringsCompleter setCompleter = new StringsCompleter(admiral.getEnvironmentVariableNames());
        ArgumentCompleter variableArgumentCompleter = new ArgumentCompleter(setCompleter);
        ServiceEnvCompleter serviceEnvCompleter = new ServiceEnvCompleter(admiral);
        ArgumentCompleter serviceEnvArgumentCompleter = new ArgumentCompleter(serviceEnvCompleter);
        for(DASHCommandRegistry dashCommandRegistry : dashCommandRegistries) {
            systemCompleter.add(dashCommandRegistry.getCommandsOfType(DASHCommandRegistry.CT.NETWORKCOMMANDS), networkCommandsCompleter);
            systemCompleter.add(dashCommandRegistry.getCommandsOfType(DASHCommandRegistry.CT.SERVICE_ENV), serviceEnvArgumentCompleter);
            systemCompleter.add(dashCommandRegistry.getCommandsOfType(DASHCommandRegistry.CT.SERVICE), serviceArgumentCompleter);
            systemCompleter.add(dashCommandRegistry.getCommandsOfType(DASHCommandRegistry.CT.VARIABLE), variableArgumentCompleter);
            systemCompleter.add(dashCommandRegistry.getCommandsOfType(DASHCommandRegistry.CT.NULL), NullCompleter.INSTANCE);
        }
        systemCompleter.compile();

        // JLine Functionality - SystemRegistry
        final DefaultParser parser = new DefaultParser();
        parser.setEofOnUnclosedBracket(DefaultParser.Bracket.CURLY, DefaultParser.Bracket.ROUND, DefaultParser.Bracket.SQUARE);
        parser.setEofOnUnclosedQuote(true);
        SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, null, null);
        systemRegistry.setCommandRegistries(dashCommandRegistries.toArray(new DASHCommandRegistry[0]));
        return systemRegistry;
    }

    private void showWelcome(UXPreferences uxPreferences, Admiral admiral, OutputWriter outputWriter) {
        final OutputStyler os = uxPreferences.outputStyler;

        outputWriter.outln(os.heading.format("Docker Admiral SHell - " + Version.VERSION));
        outputWriter.outln(os.log.format("Type \"help\" for help"));

        if (uxPreferences.outputPreferences.showInitialState) {
            admiral.showInitialState();
        }
    }

    private void commandLoop(UXPreferences uxPreferences, Admiral admiral, SystemRegistry systemRegistry) {
        final OutputStyler os = uxPreferences.outputStyler;

        final Thread mainThread = Thread.currentThread();
        DefaultParser parser = new DefaultParser();

        try {
            while (true) {
                Terminal.SignalHandler previousIntrHandler = null;
                Terminal.SignalHandler previousWinchHandler = null;

                try {
                    systemRegistry.cleanUp();
                    console.resetProgress();
                    final String line = reader.readLine(os.prompt.format("dash> "));
                    final String cmd = parser.getCommand(line);

                    // Allow "" cmd to happily be ignored.
                    if (!"".equals(cmd)) {
                        if (systemRegistry.hasCommand(cmd)) {
                            previousIntrHandler = terminal.handle(Terminal.Signal.INT, signal -> mainThread.interrupt());
                            previousWinchHandler = terminal.handle(Terminal.Signal.WINCH, this::windowResize);
                            console.updateCurrentWidth();

                            systemRegistry.execute(line);
                        } else {
                            console.outln(os.warning.format("Unknown command: " + cmd));
                        }
                    }
                } catch (UserInterruptException e) {
                    console.outln(os.warning.format("Type \"exit\" to exit"));
                } catch (EndOfFileException e) {
                    // This happens when "exit" is typed.  Do nothing.
                    break;
                } catch (Exception e) {
                    console.outln(os.error.format("Unexpected error from command loop."));
                    console.outln(os.error.format(e.getMessage()));
                    e.printStackTrace();
                } finally {
                    if (previousIntrHandler != null) {
                        terminal.handle(Terminal.Signal.INT, previousIntrHandler);
                    }
                    if (previousWinchHandler != null) {
                        terminal.handle(Terminal.Signal.WINCH, previousWinchHandler);
                    }
                }
            }
        } finally {
            admiral.threadsShutdown();
            try {
                admiral.unjoinServicesACT(Collections.emptyList());
            } catch (AdmiralServiceConfigNotFoundException e) {
                // This gets thrown if there are no services.  No worries - in shutdown.
            } catch (AdmiralDockerException | InterruptedException e) {
                console.outln(os.error.format("Shutdown encountered: " + e.getMessage()));
            }
        }
    }

    private void windowResize(Terminal.Signal signal) {
        console.updateCurrentWidth();
    }
}
