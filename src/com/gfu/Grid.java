package com.gfu;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class Grid {
    List<Line> gridLines;
    ColorGradient colorGradient;
    String[] labels = new String[] { "32hz", "64hz", "128hz", "256hz", "512hz", "1khz", "2khz", "4khz", "8khz", "16khz" };

    class Line {
        int x;
        Color color;
        String label;

        public Line(int x, Color color, String label) {
            this.x = x;
            this.color = color;
            this.label = label;
        }
    }

    public Grid() {
        gridLines = new ArrayList<Line>();
        colorGradient = ColorGradient.getDefault();

        for (int i = 0; i < 10; i++) {
            gridLines.add(new Line(i * 150 + 200, colorGradient.next(), labels[i]));
        }
    }

    public void draw(Graphics g) {
        ListIterator<Line> gridLineIter = gridLines.listIterator();
        for (int i = 0; i < 10; i++) {
            Line line = gridLineIter.next();

            g.setColor(line.color);
            g.drawLine(line.x, 0, line.x, Main.height);
            g.drawString(line.label, line.x + 8, 60);
        }
    }
}
