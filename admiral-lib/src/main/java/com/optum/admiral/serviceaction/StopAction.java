package com.optum.admiral.serviceaction;

import com.optum.admiral.Admiral;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.DockerModelController;
import com.optum.admiral.type.Commands;

import java.util.Collection;

public class StopAction extends ServiceAction {

    public StopAction(Admiral admiral, DockerModelController dmc, ComposeConfig composeConfig) {
        super(admiral, dmc, composeConfig);
    }

    @Override
    protected String getActionName() {
        return "stop";
    }

    @Override
    protected Commands.Binding getBinding(Commands commands) {
        return commands.stop;
    }

    @Override
    protected void doWork(Collection<AdmiralServiceConfig> services, boolean isAll)
            throws
        AdmiralDockerException
    {
        dmc.stopServices(services);
    }
}
