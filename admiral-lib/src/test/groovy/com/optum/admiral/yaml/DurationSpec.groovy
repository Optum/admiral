package com.optum.admiral.yaml

import com.optum.admiral.type.Duration;
import spock.lang.Specification;

class DurationSpec extends Specification {

    private static final long MILLISECONDS = 1;
    private static final long SECONDS = 1000;
    private static final long MINUTES = 60 * 1000;
    private static final long HOURS = 60 * 60 * 1000;
    private static final long DAYS = 24 * 60 * 60 * 1000;

    def "Combined" () {
        expect:
        Duration d = new Duration(key)
        d.getMS() == value

        where:
                  key | value
               "15ms" | 15 * MILLISECONDS
                "15s" | 15 * SECONDS
                "15m" | 15 * MINUTES
                "15h" | 15 * HOURS
                "15d" | 15 * DAYS
        "1d2h3m5s7ms" | (1 * DAYS) + (2 * HOURS) + (3 * MINUTES) + (5 * SECONDS) + (7 * MILLISECONDS)
               "2h5s" | (2 * HOURS) + (5 * SECONDS)
              "3m7ms" | (3 * MINUTES) + (7 * MILLISECONDS)
    }

}
