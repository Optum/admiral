package com.optum.admiral.serviceaction;

import com.optum.admiral.Admiral;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.model.DockerModelController;
import com.optum.admiral.type.Commands;

import java.io.IOException;
import java.util.Collection;

public class UpAction extends ServiceAction {

    public UpAction(Admiral admiral, DockerModelController dmc, ComposeConfig composeConfig) {
        super(admiral, dmc, composeConfig);
    }

    @Override
    protected String getActionName() {
        return "up";
    }

    @Override
    protected Commands.Binding getBinding(Commands commands) {
        return commands.up;
    }

    @Override
    protected void doWork(Collection<AdmiralServiceConfig> services, boolean isAll)
        throws
            AdmiralDockerException,
            AdmiralServiceConfigNotFoundException,
            InterruptedException,
            IOException
    {
        dmc.createNetworks(composeConfig.getNetworks());
        dmc.createServicesInDependencyOrder(services);
        dmc.startServicesInDependencyOrder(services);
    }
}
