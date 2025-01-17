package com.optum.admiral.io;

import com.optum.admiral.config.InvalidDependsOnException;
import com.optum.admiral.exception.AdmiralDockerEngineUnreachableException;
import com.optum.admiral.exception.AdmiralContainerNotFoundException;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.exception.AdmiralIOException;
import com.optum.admiral.exception.AdmiralImagePullFailedException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.exception.PortInUseException;
import com.optum.admiral.exception.VolumeMountMismatchException;
import com.optum.admiral.type.exception.InvalidSemanticVersion;
import com.optum.admiral.type.exception.VariableSpecContraint;
import com.optum.admiral.util.MultipleFilesFoundException;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdmiralExceptionContainmentField<R> {

    /**
     * The AdmiralContainedException can only be thrown from the AdmiralExceptionContainmentField.
     */
    public static class AdmiralContainedException extends Exception {
        private List<String> messages = new ArrayList<>();

        private AdmiralContainedException(String... messages) {
            this.messages.addAll(Arrays.asList(messages));
        }

        private AdmiralContainedException(String singleMessage) {
            messages.add(singleMessage);
        }

        private void add(String message) {
            messages.add(message);
        }

        public List<String> getMessages() {
            return messages;
        }

        private AdmiralContainedException(OutputStyler os, MultipleFilesFoundException e) {
            final List<String> filenames = new ArrayList<>(e.files.size());
            for (File instance : e.files) {
                filenames.add(instance.getPath());
            }
            add(os.warning.format("ERROR") + ": Found multiple config files with supported names: " + String.join(", ", filenames));
        }

        private static final String EPF = "Error parsing file: ";

        private AdmiralContainedException(OutputStyler os, InvalidBooleanException e) {
            this(
                os.error.format(EPF) + os.file.format(e.getSource()),
                os.error.format(String.format("Property \"%s\" on or above line %d is boolean but the value \"%s\" is an invalid boolean.",
                    e.getPropertyName(), e.getLine(), e.getInvalidValue()))
            );
        }

        private AdmiralContainedException(OutputStyler os, InvalidEnumException e) {
            this(
                os.error.format(EPF) + os.file.format(e.getSource()),
                os.error.format(String.format("Property \"%s\" on or above line %d with value \"%s\" is invalid.  Valid values are: " +
                    e.getValidValues(), e.getPropertyName(), e.getLine(), e.getInvalidValue()))
            );
        }

        private AdmiralContainedException(OutputStyler os, PropertyNotFoundException e) {
            this(
                os.error.format(EPF) + os.file.format(e.getSource()),
                os.error.format(String.format("Property \"%s\" on or above line %d is not valid.", e.getPropertyName(), e.getLine()))
            );
        }

        private AdmiralContainedException(OutputStyler os, AdmiralConfigurationException e) {
            this(
                os.error.format("Configuration Error: ") + os.file.format(e.getSource()),
                os.error.format(e.getMessage())
            );
        }

        private AdmiralContainedException(OutputStyler os, IOException e) {
            this(
                os.error.format("Unexpected I/O Error"),
                os.error.format(e.getMessage())
            );
        }

        private AdmiralContainedException(OutputStyler os, InvalidSemanticVersion e) {
            this(
                os.error.format("Invalid value for a semantic version: " + e.getValue())
            );
        }

        private AdmiralContainedException(OutputStyler os, InvalidDependsOnException e) {
            for (InvalidDependsOnException.InvalidDependsOn invalidDependsOn : e.getInvalidDependsOn()) {
                final String msg = os.error.format("Error in service: ") + os.service.format(invalidDependsOn.serviceName) +
                    os.error.format(" - depends_on service: ") + os.service.format(invalidDependsOn.dependsOnName) +
                    os.error.format(" but that service does not exist.");
                add(msg);
            }
        }

        private AdmiralContainedException(OutputStyler os, AdmiralFileException e) {
            this(
                os.error.format(e.getContext()),
                os.error.format("Filename: ") + os.file.format(e.getFilename())
            );
        }

        private AdmiralContainedException(OutputStyler os, AdmiralURLException e) {
            this(
                os.error.format(e.getContext()),
                os.error.format("URL: ") + os.file.format(e.getURL())
            );
        }

        private AdmiralContainedException(OutputStyler os, VariableSpecContraint e) {
            this(e.getMessage());
        }

        private AdmiralContainedException(OutputStyler os, ParseException e) {
            this(e.getMessage());
        }

        private AdmiralContainedException(OutputStyler os, InterruptedException e) {
            this(
                os.error.format("Command Stopped by User Request.")
            );
        }

        private AdmiralContainedException(OutputStyler os, AdmiralServiceConfigNotFoundException e) {
            this(
                (e.getServiceName() == null)
                    ? os.error.format("No services defined.")
                    : os.error.format("Service not found: ") + os.service.format(e.getServiceName())
            );
        }

        private AdmiralContainedException(OutputStyler os, PortInUseException e) {
            this(
                os.error.format("Container ") + os.container.format(e.getContainerName()) +
                    os.error.format(" could not do that because port ") + os.errorFocus.format(Integer.toString(e.getPort())) +
                    os.error.format(" is in use."),
                os.help.format("Type \"") + os.command.format("port " + e.getPort()) +
                    os.help.format("\" to find the offending container.")
            );
        }

        private AdmiralContainedException(OutputStyler os, VolumeMountMismatchException e) {
            this(
                os.error.format("Volume mount file/directory mismatch between ") + os.file.format(e.getHostPath()) +
                    os.error.format(" and ") + os.file.format(e.getContainerPath()) +
                    os.error.format(".")
            );
        }

        private AdmiralContainedException(OutputStyler os, AdmiralImagePullFailedException admiralImagePullFailedException) {
            this(
                os.error.format("Image pull failed: ") + os.image.format(admiralImagePullFailedException.getImageName())
            );
        }

        private AdmiralContainedException(OutputStyler os, AdmiralDockerException e) {
            this(
                os.error.format("Admiral Parsed Docker Exception: " + e.getMessage() + "\n" + printStackTraceToString(e))
            );
        }

        private AdmiralContainedException(OutputStyler os, AdmiralIOException e) {
            this(
                os.error.format("Admiral IO Exception: " + e.getCause().getMessage() + "\n" + printStackTraceToString(e.getCause()))
            );
        }

        private static String printStackTraceToString(Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            return sw.toString();
        }

        private AdmiralContainedException(OutputStyler os, AdmiralContainerNotFoundException e) {
            this(
                os.error.format("Container not found: " + e.getContainerName())
            );
        }

        private AdmiralContainedException(OutputStyler os, AdmiralDockerEngineUnreachableException e) {
            this(
                os.error.format("Cannot connect to the Docker Engine."),
                e.isDockerSocketMissing()
                    ? os.error.format("Is Docker running?")
                    : os.errorFocus.format("Is Docker running?"),
                os.help.format("This is almost certainly not a problem with your configuration or Admiral."),
                os.help.format("Most likely this just means Docker is not running."),
                os.help.format("You can verify Docker is running by trying any Docker command, like:"),
                "  " + os.command.format("% docker ps"),
                os.help.format("Less likely, but still possible, the Docker installation might have broken."),
                os.help.format("This happens mostly by the loss of a file called ") + os.file.format("/var/run/docker.sock") + os.help.format(" that Docker installs."),
                os.help.format("It can be restored (on a Mac) by running the command (after first enabling Administrator access): "),
                "  " + os.command.format("% sudo ln -s $HOME/.docker/run/docker.sock /var/run/docker.sock"),
                e.isDockerSocketMissing()
                        ? os.errorFocus.format("It appears that file is missing.  You should run the command above and try Admiral again.")
                        : os.help.format("It appears that file exists, so that might not be the problem."),
                os.help.format("If you know Docker is running and you know ") + os.file.format("/var/run/docker.sock") + os.help.format(" is not broken, your best bet is to try reinstalling Docker.")
            );
        }
    }

    private final OutputStyler os;

    public AdmiralExceptionContainmentField() {
        this.os = new OutputStyler(true);
    }

    public AdmiralExceptionContainmentField(OutputStyler os) {
        this.os = os;
    }

    public void containExecution(AdmiralExecutionEnvironment daee) throws AdmiralContainedException {
        try {
            containDockerExecution(daee);
        } catch (InterruptedException interruptedException) {
            throw new AdmiralContainedException(os, interruptedException);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void containDockerExecution(AdmiralExecutionEnvironment daee)
            throws
                AdmiralContainedException,
                InterruptedException,
                IOException {
        try {
            containAdmiralExecution(daee);
        } catch (AdmiralServiceConfigNotFoundException admiralServiceConfigNotFoundException) {
            throw new AdmiralContainedException(os, admiralServiceConfigNotFoundException);
        } catch (AdmiralImagePullFailedException admiralImagePullFailedException) {
            throw new AdmiralContainedException(os, admiralImagePullFailedException);
        } catch (PortInUseException portInUseException) {
            throw new AdmiralContainedException(os, portInUseException);
        } catch (VolumeMountMismatchException volumeMountMismatchException) {
            throw new AdmiralContainedException(os, volumeMountMismatchException);
        } catch (AdmiralContainerNotFoundException admiralContainerNotFoundException) {
            throw new AdmiralContainedException(os, admiralContainerNotFoundException);
        } catch (AdmiralDockerEngineUnreachableException admiralDockerEngineUnreachableException) {
            throw new AdmiralContainedException(os, admiralDockerEngineUnreachableException);
        } catch (AdmiralIOException admiralIOException) {
            throw new AdmiralContainedException(os, admiralIOException);
        } catch (AdmiralDockerException admiralParsedDockerException) {
            throw new AdmiralContainedException(os, admiralParsedDockerException);
        }
    }
    /**
     * Blame SONAR for this second method
     */
    public void containAdmiralExecution(AdmiralExecutionEnvironment daee)
                throws
                    AdmiralServiceConfigNotFoundException,
                    AdmiralDockerException,
                    AdmiralContainedException,
                    InterruptedException,
                    IOException {
        try {
            daee.execute();
        } catch (AdmiralConfigurationException admiralConfigurationException) {
            throw new AdmiralContainedException(os, admiralConfigurationException);
        } catch (AdmiralFileException admiralFileException) {
            throw new AdmiralContainedException(os, admiralFileException);
        } catch (AdmiralURLException admiralURLException) {
            throw new AdmiralContainedException(os, admiralURLException);
        } catch (InvalidEnumException invalidEnumException) {
            throw new AdmiralContainedException(os, invalidEnumException);
        } catch (InvalidBooleanException invalidBooleanException) {
            throw new AdmiralContainedException(os, invalidBooleanException);
        } catch (InvalidDependsOnException invalidDependsOnException) {
            throw new AdmiralContainedException(os, invalidDependsOnException);
        } catch (InvalidSemanticVersion invalidSemanticVersion) {
            throw new AdmiralContainedException(os, invalidSemanticVersion);
        } catch (MultipleFilesFoundException multipleFilesFoundException) {
            throw new AdmiralContainedException(os, multipleFilesFoundException);
        } catch (PropertyNotFoundException propertyNotFoundException) {
            throw new AdmiralContainedException(os, propertyNotFoundException);
        } catch (VariableSpecContraint variableSpecContraint) {
            throw new AdmiralContainedException(os, variableSpecContraint);
        }
    }

    public R containInitialization(AdmiralInitializationEnvironment<R> dare) throws AdmiralContainedException {
        try {
            return dare.initialize();
        } catch (MultipleFilesFoundException multipleFilesFoundException) {
            throw new AdmiralContainedException(os, multipleFilesFoundException);
        } catch (InvalidEnumException invalidEnumException) {
            throw new AdmiralContainedException(os, invalidEnumException);
        } catch (InvalidBooleanException invalidBooleanException) {
            throw new AdmiralContainedException(os, invalidBooleanException);
        } catch (PropertyNotFoundException propertyNotFoundException) {
            throw new AdmiralContainedException(os, propertyNotFoundException);
        } catch (AdmiralConfigurationException admiralConfigurationException) {
            throw new AdmiralContainedException(os, admiralConfigurationException);
        } catch (IOException ioException) {
            throw new AdmiralContainedException(os, ioException);
        } catch (InvalidSemanticVersion invalidSemanticVersion) {
            throw new AdmiralContainedException(os, invalidSemanticVersion);
        } catch (ParseException parseException) {
            throw new AdmiralContainedException(os, parseException);
        } catch (VariableSpecContraint variableSpecContraint) {
            throw new AdmiralContainedException(os, variableSpecContraint);
        } catch (InvalidDependsOnException invalidDependsOnException) {
            throw new AdmiralContainedException(os, invalidDependsOnException);
        } catch (AdmiralFileException admiralFileException) {
            throw new AdmiralContainedException(os, admiralFileException);
        } catch (AdmiralURLException admiralURLException) {
            throw new AdmiralContainedException(os, admiralURLException);
        }
    }
}
