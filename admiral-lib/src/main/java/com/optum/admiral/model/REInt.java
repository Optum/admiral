package com.optum.admiral.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.exception.DockerException;
import com.optum.admiral.exception.AdmiralDockerEngineUnreachableException;
import com.optum.admiral.exception.AdmiralContainerNotFoundException;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.exception.AdmiralIOException;
import com.optum.admiral.exception.AdmiralImageNotFoundException;
import com.optum.admiral.exception.AdmiralInvalidReferenceFormatException;
import com.optum.admiral.exception.AdmiralMountsDeniedException;
import com.optum.admiral.exception.AdmiralNetworkNotFoundException;
import com.optum.admiral.exception.AdmiralNetworkHasActiveEndpointsException;
import com.sun.jna.LastErrorException;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REInt = Runtime Exception Interceptor
 *
 * @param <R>
 */
public class REInt<R> {
    public R invoke(SneakyMethodClass<R> smc) throws AdmiralDockerException {
        try {
            return smc.method();
        } catch (DockerException e) {
            String message = e.getMessage();
            parseForJSON(message);
            throw new AdmiralDockerException(message);
        } catch (RuntimeException e)
        {
            // This library catches IOExceptions and rethrows them as RuntimeExceptions.
            // Try to undo that nonsense here.
            Throwable t = e.getCause();
            if (t!=null) {
                if (t instanceof IOException) {
                    IOException ioException = (IOException) t;
                    Throwable t2 = ioException.getCause();
                    if (t2 instanceof LastErrorException) {
                        File dockerSocket = new File("/var/run/docker.sock");
                        boolean dockerSocketMissing = !dockerSocket.exists();
                        throw new AdmiralDockerEngineUnreachableException(dockerSocketMissing);
                    } else {
                        // General IOException that we don't know anything more specific about.
                        throw new AdmiralIOException(ioException);
                    }
                }
            }
            throw e;
        }
    }

    private void parseForJSON(String fullText) throws AdmiralDockerException {
        final Pattern pattern = Pattern.compile(".*(\\{.+\\})");
        final Matcher matcher = pattern.matcher(fullText);
        if(matcher.find()) {
            final String jsonCandidate = matcher.group(1);
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode json = mapper.readTree(jsonCandidate);
                String message = json.get("message").asText();
                decodeJSONMessage(message);
            } catch (JsonProcessingException e) {
                // Ignore
            }
        } else {
            // This exception isn't in JSON
            detectMountsDenied(fullText);
        }
    }

    private static void decodeJSONMessage(String message) throws AdmiralDockerException {
        detectInvalidReference(message);
        detectMissingContainer(message);
        detectMissingNetwork(message);
        detectMissingImage(message);
        detectNetworkHasActiveEnpoints(message);
    }

    private static void detectInvalidReference(String original) throws AdmiralInvalidReferenceFormatException {
        if ("invalid reference format".equals(original)) {
            throw new AdmiralInvalidReferenceFormatException();
        }
    }

    private static void detectMissingContainer(String original) throws AdmiralContainerNotFoundException {
        // Docker
        {
            Pattern pattern = Pattern.compile("No such container: (\\S+)");
            Matcher matcher = pattern.matcher(original);
            if (matcher.find()) {
                if (matcher.groupCount() == 1) {
                    String containerName = matcher.group(1);
                    throw new AdmiralContainerNotFoundException(containerName);
                }
            }
        }

        // Podman
        {
            Pattern pattern = Pattern.compile("no container with name or ID \"(\\S+)\"");
            Matcher matcher = pattern.matcher(original);
            if (matcher.find()) {
                if (matcher.groupCount() == 1) {
                    String containerName = matcher.group(1);
                    throw new AdmiralContainerNotFoundException(containerName);
                }
            }
        }
    }

    private static void detectMissingNetwork(String original) throws AdmiralNetworkNotFoundException {
        Pattern pattern = Pattern.compile("network (\\S+) not found");
        Matcher matcher = pattern.matcher(original);
        if (matcher.find()) {
            if (matcher.groupCount()==1) {
                String networkName = matcher.group(1);
                throw new AdmiralNetworkNotFoundException(networkName);
            }
        }
    }

    private static void detectMissingImage(String original) throws AdmiralImageNotFoundException {
        Pattern pattern = Pattern.compile("No such image: (\\S+)");
        Matcher matcher = pattern.matcher(original);
        if (matcher.find()) {
            if (matcher.groupCount()==1) {
                String imageName = matcher.group(1);
                throw new AdmiralImageNotFoundException(imageName);
            }
        }

    }

    private static void detectMountsDenied(String original) throws AdmiralMountsDeniedException {
        Pattern pattern = Pattern.compile("^Status 500: Mounts denied: \\nThe path (.+) is not shared from the host and is not known to Docker\\.");
        Matcher matcher = pattern.matcher(original);
        if (matcher.find()) {
            if (matcher.groupCount()==1) {
                String mountName = matcher.group(1);
                throw new AdmiralMountsDeniedException(mountName);
            }
        }
    }

    private static void detectNetworkHasActiveEnpoints(String original) throws AdmiralNetworkHasActiveEndpointsException {
        Pattern pattern = Pattern.compile("^error while removing network: network (.+) id (.+) has active endpoints");
        Matcher matcher = pattern.matcher(original);
        if (matcher.find()) {
            if (matcher.groupCount()==2) {
                String networkName = matcher.group(1);
                String networkId = matcher.group(2);
                throw new AdmiralNetworkHasActiveEndpointsException(networkName, networkId);
            }
        }
    }


}
