package com.optum.admiral.cli;

import java.util.ArrayList;
import java.util.List;

public class OptionerSourceAgregator implements OptionerSource {
    private final OptionerSource[] optionerSources;

    protected OptionerSourceAgregator(OptionerSource... optionerSources) {
        this.optionerSources = optionerSources;
    }

    @Override
    public Optioner[] getOptioners() {
        List<Optioner> optioners = new ArrayList<>();
        for(OptionerSource optionerSource : optionerSources) {
            for(Optioner optioner : optionerSource.getOptioners()) {
                optioners.add(optioner);
            }
        }
        return optioners.toArray(new Optioner[0]);
    }
}
