package com.gfu;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class StreamInfo {
    private static int BITRATE = 16;

    private static File file = null;
    private static StreamInfo streamInfoSingleton = null;

    public AudioInputStream baseInputStream;
    public AudioInputStream decodedInputStream;

    public AudioFormat baseFormat;
    public AudioFormat decodedFormat;

    public int bytesPerSecond = 0;

    public static StreamInfo getStreamInfo(File file) throws IOException, UnsupportedAudioFileException {
        if (streamInfoSingleton == null || !file.equals(StreamInfo.file)) {
            streamInfoSingleton = new StreamInfo(file);
        }
        return streamInfoSingleton;
    }

    private StreamInfo(File file) throws IOException, UnsupportedAudioFileException {
        StreamInfo.file = file;

        reset();
    }

    public void reset() throws IOException, UnsupportedAudioFileException {
        baseInputStream = AudioSystem.getAudioInputStream(file);
        baseFormat = baseInputStream.getFormat();
        decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                BITRATE,
                baseFormat.getChannels(),
                baseFormat.getChannels() * (BITRATE / 8),
                baseFormat.getSampleRate(),
                false);
        Main.debug("Channels: " + baseFormat.getChannels());
        Main.debug("FrameRate: " + baseFormat.getFrameRate());
        Main.debug("FrameSize: " + baseFormat.getFrameSize());
        Main.debug("SampleRate: " + baseFormat.getSampleRate());
        Main.debug("BitRate: " + baseFormat.getSampleSizeInBits());
        decodedInputStream = AudioSystem.getAudioInputStream(decodedFormat, baseInputStream);

        bytesPerSecond = (int) (baseFormat.getSampleRate() * baseFormat.getChannels() * (BITRATE / 8));
    }
}
