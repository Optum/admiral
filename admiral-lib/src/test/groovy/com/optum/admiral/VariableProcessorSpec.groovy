package com.optum.admiral

import spock.lang.Ignore
import spock.lang.Specification

@Ignore
class VariableProcessorSpec extends Specification {
    static ConfigVariableProcessor vp1 = new ConfigVariableProcessor()
    static String systemUserName
    static int systemCount

    static {
        vp1.initWithEnvironmentVariablesFromSystem()
        systemUserName = vp1.get("USER")
        systemCount = vp1.size()
        vp1.addEnvironmentVariablesFromFile(new File("src/test/resources/spec.env"))
    }

    /**
     * Case 1) Unknown references resolve to blank
     * Case 2) System references resolve
     * Case 3) We can override a system value
     * Case 4) We can reference an overridden system value
     * Case 5) Allow VALUE to have = characters.
     * Case 6) We ignore $VALUE substition, which is valid for docker-config.yaml variables, but not environment variables.
     *
     * FIRST is set to a forward reference that is not found.  It must be blank.
     * SECOND is set to a reference to the system variable USER, which should be what we found and saved in systemUserName above
     * USER is set to the constant "notme" overriding what was set in the system
     * COMPLEX value has an equal character that must be preserved in the value.
     * OTHER is set to a reference to the system variable USER, which has been overridden to "notme".
     *
     * Additionally this tests:
     * 1) skip comments
     * 2) trim whitespace all around the three tokens of {KEY,=,VALUE}
     * 3) blank lines are skipped
     * 4) that we only added four variables - one was an override, and none of the blank lines were accidentally processed
     */
    def ".env file processing" () {
        expect:
        vp1.get(key) == value
        vp1.size() == systemCount + 5

        where:
        key | value
        'FIRST' | ''
        'SECOND' | systemUserName
        'USER' | 'notme'
        'OTHER' | 'notme'
        'COMPLEX' | 'encoding=false'
        'LAST' | '$USER'
    }

}
