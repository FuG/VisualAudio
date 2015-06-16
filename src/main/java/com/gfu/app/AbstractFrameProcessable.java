package com.gfu.app;

import java.awt.*;

public abstract class AbstractFrameProcessable {
    protected int x, y;
    protected Color color;

    protected long startTimeMillis;
    protected boolean skipFrame;

    public AbstractFrameProcessable(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;

        startTimeMillis = System.currentTimeMillis();
        skipFrame = false;
    }

    public boolean getSkipFrame() {
        return skipFrame;
    }

    public boolean toggleSkipFrame() {
        skipFrame = (skipFrame == true) ? false : true;
        return skipFrame;
    }

    public abstract void init();

    public abstract void process();

    public abstract void draw(Graphics g);
}
