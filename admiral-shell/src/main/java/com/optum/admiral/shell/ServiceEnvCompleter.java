package com.optum.admiral.shell;

import com.optum.admiral.Admiral;
import com.optum.admiral.ContainerParameterProcessor;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

public class ServiceEnvCompleter implements Completer {
    private final Admiral admiral;

    public ServiceEnvCompleter(Admiral admiral) {
        this.admiral = admiral;
    }

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> candidates) {
        if (parsedLine.wordIndex() == 0) {
            return;
        } else if (parsedLine.wordIndex() == 1) {
            for(String serviceName : admiral.getServiceNames()) {
                candidates.add(new Candidate(serviceName));
            }
        } else {
            String serviceName = parsedLine.words().get(1);
            for(ContainerParameterProcessor.Entry entry : admiral.getEnvironmentVariables(serviceName)) {
                String var = entry.getKey();
                candidates.add(new Candidate(var));
            }
        }
    }
}
