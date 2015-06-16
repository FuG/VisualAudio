package com.gfu;

import java.awt.*;

public class Bubble extends AbstractFrameProcessable {
    private static double xConstant = 0.20;
    private static double yConstant = 2;

    private int diameterMax, diameterCurrent;
    private long ttlMillis;

    public Bubble(int x, int y, int diameterMax, long ttlMillis, Color color) {
        super(x, y, color);
        this.diameterMax = diameterMax;
        this.diameterCurrent = 0;
        this.ttlMillis = ttlMillis;
    }

    @Override
    public void init() {
        diameterCurrent = 0;
        startTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void process() {
        long timeElapsedMillis = System.currentTimeMillis() - startTimeMillis;

        if (timeElapsedMillis >= ttlMillis) {
            init();
            return;
        }

        double radiusFactor = 1.0 - (timeElapsedMillis * 1.0 / ttlMillis);
        diameterCurrent = (int) (diameterCurrent * radiusFactor);
    }

    public void applyUpdate(double radiusFactor, double volumeFactor) {
        // this smooths out the visuals
        double potentialDiameter = radiusFactor * diameterMax * Math.sqrt(volumeFactor);
        if (potentialDiameter > diameterCurrent) {
            diameterCurrent = (int) potentialDiameter;
            startTimeMillis = System.currentTimeMillis();
        }
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.fillOval((int) (x - 0.5f * diameterCurrent * xConstant), (int) (y - 0.5f * diameterCurrent * yConstant),
                (int) (diameterCurrent * xConstant), (int) (diameterCurrent * yConstant));
    }
}
