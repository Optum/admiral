package com.optum.admiral.booter;

import com.optum.admiral.Admiral;
import com.optum.admiral.config.InvalidDependsOnException;
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

public interface Booter {
    Admiral getBootResult();
    Admiral boot()
            throws
            AdmiralConfigurationException,
            AdmiralFileException,
            AdmiralURLException,
            InvalidBooleanException,
            InvalidDependsOnException,
            InvalidSemanticVersion,
            IOException,
            MultipleFilesFoundException,
            PropertyNotFoundException,
            VariableSpecContraint, InterruptedException, InvalidEnumException, AdmiralServiceConfigNotFoundException;
}
