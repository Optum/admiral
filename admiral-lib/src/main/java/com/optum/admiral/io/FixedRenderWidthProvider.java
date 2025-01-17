package com.optum.admiral.io;

public class FixedRenderWidthProvider implements RenderWidthProvider {

    private final int width;

    public FixedRenderWidthProvider(int width) {
        this.width = width;
    }

    @Override
    public int getRenderWidth() {
        return width;
    }
}
