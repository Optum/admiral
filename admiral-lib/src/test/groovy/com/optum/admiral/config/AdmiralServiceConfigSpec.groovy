package com.optum.admiral.config

import com.optum.admiral.ConfigVariableProcessor
import com.optum.admiral.ContainerNamingConvention
import com.optum.admiral.DockerComposeContainerNamingConvention
import com.optum.admiral.util.FileService
import com.optum.admiral.yaml.ComposeYaml
import spock.lang.Specification

class AdmiralServiceConfigSpec extends Specification {

    ComposeYaml composeYaml1 = ComposeYaml.loadFromYamlFile(new File("src/test/resources/testa-1.yaml"))
    ComposeYaml composeYaml2 = ComposeYaml.loadFromYamlFile(new File("src/test/resources/testa-2.yaml"))
    FileService fileService =  new FileService(new File("."));

    def "Set Image" () {
        when:
            ConfigVariableProcessor vp = new ConfigVariableProcessor();
            AdmiralServiceConfig ss = new AdmiralServiceConfig("test");
            ss.applyServiceYaml(fileService, composeYaml1.services.get("test"), vp);

        then:
            ss.getImage() == "dockerimage:1.0.1";

    }

    def "Override Image" () {
        when:
        ConfigVariableProcessor vp = new ConfigVariableProcessor();
        AdmiralServiceConfig ss = new AdmiralServiceConfig("test");
        ss.applyServiceYaml(fileService, composeYaml1.services.get("test"), vp);
        ss.applyServiceYaml(fileService, composeYaml2.services.get("test"), vp);

        then:
        ss.getImage() == "dockerimage:1.0.2";

    }

    def "Environment overrides env_file" () {
        when:
        ConfigVariableProcessor vp = new ConfigVariableProcessor();
        AdmiralServiceConfig ss = new AdmiralServiceConfig("test");
        ss.applyServiceYaml(fileService, composeYaml1.services.get("test"), vp);

        then:
        4 == ss.getEnvironmentVariableCount()

        and:
        value == ss.getEnvironmentVariable(key)

        where:
        key     | value
        "COLOR" | "test1color"
        "NAME"  | "test1name"
        "SHAPE" | "envdata1shape"
        "SIZE"  | "envdata1size"

    }

    def "Second env_file does not override Environment in previous file" () {
        when:
        ConfigVariableProcessor vp = new ConfigVariableProcessor();
        AdmiralServiceConfig ss = new AdmiralServiceConfig("test");
        ss.applyServiceYaml(fileService, composeYaml1.services.get("test"), vp);
        ss.applyServiceYaml(fileService, composeYaml2.services.get("test"), vp);

        then:
        4 == ss.getEnvironmentVariableCount()

        and:
        value == ss.getEnvironmentVariable(key)

        where:
        key     | value
        "COLOR" | "test2color"
        "NAME"  | "test1name"
        "SHAPE" | "envdata2shape"
        "SIZE"  | "test2size"

    }

    def "Env_file from second yaml must not overwrite environment from first yaml." () {
        when:
        String projectName = "project"
        ConfigVariableProcessor vp = new ConfigVariableProcessor();
        AdmiralServiceConfig ss = new AdmiralServiceConfig("test");
        ContainerNamingConvention containerNamingConvention = new DockerComposeContainerNamingConvention(projectName);
        ss.applyServiceYaml(fileService, composeYaml1.services.get("test"), vp);
        ss.applyServiceYaml(fileService, composeYaml2.services.get("test"), vp);
        ComposeConfig cc = new ComposeConfig(null, null, projectName, vp, containerNamingConvention);
        cc.addServiceConfig(ss);

        then:
        AdmiralContainerConfig admiralContainerConfig = cc.getServiceConfig("test");
        admiralContainerConfig.getEnvironmentVariable(key) == value;

        where:
        key | value
        "COLOR" | "test2color"
        "NAME" | "test1name"
        "SHAPE" | "envdata2shape"
        "SIZE" | "test2size"
    }

}
