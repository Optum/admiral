package com.optum.admiral.serviceaction;

import com.optum.admiral.Admiral;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.logging.ActionHarness;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.model.DockerModelController;
import com.optum.admiral.type.Commands;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public abstract class ServiceAction {
    protected final Admiral admiral;
    protected final DockerModelController dmc;
    protected final ComposeConfig composeConfig;

    public ServiceAction(Admiral admiral, DockerModelController dmc, ComposeConfig composeConfig) {
        this.admiral = admiral;
        this.dmc = dmc;
        this.composeConfig = composeConfig;
    }

    public void perform(List<String> serviceNames)
        throws AdmiralDockerException,
            AdmiralServiceConfigNotFoundException,
            InterruptedException,
            IOException
    {
        final ActionHarness actionHarness = new ActionHarness(getActionName(), serviceNames);

        final Collection<AdmiralServiceConfig> services;

        try {
            final boolean isAll;
            if (serviceNames.isEmpty()) {
                services = composeConfig.getServicesCopy();
                services.removeIf(c -> getBinding(c.getCommands()) != Commands.Binding.AUTO);
                isAll = true;
            } else {
                services = expandGroups(composeConfig, serviceNames);
                services.removeIf(c -> getBinding(c.getCommands()) == Commands.Binding.NEVER);
                isAll = false;
            }

            dmc.connectIfNecessary();

            doWork(services, isAll);

        } catch (Exception e) {
            actionHarness.setException(e);
            throw e;
        } finally {
            actionHarness.getTimer().stop();
            dmc.publish(l -> l.resetProgress(actionHarness));
        }
    }

    private List<AdmiralServiceConfig> expandGroups(ComposeConfig composeConfig, List<String> serviceOrGroupNames) throws AdmiralServiceConfigNotFoundException {
        return composeConfig.getGroupEngine().expandGroups(serviceOrGroupNames);
    }

    protected abstract String getActionName();
    protected abstract Commands.Binding getBinding(Commands commands);
    protected abstract void doWork(Collection<AdmiralServiceConfig> services, boolean isAll) throws InterruptedException, AdmiralDockerException, IOException, AdmiralServiceConfigNotFoundException;
}
