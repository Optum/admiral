package com.optum.admiral.io;

import com.optum.admiral.config.InvalidDependsOnException;
import com.optum.admiral.type.exception.InvalidSemanticVersion;
import com.optum.admiral.type.exception.VariableSpecContraint;
import com.optum.admiral.util.MultipleFilesFoundException;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;
import org.apache.commons.cli.ParseException;

import java.io.IOException;

public interface AdmiralInitializationEnvironment<R> {

    R initialize()
            throws
            AdmiralConfigurationException,
            AdmiralFileException,
            AdmiralURLException,
            InvalidBooleanException,
            InvalidDependsOnException,
            InvalidSemanticVersion,
            IOException,
            MultipleFilesFoundException,
            ParseException,
            PropertyNotFoundException,
            VariableSpecContraint, InvalidEnumException;
}