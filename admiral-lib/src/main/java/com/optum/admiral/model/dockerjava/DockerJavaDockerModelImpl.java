package com.optum.admiral.model.dockerjava;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;

import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.exception.AdmiralImagePullFailedException;
import com.optum.admiral.model.Container;
import com.optum.admiral.model.Debugger;
import com.optum.admiral.model.DockerModelController;
import com.optum.admiral.model.Image;
import com.optum.admiral.model.ProgressHandler;
import com.optum.admiral.model.REInt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DockerJavaDockerModelImpl {
    final DockerModelController dmc;
    final Debugger debugger;
    DockerClientConfig config;
    DockerHttpClient httpClient;
    DockerClient dockerClient;
    ProgressHandler progressHandler;

    public DockerJavaDockerModelImpl(DockerModelController dmc, Debugger debugger) {
        this.dmc = dmc;
        this.debugger = debugger;
    }

    public void setProgressHandler(ProgressHandler progressHandler) {
        this.progressHandler = progressHandler;
    }

    public void connect() {
        config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    public boolean connectIfNecessary() throws AdmiralDockerException {
        if (!isConnected()) {
            connect();
            return true;
        }
        return false;
    }


    public void disconnect() throws AdmiralDockerException {
        // Guard
        if (dockerClient == null)
            return;

        // Go
        try {
            dockerClient.close();
        } catch (IOException e) {
            throw new AdmiralDockerException(e.getMessage());
        }
    }

    public boolean isConnected() {
        return false;
//        if (dockerClient == null)
//            return false;
//
//        try {
//            ping();
//        } catch (AdmiralDockerException e) {
//            // Eat it and return false
//            return false;
//        }
//        return true;
    }

    public String ping() throws AdmiralDockerException {
        new REInt<Void>().invoke( () -> dockerClient.pingCmd().exec());
        return "pong";
    }

    public Version version() throws AdmiralDockerException {
        return new REInt<Version>().invoke( () -> dockerClient.versionCmd().exec());
    }

    public Info info() throws AdmiralDockerException {
        return new REInt<Info>().invoke( () -> dockerClient.infoCmd().exec());
    }

    public InspectContainerResponse inspectContainer(String containerName) throws AdmiralDockerException {
        return new REInt<InspectContainerResponse>().invoke( () -> dockerClient.inspectContainerCmd(containerName).exec());
    }

    public List<Network> listNetworks(String dockerNetworkName) throws AdmiralDockerException {
        return new REInt<List<Network>>().invoke( () -> dockerClient.listNetworksCmd().withNameFilter(dockerNetworkName).exec());
    }

    public List<Container> listContainers() throws AdmiralDockerException {
        return new REInt<List<Container>>().invoke( () -> {
            List<com.github.dockerjava.api.model.Container> dcc = dockerClient.listContainersCmd().exec();
            List<Container> containers = new ArrayList<>();
            for (com.github.dockerjava.api.model.Container c : dcc) {
                containers.add(new DockerJavaContainer(c));
            }
            return containers;
        }
        );
    }

    public List<Image> listImages() throws AdmiralDockerException {
        return new REInt<List<Image>>().invoke( () -> {
            List<com.github.dockerjava.api.model.Image> dci = dockerClient.listImagesCmd().exec();
            List<Image> images = new ArrayList<>();
            for (com.github.dockerjava.api.model.Image i : dci) {
                images.add(new DockerJavaImage(i));
            }
            return images;
        }
        );
    }

    /**
     * This method blocks and waits for the asynchronous call it makes to the Docker-Java library.
     */
    public void pullImage(String imageName, String platform) throws AdmiralDockerException {
        DockerJavaPullCallback dockerJavaPullCallback = new REInt<DockerJavaPullCallback>().invoke( () -> {
                PullImageCmd pic = dockerClient.pullImageCmd(imageName);
                if (!platform.isEmpty())
                    pic.withPlatform(platform);
                return pic.exec(new DockerJavaPullCallback(Thread.currentThread(), dmc, progressHandler));
            }
        );
        try {
            dockerJavaPullCallback.awaitCompletion();
        } catch (InterruptedException e) {
            throw new AdmiralImagePullFailedException(imageName);
        }
    }

    public CreateNetworkResponse createNetwork(String dockerNetworkName, Map<String, String> labels) throws AdmiralDockerException {
        return new REInt<CreateNetworkResponse>().invoke( () -> dockerClient.createNetworkCmd()
                .withName(dockerNetworkName)
                .withLabels(labels)
                .withAttachable(true)
                .withCheckDuplicate(true)
                .exec());
    }

    public void removeNetwork(String dockerNetworkName) throws AdmiralDockerException {
        new REInt<Void>().invoke( () -> dockerClient.removeNetworkCmd(dockerNetworkName).exec());
    }

    public ResultCallback<Frame> attachToContainer(final String containerName, boolean stderr, ResultCallback<Frame> rc) throws AdmiralDockerException {
        return new REInt<ResultCallback<Frame>>().invoke( () -> dockerClient.attachContainerCmd(containerName)
                .withStdOut(!stderr)
                .withStdErr(stderr)
                .withFollowStream(true)
                .exec(rc));
    }

    public CreateContainerCmd getCreateContainerCmd(String containerName) {
        return dockerClient.createContainerCmd(containerName);
    }

    public CreateContainerResponse createContainer(CreateContainerCmd createContainerCmd) throws AdmiralDockerException {
        return new REInt<CreateContainerResponse>().invoke( () -> createContainerCmd.exec());
    }

    public void removeContainer(String containerName) throws AdmiralDockerException {
        new REInt<Void>().invoke( () -> dockerClient.removeContainerCmd(containerName).exec());
    }

    public void startContainer(String containerName) throws AdmiralDockerException {
        new REInt<Void>().invoke( () -> dockerClient.startContainerCmd(containerName).exec());
    }

    public void stopContainer(String containerName, int timeout) throws AdmiralDockerException {
        new REInt<Void>().invoke( () -> dockerClient.stopContainerCmd(containerName).withTimeout(timeout).exec());
    }

    public void copyToContainer(String containerName, String source, String target) throws AdmiralDockerException {
        new REInt<Void>().invoke( () -> dockerClient.copyArchiveToContainerCmd(containerName).withHostResource(source).withRemotePath(target).exec());
    }

    public void connectToNetwork(ContainerNetwork containerNetwork) throws AdmiralDockerException {
        new REInt<Void>().invoke( () -> dockerClient.connectToNetworkCmd().withContainerNetwork(containerNetwork).exec());
    }

    public ExecCreateCmdResponse execute(String containerName, ResultCallback<Frame> rc, String... cmd) throws AdmiralDockerException {
        ExecCreateCmdResponse execCreateCmdResponse = new REInt<ExecCreateCmdResponse>().invoke(() -> dockerClient.execCreateCmd(containerName)
                .withCmd(cmd)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec());

        new REInt<ResultCallback<Frame>>().invoke(() -> dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(rc));

        return execCreateCmdResponse;
    }

    public long checkExecResponse(ExecCreateCmdResponse execCreateCmdResponse) throws AdmiralDockerException {
        InspectExecResponse inspectExecResponse = new REInt<InspectExecResponse>().invoke( () -> dockerClient.inspectExecCmd(execCreateCmdResponse.getId())
                .exec());

        final Long exitCode=inspectExecResponse.getExitCodeLong();
        return exitCode;
    }
}
