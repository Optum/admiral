package com.optum.admiral.cli;

public abstract class AbstractOptioner implements Optioner {
    private final boolean isPreOption;
    protected AbstractOptioner(boolean isPreOption) {
        this.isPreOption = isPreOption;
    }
    @Override
    public boolean isPreOption() {
        return isPreOption;
    }
}
