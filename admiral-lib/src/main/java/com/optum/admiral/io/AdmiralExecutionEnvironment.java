package com.optum.admiral.io;

import com.optum.admiral.config.InvalidDependsOnException;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.AdmiralServiceConfigNotFoundException;
import com.optum.admiral.type.exception.InvalidSemanticVersion;
import com.optum.admiral.type.exception.VariableSpecContraint;
import com.optum.admiral.util.MultipleFilesFoundException;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;

import java.io.IOException;

public interface AdmiralExecutionEnvironment {

    void execute()
        throws
            AdmiralConfigurationException,
            AdmiralExceptionContainmentField.AdmiralContainedException,
            AdmiralDockerException,
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
            VariableSpecContraint;
}
