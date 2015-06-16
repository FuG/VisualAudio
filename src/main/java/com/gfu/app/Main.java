package com.gfu.app;

import java.applet.Applet;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class Main extends Applet implements Runnable {

    public static double FPS = 60;
    public static double millisBetweenFrames = 1000 / FPS;
    public static long loopStartTime = 0;
    public static boolean skipFrameUpdate;

    public static long modulusCounter = 0;
    public static long timeSecond = 0;

    static String theNextEpisode8 = "the_next_episode_8.wav";
    static String theNextEpisode16 = "the_next_episode_16.wav";
    static String theNextEpisode24 = "the_next_episode_24.wav";
    static String lightMyFire8 = "light_my_fire.wav";
    static String truthBeKnown8 = "truth_be_known.wav";
    static String time8 = "time_8.wav";
    static String time16 = "time_16.wav";

    String filePath = "C:\\Users\\Gary\\IdeaProjects\\VisualAudio\\resources\\" + theNextEpisode24;
    File file = null;

    public static int width, height;
    Image backBuffer;
    Graphics backg;

    DSP dsp;
    StreamPlayer streamPlayer;
    Thread playerThread;
    List<double[]> fftResults;
    ListIterator<double[]> resultsIterator;
    List<Double> volumeFactors;
    ListIterator<Double> volumesIterator;

    Grid grid;
    List<AbstractFrameProcessable> bubbleList;
    Thread t = null;

    public static void debug(String s) {
        System.out.println(s);
    }

    public void init() {
        width = getSize().width;
        height = getSize().height;
        backBuffer = createImage(width, height);
        backg = backBuffer.getGraphics();
        backg.setColor(Color.BLACK);

        Graphics2D g2 = (Graphics2D)backg;

        Map<RenderingHints.Key, Object> renderingHintsMap = new HashMap<RenderingHints.Key, Object>();
        renderingHintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        renderingHintsMap.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        renderingHintsMap.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        renderingHintsMap.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        renderingHintsMap.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        renderingHintsMap.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2.setRenderingHints(renderingHintsMap);

        file = new File(filePath);

        long startTime = System.currentTimeMillis();
        grid = new Grid();
        dsp = new DSP(bubbleList, file); // ~140 ms
        System.out.println("DSP Startup: " + (System.currentTimeMillis() - startTime) + "ms");
        fftResults = dsp.normalizedFFTResults;
        volumeFactors = dsp.volumeFactors;
        resultsIterator = fftResults.listIterator();
        volumesIterator = volumeFactors.listIterator();

        try {
            streamPlayer = new StreamPlayer(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setupBubbles();
        skipFrameUpdate = true; // start as true as the flip is at the beginning of process()

        System.out.println("Millis b/w Frames: " + millisBetweenFrames + " ms");
    }

    public void start() {
        if (t == null) {
            t = new Thread(this);
            t.start();
        }

        if (playerThread == null) {
            playerThread = new Thread(streamPlayer);
            playerThread.start();
        }
    }

    public void run() {
        loopStartTime = System.currentTimeMillis();
        double nextFrameTime = loopStartTime;
        try {
            while (true) {
                // TODO: make an fps regulator (responsible for sleeping)
                nextFrameTime += millisBetweenFrames;
                processBubbles(); // 0 - 2 ms
                repaint();

                long timeLeft = (long) (nextFrameTime - System.currentTimeMillis());
//                System.out.println("Process & Repaint Time: " + timeLeft + "ms");
                if (timeLeft > 0) {
                    long sleepTime = timeLeft;
//                    System.out.println("Sleeping: " + sleepTime + " ms");
                    Thread.sleep(sleepTime);
                }
            }
        } catch (InterruptedException e) {
            System.out.println(e);
        }
    }

    public void update(Graphics g) {
        g.drawImage(backBuffer, 0, 0, this);
        getToolkit().sync();
    }

    public void paint(Graphics g) {
        update(g);
    }

    private void setupBubbles() {
        int diameter = 500;
        int shrinkTimeMillis = 2000;

        bubbleList = new ArrayList<AbstractFrameProcessable>();
        ColorGradient colorGradient = new ColorGradient();

        for (int i = 0; i < 10; i++) {
            Bubble bubble = new Bubble(i * 150 + 200, height / 2, diameter, shrinkTimeMillis, colorGradient.next());
            bubble.init();
            bubbleList.add(bubble);
        }
    }

    private void processBubbles() {
        // Fill in black background
        backg.setColor(Color.BLACK);
        backg.fillRect(0, 0, width, height);
        backg.setColor(Color.WHITE);
        backg.drawLine(0, height / 2, width - 1, height / 2);

        grid.draw(backg);

        if (modulusCounter++ % 60 == 0) {
            timeSecond++;
        }

        backg.setColor(Color.WHITE);
        backg.drawString("Elapsed Time: " + timeSecond + " s", 20, 20);

        if (modulusCounter % (60 / dsp.FPS) != 1) { // % == 1 because modulusCounter++ occurred just before
            for (AbstractFrameProcessable bubble : bubbleList) {
                bubble.process();
                bubble.draw(backg);
            }
            return;
        }
        // Process bubbles with fft results and draw them on backbuffer
        double[] frameResults = resultsIterator.next();
        double volumeFactor = volumesIterator.next();
        double sum = sumResult(frameResults);

//        System.out.println("\nFrame Sum: " + sum);

        int index = 0;
        for (AbstractFrameProcessable bubble : bubbleList) {
            double radiusFactor = Math.sqrt(frameResults[index] / sum);
//            System.out.println("Radius Factor(" + index + "): " + radiusFactor);

            ((Bubble) bubble).applyUpdate(radiusFactor, volumeFactor);
            bubble.process();
            bubble.draw(backg);
            index++;
        }
    }

    private double sumResult(double[] input) {
        double sum = 0;

        for (int i = 0; i < input.length; i++) {
            sum += input[i];
        }

        return sum;
    }
}
