package com.optum.admiral;

import com.optum.admiral.yaml.exception.AdmiralConfigurationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigVariableProcessor {

    private Map<String, Entry> data;
    private List<Pattern> secretPatterns = new ArrayList<>();

    public ConfigVariableProcessor() {
        this.data = new TreeMap<>();
    }

    public void addSecretVariablePattern(String patternAsString) {
        final Pattern pattern = Pattern.compile(patternAsString);
        secretPatterns.add(pattern);
    }

    public boolean isSecret(String key) {
        for(Pattern pattern : secretPatterns) {
            final Matcher matcher = pattern.matcher(key);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    public enum EntrySourceType {
        BUILTIN, SYSTEM, USERPROVIDED, FILE, URL
    }

    public class Entry {
        private final boolean isSecret;
        public final String key;
        public final String value;
        public final String source;
        public final EntrySourceType sourceType;

        private Entry(String key, String value, String source, EntrySourceType sourceType) {
            this.isSecret = isSecret(key);
            this.key = key;
            this.value = value;
            this.source = source;
            this.sourceType = sourceType;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public String getDisplayValue() {
            if (isSecret) {
                return "********";
            } else {
                return value;
            }
        }

        public String getSource() {
            return source;
        }

        public EntrySourceType getEntrySourceType() {
            return sourceType;
        }
    }

    public Map<String, Entry> getData() {
        return data;
    }

    public int size() {
        return data.size();
    }

    public String get(String key) {
        Entry entry = data.get(key);
        if (entry==null)
            return null;

        return entry.getValue();
    }

    /**
     * If systemEnvironmentVariables is null, all environment variables are loaded (the docker compose way)
     * Otherwise the variables to load must be explicitly listed.
     */
    public void initWithEnvironmentVariablesFromSystem(List<String> systemEnvironmentVariables) {
        data.clear();
        for(Map.Entry<String, String> entry : System.getenv().entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (systemEnvironmentVariables==null || systemEnvironmentVariables.contains(key)) {
                data.put(key, new Entry(key, value, "system_environment_variables:", EntrySourceType.SYSTEM));
            }
        }
    }

    public void addEnvironmentVariablesFromURL(URL url) throws IOException, AdmiralConfigurationException {
        String context = url.toString();
        addEnvironmentVariablesFromStream(context, url.openStream(), EntrySourceType.URL);
    }

    public void addEnvironmentVariablesFromFile(File file) throws IOException, AdmiralConfigurationException {
        String context = file.toString();
        try (FileInputStream fis = new FileInputStream(file)) {
            addEnvironmentVariablesFromStream(context, fis, EntrySourceType.FILE);
        }
    }

    public void addEnvironmentVariablesFromStream(String context, InputStream stream, EntrySourceType entrySourceType) throws IOException, AdmiralConfigurationException {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(stream))
        )
        {
            String line;
            while((line = in.readLine()) != null) {
                processEnvLine(context, line, entrySourceType);
            }
        }
    }

    public String eval(String rawValue) throws AdmiralConfigurationException {
        if (rawValue==null)
            return null;

        Pattern pattern = Pattern.compile("([$][{].*?[}])");
        Matcher matcher = pattern.matcher(rawValue);
        StringBuffer result = new StringBuffer();
        while(matcher.find()) {
            String found = matcher.group(1);
            String replacement = processEnvironmentVariableTemplate(found);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public void processEnvLine(String source, String line, EntrySourceType entrySourceType) throws AdmiralConfigurationException {
        final String trimmed = line.trim();
        if (trimmed.startsWith("#") || trimmed.length()==0) {
            return;
        }
        final String[] pieces = trimmed.split("=",2);
        if (pieces.length!=2) {
            throw new IllegalArgumentException("Line is not a valid KEY=VALUE: " + line);
        } else {
            final String key = pieces[0].trim();
            final String value = pieces[1].trim();
            addKeyValueEnvironmentVariable(key, value, source, entrySourceType);
        }
    }

    public void addKeyValueEnvironmentVariable(String key, String value, String source, EntrySourceType entrySourceType) throws AdmiralConfigurationException {
        data.put(key, new Entry(key, eval(value), source, entrySourceType));
    }

    public String processEnvironmentVariableTemplate(String template) throws AdmiralConfigurationException {
        final String rawKey = template.substring(2, template.length()-1);

        boolean foundDefaultSyntax = false;
        String keyWithDefault = null;
        String defaultValue = null;
        Pattern defaultPattern = Pattern.compile("^(.+?):?[-](.+)$");
        Matcher defaultMatcher = defaultPattern.matcher(rawKey);
        if(defaultMatcher.find()) {
            foundDefaultSyntax = true;
            keyWithDefault = defaultMatcher.group(1);
            defaultValue = defaultMatcher.group(2);
        }

        boolean foundErrorSyntax = false;
        String keyWithError = null;
        String errorMessage = null;
        Pattern errorPattern = Pattern.compile("^(.+?):?[?](.+)$");
        Matcher errorMatcher = errorPattern.matcher(rawKey);
        if(errorMatcher.find()) {
            foundErrorSyntax = true;
            keyWithError = errorMatcher.group(1);
            errorMessage = errorMatcher.group(2);
        }

        if (foundDefaultSyntax && foundErrorSyntax) {
            throw new AdmiralConfigurationException("Config", "Value " + rawKey + " contains both default (-) and required syntax (?) but only one is allowed.");
        }

        if (foundDefaultSyntax) {
            return lookupAndReplaceWithDefault(keyWithDefault, defaultValue);
        } else if (foundErrorSyntax) {
            return lookupAndReplaceWithError(keyWithError, errorMessage);
        } else {
            return lookupAndReplace(rawKey);
        }
    }

    public String lookupAndReplace(String key) {
        if (data.containsKey(key)) {
            return data.get(key).getValue();
        } else {
            return "";
        }
    }

    public String lookupAndReplaceWithDefault(String key, String defaultValue) {
        if (data.containsKey(key)) {
            return data.get(key).getValue();
        } else {
            return defaultValue;
        }
    }

    public String lookupAndReplaceWithError(String key, String errorMessage) throws AdmiralConfigurationException {
        if (data.containsKey(key)) {
            String value = data.get(key).getValue();
            if (value.isEmpty()) {
                throw new AdmiralConfigurationException("Config", "The variable " + key +" must not be empty: " + errorMessage);
            }
            return value;
        } else {
            throw new AdmiralConfigurationException("Config", "The variable " + key +" must be defined: " + errorMessage);
        }
    }

}
