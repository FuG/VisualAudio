package com.gfu.app;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class StreamPlayer implements Runnable {
    private File file;
    private StreamInfo streamInfo;

    public StreamPlayer(File file) throws IOException, UnsupportedAudioFileException {
        streamInfo = StreamInfo.getStreamInfo(file);
    }

    public void run() {
        try {
            play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void play() throws LineUnavailableException, IOException {
        AudioInputStream din = streamInfo.decodedInputStream;
        byte[] data = new byte[4096];
        SourceDataLine line = getLine(streamInfo.decodedFormat);

        if (line != null) {
            line.start();

            int nBytesRead = 0;
            while (nBytesRead != -1) {
                nBytesRead = din.read(data, 0, data.length);
                if (nBytesRead != -1) {
                    line.write(data, 0, nBytesRead);
                }
            }

            line.drain();
            line.stop();
            line.close();
        }
    }

    private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }
}
