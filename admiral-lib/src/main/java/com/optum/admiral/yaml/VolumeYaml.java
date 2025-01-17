package com.optum.admiral.yaml;

import com.optum.admiral.type.Volume;
import com.optum.admiral.type.VolumeLongSyntax;
import com.optum.admiral.type.VolumeShortSyntax;
import com.optum.admiral.util.FileService;
import com.optum.admiral.yaml.exception.AdmiralConfigurationException;

import java.io.File;
import java.io.IOException;

/**
 * Docker behaves differently depending on whether your Volume was created with short syntax or long syntax.
 * So you can't just normalize short into long, because short *is different*.
 *
 * SnakeYaml Parser needs to use this class for both short and long syntax volumes, since they are interchangable
 * in the "volume:" yaml list.
 *
 * If the list entry is a string, SnakeYaml calls the VolumeYaml(String) constructor.
 * If the list entry is a map, SnakeYaml calls the VolumeYaml() constructor, then setter methods on the matching members.
 */
public class VolumeYaml {
    /**
     * We record which format was used
     */
    private final boolean shortSyntax;

    /**
     * These are the Long Syntax YAML fields directly written by SnakeYaml Parser
     */
    public String type;
    public String source;
    public String target;
    public Boolean read_only;

    /**
     * We break apart the Short Syntax YAML single string into these fields.
     */
    private String shortSource;
    private String shortTarget;
    private String shortMode;

    /**
     * Used by SnakeYaml for long form, followed by member setters.
     */
    public VolumeYaml() {
        this.shortSyntax = false;
    }

    private static String DEFAULTSHORTMODE = "rw";

    /**
     * Short form
     */
    public VolumeYaml(String s) {
        this.shortSyntax = true;

        String[] pieces = s.split(":");
        if (pieces.length==1) {
            shortSource=null;
            shortTarget=pieces[0];
            shortMode=DEFAULTSHORTMODE;
        } else if (pieces.length==2) {
            shortSource=pieces[0];
            shortTarget=pieces[1];
            shortMode=DEFAULTSHORTMODE;
        } else if (pieces.length==3) {
            shortSource=pieces[0];
            shortTarget=pieces[1];
            shortMode=pieces[2];
        }
    }

    /**
     * Factory.
     * @return
     */
    public Volume createVolume(FileService fileService, YamlParserHelper yph) throws AdmiralConfigurationException {
        if (shortSyntax) {
            File relativeFile = fileService.relativeFile(yph.getS(shortSource));

            try {
                return new VolumeShortSyntax(relativeFile.getCanonicalPath(),
                        yph.getS(shortTarget),
                        yph.getS(shortMode));
            } catch (IOException e) {
                throw new AdmiralConfigurationException("Volumes:", "Unable to resolve volume source path of " + shortSource);
            }
        } else {
            File relativeFile = fileService.relativeFile(yph.getS(source));
            try {
                return new VolumeLongSyntax(yph.getS(type),
                        relativeFile.getCanonicalPath(),
                        yph.getS(target),
                        read_only);
            } catch (IOException e) {
                throw new AdmiralConfigurationException("Volumes:", "Unable to resolve volume source path of " + source);
            }
        }
    }

    @Override
    public String toString() {
        if (shortSyntax) {
            return "VolumeYamlShortSyntax{" +
                    "type='" + type + '\'' +
                    ", source='" + source + '\'' +
                    ", target='" + target + '\'' +
                    ", read_only='" + read_only + '\'' +
                    '}';
        } else {
            return "VolumeYamlLongSyntax{" +
                    ", shortSource='" + shortSource + '\'' +
                    ", shortTarget='" + shortTarget + '\'' +
                    ", shortMode='" + shortMode + '\'' +
                    '}';
        }
    }
}
