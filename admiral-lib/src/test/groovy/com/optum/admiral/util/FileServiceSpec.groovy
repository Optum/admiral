package com.optum.admiral.util

import spock.lang.Ignore
import spock.lang.Specification

/**
 * This can run locally on a Mac where /tmp/test has been created (which links to /private/tmp/test)
 */
@Ignore
class FileServiceSpec extends Specification {

    FileService fileService = new FileService(new File("/private/tmp/test"));

    def "Relative file processing" () {
        expect:
        fileService.relativeFile(key).getCanonicalFile().toString().equals(value)

        where:
        key | value
        '' | '/private/tmp/test'
        '/usr/local' | '/usr/local'
        'usr/local' | '/private/tmp/test/usr/local'
        '../usr/local' | '/private/tmp/usr/local'
        './usr/local' | '/private/tmp/test/usr/local'
        '../../../../../usr/local' | '/usr/local'
    }

}
