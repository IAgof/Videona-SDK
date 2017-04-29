package com.videonasocialmedia.transcoder.audio;

import android.support.annotation.NonNull;


import com.videonasocialmedia.transcoder.MediaTranscoder;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioDecoderListener;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioEncoderListener;
import com.videonasocialmedia.transcoder.audio.listener.OnMixSoundListener;
import com.videonasocialmedia.videonamediaframework.utils.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by alvaro on 19/09/16.
 */
public class AudioMixer implements OnAudioDecoderListener, OnMixSoundListener,
        OnAudioEncoderListener {

    private boolean DEBUG = true;

    private String inputFile1;
    private String inputFile2;
    private float volume;
    private String tempDirectory;
    private String outputFile;

    private AudioDecoder audioDecoder1;
    private AudioDecoder audioDecoder2;

    private boolean isInputFile1Decoded = false;
    private boolean isInputFile2Decoded = false;

    private MediaTranscoder.MediaTranscoderListener listener;

    private String tempFileTwo;
    private String tempFileOne;

    private long durationOutputFile;

    public AudioMixer(String inputFile1, String inputFile2, float volume,
                      String tempDirectory, String outputFile, long durationOutputFile) {
        this.inputFile1 = inputFile1;
        this.inputFile2 = inputFile2;
        this.volume = volume;
        this.tempDirectory = tempDirectory;
        this.tempFileOne  = tempDirectory + File.separator + "tempFile1.pcm";
        this.tempFileTwo = tempDirectory + File.separator + "tempFile2.pcm";
        this.outputFile = outputFile;
        this.durationOutputFile = durationOutputFile;
        cleanTempDirectory();
    }

    public void setOnAudioMixerListener(MediaTranscoder.MediaTranscoderListener listener) {
        this.listener = listener;
    }

    public void export() {
        audioDecoder1 = new AudioDecoder(inputFile1, tempFileOne, this);
        audioDecoder2 = new AudioDecoder(inputFile2, tempFileTwo, durationOutputFile, this);
        audioDecoder1.decode();
        audioDecoder2.decode();
    }

    @NonNull
    private String mixTwoSounds() {
        MixSound mixSound = new MixSound(this);
        String tempMixAudio = tempDirectory + File.separator + "mixAudio.pcm";

        try {
            mixSound.mixAudioTwoFiles(audioDecoder1.getOutputFile(), audioDecoder2.getOutputFile(),
                    volume, tempMixAudio);
        } catch (IOException e) {
            e.printStackTrace();
            if(listener!=null)
                listener.onTranscodeError(e.getMessage());
        }

        if (DEBUG) {
            String tempMixAudioWav = tempDirectory + File.separator + "mixAudio.wav";
            UtilsAudio.copyWaveFile(tempMixAudio, tempMixAudioWav);
        }
        return tempMixAudio;
    }

    private void encodeAudio(String inputFileRaw) {
        AudioEncoder encoder = new AudioEncoder(inputFileRaw, outputFile, this);
        encoder.run();
    }

    private void cleanTempDirectory() {
        FileUtils.cleanDirectory(new File(tempDirectory));
    }

    @Override
    public void OnFileDecodedSuccess(String inputFile) {
        if (inputFile.compareTo(inputFile1) == 0) {
            isInputFile1Decoded = true;
            if(listener!= null) listener.onTranscodeProgress("Decoded file 1");

            if (DEBUG) {
                String tempFileOneWav = tempDirectory + File.separator + "tempFile1.wav";
                UtilsAudio.copyWaveFile(tempFileOne, tempFileOneWav);
            }
        }

        if (inputFile.compareTo(inputFile2) == 0) {
            isInputFile2Decoded = true;
            if(listener!= null) listener.onTranscodeProgress("Decoded file 2");

            if (DEBUG) {
                String tempFileTwoWav = tempDirectory + File.separator + "tempFile2.wav";
                UtilsAudio.copyWaveFile(tempFileTwo, tempFileTwoWav);
            }
        }
        if (isInputFile1Decoded && isInputFile2Decoded) {
            mixTwoSounds();
        }
    }

    @Override
    public void OnFileDecodedError(String error) {
        //listener.onAudioMixerError(error);
        if(listener!=null)
            listener.onTranscodeError(error);
    }

    @Override
    public void OnMixSoundSuccess(String outputFile) {
        if (listener!= null) {
            listener.onTranscodeProgress("Audio files mixed");
        }
        encodeAudio(outputFile);
    }

    @Override
    public void OnMixSoundError(String error) {
        if (listener!= null) {
            listener.onTranscodeError(error);
        }
    }

    @Override
    public void OnFileEncodedSuccess(String outputFile) {
        if (listener != null) {
            listener.onTranscodeProgress("Transcoded completed");
            listener.onTranscodeSuccess(outputFile);
        }
        if (!DEBUG) {
            cleanTempDirectory();
        }
    }

    @Override
    public void OnFileEncodedError(String error) {
        if(listener!= null)  listener.onTranscodeError(error);
    }
}
