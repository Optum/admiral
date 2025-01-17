package com.optum.admiral.booter;

import com.optum.admiral.Admiral;
import com.optum.admiral.AdmiralBootOptions;
import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.config.InvalidDependsOnException;
import com.optum.admiral.event.SimpleAdmiralEventPublisher;
import com.optum.admiral.io.AdmiralFileException;
import com.optum.admiral.io.AdmiralURLException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.type.exception.InvalidSemanticVersion;
import com.optum.admiral.type.exception.VariableSpecContraint;
import com.optum.admiral.util.MultipleFilesFoundException;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;

import java.io.IOException;

public class AdmiralBootstrapper {
    private final SimpleAdmiralEventPublisher simpleAdmiralEventPublisher;
    private final AdmiralOptions admiralOptions;
    private final AdmiralBootOptions admiralBootOptions;

    public AdmiralBootstrapper(SimpleAdmiralEventPublisher simpleAdmiralEventPublisher,
                               AdmiralOptions admiralOptions,
                               AdmiralBootOptions admiralBootOptions) {
        this.simpleAdmiralEventPublisher = simpleAdmiralEventPublisher;
        this.admiralOptions = admiralOptions;
        this.admiralBootOptions = admiralBootOptions;
    }

    public Admiral boot()
        throws
            AdmiralConfigurationException,
            AdmiralFileException,
            AdmiralServiceConfigNotFoundException,
            AdmiralURLException,
            InvalidBooleanException,
            InvalidDependsOnException,
            InvalidSemanticVersion,
            IOException,
            MultipleFilesFoundException,
            PropertyNotFoundException,
            VariableSpecContraint, InterruptedException, InvalidEnumException {

        final Booter booter = admiralBootOptions.createBooter(admiralOptions, simpleAdmiralEventPublisher);
        booter.boot();

        // Get the boot results.
        return booter.getBootResult();
    }
}
