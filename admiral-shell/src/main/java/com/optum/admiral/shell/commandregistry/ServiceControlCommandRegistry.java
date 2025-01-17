package com.optum.admiral.shell.commandregistry;

import com.optum.admiral.Admiral;
import com.optum.admiral.console.Console;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.preferences.UXPreferences;
import com.optum.admiral.shell.AdmiralShellModelController;
import org.jline.console.CommandInput;

import java.io.IOException;
import java.util.Arrays;

public class ServiceControlCommandRegistry extends DASHCommandRegistry {
    public ServiceControlCommandRegistry(Admiral admiral, AdmiralShellModelController admiralShellModelController, UXPreferences uxPreferences, Console console) {
        super("Service Control Commands", admiral, admiralShellModelController, uxPreferences, console);

        cmd("bounce",  this::bounceCMD, CT.SERVICE, "Bounce (stop/rm/create/start) services");
        cmd("create",  this::createCMD,  CT.SERVICE, "Create services");
        cmd("down",    this::downCMD,    CT.SERVICE, "Down (stop/rm) services then Remove Network");
        cmd("network", this::networkCMD, CT.NETWORKCOMMANDS, "Create or Rm networks.");
        cmd("restart", this::restartCMD, CT.SERVICE, "Restart (stop/start) services");
        cmd("rm",      this::rmCMD,      CT.SERVICE, "Remove stopped services");
        cmd("start",   this::startCMD,   CT.SERVICE, "Start services");
        cmd("stop",    this::stopCMD,    CT.SERVICE, "Stop services");
        cmd("up",      this::upCMD,      CT.SERVICE, "Create Network then Up (create/start) services");
        cmd("wait",    this::waitCMD,    CT.NULL, "Wait for container healthchecks");

        registerCommands(commandExecute);
    }

    private void bounceCMD(CommandInput input) throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException {
        admiral.bounceACT(Arrays.asList(input.args()));
    }

    private void createCMD(CommandInput input) throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException {
        admiral.createACT(Arrays.asList(input.args()));
    }

    private void downCMD(CommandInput input) throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException {
        admiral.downACT(Arrays.asList(input.args()));
    }

    private void networkCMD(CommandInput input) throws AdmiralDockerException {
        admiralShellModelController.networkACT(input.args());
    }

    private void restartCMD(CommandInput input) throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException {
        admiral.restartACT(Arrays.asList(input.args()));
    }

    private void rmCMD(CommandInput input) throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException {
        admiral.rmACT(Arrays.asList(input.args()));
    }

    private void startCMD(CommandInput input) throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException {
        admiral.startACT(Arrays.asList(input.args()));
    }

    private void stopCMD(CommandInput input) throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException {
        admiral.stopACT(Arrays.asList(input.args()));
    }

    private void upCMD(CommandInput input) throws AdmiralDockerException, AdmiralServiceConfigNotFoundException, InterruptedException, IOException {
        admiral.upACT(Arrays.asList(input.args()));
    }

    private void waitCMD(CommandInput input) throws AdmiralDockerException, AdmiralServiceConfigNotFoundException {
        admiral.waitACT(Arrays.asList(input.args()));
    }

}
