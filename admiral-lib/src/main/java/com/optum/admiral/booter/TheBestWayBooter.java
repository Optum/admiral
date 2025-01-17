package com.optum.admiral.booter;

import com.optum.admiral.Admiral;
import com.optum.admiral.AdmiralOptions;
import com.optum.admiral.config.InvalidDependsOnException;
import com.optum.admiral.event.AdmiralEventPublisher;
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

import java.io.File;
import java.io.IOException;

public class TheBestWayBooter implements Booter {
    private final AdmiralOptions admiralOptions;
    private final AdmiralEventPublisher admiralEventPublisher;

    TheBestWayBooter(AdmiralOptions admiralOptions, AdmiralEventPublisher admiralEventPublisher) {
        this.admiralOptions = admiralOptions;
        this.admiralEventPublisher = admiralEventPublisher;
    }

    private Admiral admiral;

    @Override
    public Admiral getBootResult() {
        return admiral;
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
        // Can I find a magic Admiral file?
        final File admiralConfigurationFile = TheAdmiralWayBooter.findAdmiralConfigurationFile();

        // Construct the right booter.
        final Booter booter;
        if (admiralConfigurationFile != null) {
            TheAdmiralWayBooterFactory theAdmiralWayBooterFactory = new TheAdmiralWayBooterFactory(admiralConfigurationFile.getCanonicalPath());
            booter = theAdmiralWayBooterFactory.createBooter(admiralOptions, admiralEventPublisher);
        } else {
            TheDockerComposeWayBooterFactory theDockerComposeWayBooterFactory = new TheDockerComposeWayBooterFactory(null);
            booter = theDockerComposeWayBooterFactory.createBooter(admiralOptions, admiralEventPublisher);
        }

        admiral = booter.boot();
        return admiral;
    }
}
