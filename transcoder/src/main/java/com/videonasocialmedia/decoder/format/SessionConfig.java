package com.videonasocialmedia.decoder.format;

/**
 * Configuration information for a Broadcasting or Recording session.
 * Includes meta data, video + audio encoding
 * and muxing parameters
 */
public class SessionConfig {

    private final VideoEncoderConfig mVideoConfig;
    private final AudioEncoderConfig mAudioConfig;

    /**
     * Creates a new session configuration to encoder output
     *
     */
    public SessionConfig() {
        mVideoConfig = new VideoEncoderConfig(1280, 720, 5 * 1000 * 1000);
        mAudioConfig = new AudioEncoderConfig(1, 48000, 192 * 1000);
    }

    /**
     *
     * @param width the width of the video in pixels
     * @param height the height of the video in pixels
     * @param videoBitrate
     * @param audioChannels 1 or 2 channels
     * @param audioFrequency usually 48000 Hz o 441000 Hz
     * @param audioBitrate
     */
    public SessionConfig(int width, int height, int videoBitrate,
                         int audioChannels, int audioFrequency, int audioBitrate){
        mVideoConfig = new VideoEncoderConfig(width, height, videoBitrate);
        mAudioConfig = new AudioEncoderConfig(audioChannels, audioFrequency, audioBitrate);

    }

    public int getTotalBitrate() {
        return mVideoConfig.getBitRate() + mAudioConfig.getBitrate();
    }

    public int getVideoWidth() {
        return mVideoConfig.getWidth();
    }

    public int getVideoHeight() {
        return mVideoConfig.getHeight();
    }

    public int getVideoBitrate() {
        return mVideoConfig.getBitRate();
    }

    public int getNumAudioChannels() {
        return mAudioConfig.getNumChannels();
    }

    public int getAudioBitrate() {
        return mAudioConfig.getBitrate();
    }

    public int getAudioSamplerate() {
        return mAudioConfig.getSampleRate();
    }

}
