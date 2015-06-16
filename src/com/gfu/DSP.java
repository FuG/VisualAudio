package com.gfu;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DSP {
    public int FPS = 10;
    private int BYTES_PER_PERIOD = 4410;
    private int SAMPLES_PER_PERIOD = 4096;
    public int bandCount = 10;

    public int[] bandCenters = { 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384};

    private List<AbstractFrameProcessable> bubbleList;

    private File file;

    private StreamInfo streamInfo;

    private double[] audioData;

    private List<double[]> sampleSegments;
    public List<double[]> normalizedFFTResults;
    public List<Double> volumeFactors;

    public DSP(List<AbstractFrameProcessable> bubbleList, File file) {
        this.bubbleList = bubbleList;
        sampleSegments = new ArrayList<double[]>();

        try {
            this.file = file;

            streamInfo = StreamInfo.getStreamInfo(file);
            audioData = decodeStreamToDoubleArray();
            streamInfo.reset();

            BYTES_PER_PERIOD = streamInfo.bytesPerSecond / FPS;

            process();
//            printAudioDataToFile();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void process() {
        BYTES_PER_PERIOD = streamInfo.bytesPerSecond / FPS;
        int totalFullFrames = audioData.length / BYTES_PER_PERIOD;
//        int lastFrameSize = audioData.length % BYTES_PER_PERIOD;
//
//        double totalMillis = audioData.length * 1.0 / streamInfo.bytesPerSecond * 1000;

        // Gather samples from all periods
        double smallOffset = BYTES_PER_PERIOD * 1.0 / SAMPLES_PER_PERIOD;
        for (int i = 0; i < totalFullFrames; i++) {
            double[] segmentSample = new double[SAMPLES_PER_PERIOD];
            int largeOffset = i * BYTES_PER_PERIOD;

            for (int j = 0; j < SAMPLES_PER_PERIOD; j++) {
                 segmentSample[j] = audioData[largeOffset + (int)(smallOffset * j)];
            }

            sampleSegments.add(segmentSample);
        }

        // Run transform on all samples and create list of normalizedFFTResults
        normalizedFFTResults = new ArrayList<double[]>();
        volumeFactors = new ArrayList<Double>();
        for (double[] ds: sampleSegments) {
            double[] fftResult = transform(ds);

            double[] normalizedResult = new double[bandCount];

            for (int k = 0; k < normalizedResult.length; k++) {
                int bandCenter = bandCenters[k] / FPS / 2; // only working with first half of fft result
                int minusDelta = bandCenter;
                int plusDelta;

                // minusDelta: how far back from the center
                if (k != 0) {
                    minusDelta = (bandCenter - bandCenters[k-1] / FPS / 2) / 2; // half way between
                }

                // plusDelta: how far forward from the center
                if (k != normalizedResult.length - 1) {
                    plusDelta = (bandCenters[k+1] / FPS - bandCenter) / 4;
                } else {
                    plusDelta = SAMPLES_PER_PERIOD / 2 - bandCenter;
                }

                // gather summation for selected freq. band center
                int sum = 0;
                for (int m = bandCenter - minusDelta; m < bandCenter + plusDelta; m++) {
                    sum += fftResult[m];
                }

                normalizedResult[k] = sum / Math.sqrt(bandCenters[k] / 10.0);
            }

            Main.debug("\nNormalized:");
            for (int n = 0; n < bandCount; n++) {
                Main.debug(bandCenters[n] + "Hz: " + (int) normalizedResult[n]);
            }

            normalizedFFTResults.add(normalizedResult);

            // Find a volume measure
            volumeFactors.add(volumeFactor(ds));
        }
    }

    private double volumeFactor(double[] sample) {
        double sum = 0;

        for (double d : sample) {
            if (d > 0) {
                sum += d;
            }
        }
        double volumeFactor = sum / sample.length / 32768.0 / 4;

        Main.debug("Volume Factor: " + volumeFactor);

        return volumeFactor;
    }

    private double[] decodeStreamToDoubleArray() throws IOException {
        AudioInputStream din = streamInfo.decodedInputStream;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[4096];

        int nBytesRead = 0;
        while (nBytesRead != -1) {
            nBytesRead = din.read(data, 0, data.length);
            if (nBytesRead != -1) {
                out.write(data, 0, nBytesRead);
            }
        }

        byte[] bytes = out.toByteArray();
        double[] doubleArray = new double[bytes.length / 2];

        for (int i = 0; i < bytes.length / 2; i++) {
//            byte[] temp = new byte[8];
//            temp[0] = bytes[i*2];
//            temp[1] = bytes[i*2+1];
//
//
//            ByteBuffer.wrap(temp).getDouble();
//
//            doubleArray[i] = ByteBuffer.wrap(temp).getDouble();
            int val = ((bytes[i*2] & 0xff) << 8) | (bytes[i*2+1] & 0xff);
            doubleArray[i] = val * 1.0;
        }

        return doubleArray;
    }

    private double[] transform(double[] input) {
        double[] tempConversion = new double[input.length];

//        Main.debug("\nNew Sample:");
//        for (double d : input) {
//            Main.debug(String.valueOf(d));
//        }

        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        try {
            Complex[] complex = transformer.transform(input, TransformType.FORWARD);

            for (int i = 0; i < complex.length; i++) {
                double rr = (complex[i].getReal());
                double ri = (complex[i].getImaginary());

                tempConversion[i] = Math.sqrt((rr * rr) + (ri * ri));
            }

        } catch (IllegalArgumentException e) {
            System.out.println(e);
        }

//        Main.debug("===FFT Result:");
//        for (double d : tempConversion) {
//            Main.debug(String.valueOf(d));
//        }

        return tempConversion;
    }

    public double[] toDoubles(byte[] byteArray) {
        double[] doubleArray = new double[byteArray.length / 2];

        for (int i = 0; i < byteArray.length / 2; i++) {
            byte[] temp = new byte[2];
            temp[0] = byteArray[i*2];
            temp[1] = byteArray[i*2+1];

            ByteBuffer.wrap(temp).getDouble();

            doubleArray[i] = ByteBuffer.wrap(temp).getDouble();
        }

        return doubleArray;
    }

    public void printAudioDataToFile() {
        String filename = "C:/Users/Gary/Desktop/sample.csv";
        try {
            PrintWriter out = new PrintWriter(filename);
            Main.debug("Printing to file...");

            int i = 0;
            for (double d : audioData) {
                out.println((int)d);
                if (i++ >= 1000) {
                    Main.debug("Closing file...");
                    out.close();
                    return;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
