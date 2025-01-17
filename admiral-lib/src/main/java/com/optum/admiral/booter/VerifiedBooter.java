package com.optum.admiral.booter;

import com.optum.admiral.Admiral;
import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.config.AdmiralServiceConfig;
import com.optum.admiral.config.ComposeConfig;
import com.optum.admiral.config.InvalidDependsOnException;
import com.optum.admiral.event.AdmiralEventListener;
import com.optum.admiral.event.AdmiralEventPublisher;
import com.optum.admiral.io.AdmiralFileException;
import com.optum.admiral.io.AdmiralURLException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.model.DockerModelController;
import com.optum.admiral.type.exception.InvalidSemanticVersion;
import com.optum.admiral.type.exception.VariableSpecContraint;
import com.optum.admiral.util.MultipleFilesFoundException;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;

import java.io.IOException;
import java.util.function.Consumer;

public abstract class VerifiedBooter implements Booter  {
    private final AdmiralOptions admiralOptions;
    protected final AdmiralEventPublisher admiralEventPublisher;

    private Admiral admiral;

    public VerifiedBooter(AdmiralOptions admiralOptions, AdmiralEventPublisher admiralEventPublisher) {
        this.admiralOptions = admiralOptions;
        this.admiralEventPublisher = admiralEventPublisher;
    }

    @Override
    public Admiral boot()
        throws
            AdmiralConfigurationException,
            AdmiralFileException,
            AdmiralServiceConfigNotFoundException,
            AdmiralURLException,
            InterruptedException,
            InvalidEnumException,
            InvalidBooleanException,
            InvalidDependsOnException,
            InvalidSemanticVersion,
            IOException,
            MultipleFilesFoundException,
            PropertyNotFoundException,
            VariableSpecContraint {
        admiral = verifyFinalConfiguration(createComposeConfig());
        return admiral;
    }

    protected abstract ComposeConfig createComposeConfig()
            throws AdmiralConfigurationException, InvalidDependsOnException, InvalidSemanticVersion,
            IOException, MultipleFilesFoundException, VariableSpecContraint, AdmiralFileException, AdmiralURLException, InvalidBooleanException, PropertyNotFoundException, InterruptedException, InvalidEnumException;

    private Admiral verifyFinalConfiguration(ComposeConfig composeConfig)
            throws AdmiralConfigurationException, AdmiralServiceConfigNotFoundException, InvalidDependsOnException, VariableSpecContraint {

        composeConfig.verifyNetworks();

        composeConfig.verifyServiceDependsOn();

        for (AdmiralServiceConfig admiralServiceConfig : composeConfig.getServices()) {
            admiralServiceConfig.validateEnvironmentSpecification();
        }

        final DockerModelController dockerModelController = new DockerModelController(admiralEventPublisher, composeConfig, admiralOptions);
        admiral = new Admiral(admiralOptions, dockerModelController, composeConfig);
        return admiral;
    }

    @Override
    public Admiral getBootResult() {
        return admiral;
    }

    protected void publish(Consumer<AdmiralEventListener> event) {
        admiralEventPublisher.publish(event);
    }
}
