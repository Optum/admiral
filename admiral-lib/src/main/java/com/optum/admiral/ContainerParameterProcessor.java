package com.optum.admiral;

import com.optum.admiral.yaml.exception.AdmiralConfigurationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContainerParameterProcessor {
    private final ConfigVariableProcessor configVariableProcessor;
    private final Map<String, Entry> data;

    public ContainerParameterProcessor(ConfigVariableProcessor configVariableProcessor) {
        this.configVariableProcessor = configVariableProcessor;
        this.data = new TreeMap<>();
    }

    public class Entry {
        private final boolean isSecret;
        public final String key;
        public final String value;
        public final String source;
        public final String extraValue;
        public final String extraSource;

        public Entry(String key, String value, String source) {
            this.isSecret = configVariableProcessor.isSecret(key);
            this.key = key;
            this.value = value;
            this.source = source;
            this.extraValue = null;
            this.extraSource = null;
        }

        public Entry(String key, String value, String source, String extraValue, String extraSource) {
            this.isSecret = configVariableProcessor.isSecret(key);
            this.key = key;
            this.value = value;
            this.source = source;
            this.extraValue = extraValue;
            this.extraSource = extraSource;
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

        public boolean hasExtra() {
            return extraValue != null;
        }

        public String getExtraValue() {
            return extraValue;
        }

        public String getDisplayExtraValue() {
            if (isSecret) {
                return "********";
            } else {
                return extraValue;
            }
        }

        public String getExtraSource() {
            return extraSource;
        }
    }

    public void clear() {
        data.clear();
    }

    public void addContainerEnvironmentVariablesFromFileNamed(File file) throws IOException, AdmiralConfigurationException {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)))
        ) {
            String line;
            while((line = in.readLine()) != null) {
                processFileLine(file, line);
            }
        }
    }

    private void processFileLine(File filename, String line) throws AdmiralConfigurationException {
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
            addKeyValueEnvironmentVariable(key, value, "File: " + filename);
        }
    }

    public void addEnvironmentVariable(String key, String value, String source) throws AdmiralConfigurationException {
        data.put(key, new Entry(key, eval(value), source));
    }

    public Map<String, Entry> getData() {
        return data;
    }

    public int size() {
        return data.size();
    }

    public String get(String key) {
        return data.get(key).getValue();
    }

    private void addKeyValueEnvironmentVariable(String key, String value, String source) throws AdmiralConfigurationException {
        data.put(key, new Entry(key, eval(value), source));
    }

    private String eval(String rawValue) throws AdmiralConfigurationException {
        if (rawValue==null)
            return null;

        Pattern pattern = Pattern.compile("([$][$])|([$][{].*?[}])|([$]\\w+)");
        Matcher matcher = pattern.matcher(rawValue);
        StringBuffer result = new StringBuffer();
        while(matcher.find()) {
            for(int i=1;i<=matcher.groupCount();i++) {
                String found = matcher.group(i);
                if (found!=null) {
                    String rep = processTemplate(found);
                    if (rep != null) {
                        matcher.appendReplacement(result, Matcher.quoteReplacement(rep));
                    }
                }
            }
        }
        matcher.appendTail(result);
        return result.toString();

    }

    private String processTemplate(String template) throws AdmiralConfigurationException {
        if (template==null)
            return null;
        if ("$$".equals(template)) {
            return "$";
        }
        if (template.startsWith("${")) {
            return configVariableProcessor.processEnvironmentVariableTemplate(template);
        } else {
            String piece = template.substring(1, template.length());
            return configVariableProcessor.lookupAndReplace(piece);
        }
    }
}
