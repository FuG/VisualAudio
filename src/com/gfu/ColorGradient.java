package com.gfu;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ColorGradient {
    List<Color> colors;
    ListIterator<Color> iterator;

    public static ColorGradient getDefault() {
        return new ColorGradient();
    }

    public ColorGradient() {
        setDefaultRedBlue10();
    }

    private void setDefaultRedBlue10() {
        colors = new ArrayList<Color>();

        // Red -> Blue in 10 steps
        colors.add(Color.decode("#FF0000"));
        colors.add(Color.decode("#E2001C"));
        colors.add(Color.decode("#C60038"));
        colors.add(Color.decode("#AA0055"));
        colors.add(Color.decode("#8D0071"));
        colors.add(Color.decode("#71008D"));
        colors.add(Color.decode("#5500AA"));
        colors.add(Color.decode("#3800C6"));
        colors.add(Color.decode("#1C00E2"));
        colors.add(Color.decode("#0000FF"));

        iterator = colors.listIterator();
    }

    public Color next() {
        return iterator.next();
    }
}
