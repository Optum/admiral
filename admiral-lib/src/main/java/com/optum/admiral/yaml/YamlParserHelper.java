package com.optum.admiral.yaml;

import com.optum.admiral.ConfigVariableProcessor;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;
import com.optum.admiral.yaml.exception.InvalidEnumException;
import com.optum.admiral.yaml.exception.InvalidBooleanException;
import com.optum.admiral.yaml.exception.PropertyNotFoundException;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YamlParserHelper {

    private final ConfigVariableProcessor configVariableProcessor;

    public YamlParserHelper(ConfigVariableProcessor configVariableProcessor) {
        this.configVariableProcessor = configVariableProcessor;
    }

    public String getS(String original) throws AdmiralConfigurationException {
        return configVariableProcessor.eval(original);
    }

    /**
     * Good old Boolean.parseBoolean takes any value not "true" as false.  We want
     * to be explicit and only allow true/false, so we can't use parseBoolean.
     */
    public Boolean getB(String key, String original) throws AdmiralConfigurationException {
        final String substituted = configVariableProcessor.eval(original);
        Boolean result=null;
        if (substituted != null) {
            if ("true".equalsIgnoreCase(substituted))
                result = true;
            else if ("false".equalsIgnoreCase(substituted))
                result = false;
        }
        if (result==null) {
            String msg = String.format("The \"%s\" YAML value \"%s\" was substituted to \"%s\" but did not produce a valid boolean.", key, original, substituted);
            if (substituted != null && substituted.contains("$")) {
                msg += "  Hint: Based on docker-compose rules, variable substitution in \"environment:\""
                    + " sections supports $VAL and ${VAL} formats, but everywhere else must be ${VAL}.";
            }
            throw new AdmiralConfigurationException(key, msg);
        }
        return result;
    }

    public int getI(String key, String original) throws AdmiralConfigurationException {
        final String substituted = configVariableProcessor.eval(original);
        try {
            return Integer.parseInt(substituted);
        } catch (NumberFormatException e) {
            String msg = String.format("The \"%s\" YAML value \"%s\" was substituted to \"%s\" but did not produce a valid integer.", key, original, substituted);
            if (substituted != null && substituted.contains("$")) {
                msg += "  Hint: Based on docker-compose rules, variable substitution in \"environment:\" sections supports $VAL and ${VAL} formats, but everywhere else must be ${VAL}.";
            }
            throw new AdmiralConfigurationException(key, msg);
        }
    }

    public static AdmiralConfigurationException rebuildYAMLException(String source, YAMLException e)
            throws
            InvalidEnumException,
                InvalidBooleanException,
                PropertyNotFoundException {
        String original = e.getMessage();

        detectUnknownProperty(source, original);

        detectInvalidBoolean(source, original);

        detectInvalidEnum(source, original);

        return new AdmiralConfigurationException(source, original);
    }

    private static void detectUnknownProperty(String source, String original) throws PropertyNotFoundException {
        Pattern pattern = Pattern.compile("Unable to find property '(.+)?'[\\s\\S]+? line (.+)?,");
        Matcher matcher = pattern.matcher(original);
        if (matcher.find()) {
            if (matcher.groupCount()==2) {
                String property = matcher.group(1);
                String lineAsString = matcher.group(2);
                try {
                    int line = Integer.parseInt(lineAsString);
                    throw new PropertyNotFoundException(source, line, property);
                } catch (NumberFormatException nfe) {
                    // Do nothing... we didn't get what we expected.
                }
            }
        }
    }

    private static void detectInvalidBoolean(String source, String original) throws InvalidBooleanException {
        Pattern patterm = Pattern.compile("Cannot create property=(.+)? for [\\s\\S]+?Can not set boolean field [\\s\\S]+? to null value[\\s\\S]+? line (.+)?,[\\s\\S]+?:[\\s\\S]+?\\s*(.+):\\s*(.+)\\n");
        Matcher matcher = patterm.matcher(original);
        if (matcher.find()) {
            if (matcher.groupCount()==4) {
                String property = matcher.group(1);
                String lineAsString = matcher.group(2);
                String propertyAgain = matcher.group(3);
                String invalidValue = matcher.group(4);
                if (property.equals(propertyAgain)) {
                    try {
                        int line = Integer.parseInt(lineAsString);
                        throw new InvalidBooleanException(source, line, property, invalidValue);
                    } catch (NumberFormatException nfe) {
                        // Do nothing... we didn't get what we expected.
                    }
                }
            }
        }
    }

    private static void detectInvalidEnum(String source, String original) throws InvalidEnumException {
        Pattern pattern = Pattern.compile("Cannot create property=(.+)? for [\\s\\S]+?Unable to find enum value [\\s\\S]+? for enum class: ([\\D]+)\\n in 'reader', line (.+)?,[\\s\\S]+?:[\\s\\S]+?\\s*(.+):\\s*(.+)\\n");
        Matcher matcher = pattern.matcher(original);
        if (matcher.find()) {
            if (matcher.groupCount()==5) {
                String className = matcher.group(2);
                String lineAsString = matcher.group(3);
                String property = matcher.group(4);
                String invalidValue = matcher.group(5);
                try {
                    int line = Integer.parseInt(lineAsString);
                    throw new InvalidEnumException(source, className, line, property, invalidValue);
                } catch (NumberFormatException nfe) {
                    // Do nothing... we didn't get what we expected.
                }
            }
        }
    }

    public static void verifyNoneEmpty(List<String> list, File filename, String errorMessage) throws AdmiralConfigurationException {
        for(String item : list) {
            if (item==null || item.isEmpty())
                throw new AdmiralConfigurationException(filename.toString(), errorMessage);
        }
    }

}
