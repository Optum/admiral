package com.optum.admiral.console;

import com.optum.admiral.io.RenderWidthProvider;

public class ConsoleRenderWidthProvider implements RenderWidthProvider {
    private final Console console;

    public ConsoleRenderWidthProvider(Console console) {
        this.console = console;
    }

    @Override
    public int getRenderWidth() {
        return console.getWidth();
    }

}
