package com.videonasocialmedia.transcoder.audio_mixer;

import android.support.annotation.NonNull;

import com.videonasocialmedia.transcoder.audio_mixer.listener.OnAudioDecoderListener;
import com.videonasocialmedia.transcoder.audio_mixer.listener.OnAudioEncoderListener;
import com.videonasocialmedia.transcoder.audio_mixer.listener.OnAudioMixerListener;
import com.videonasocialmedia.transcoder.audio_mixer.listener.OnMixSoundListener;

import java.io.File;
import java.io.IOException;

/**
 * Created by alvaro on 19/09/16.
 */
public class AudioMixer implements OnAudioDecoderListener, OnMixSoundListener, OnAudioEncoderListener {

    private boolean DEBUG = true;

    private String inputFile1;
    private String inputFile2;
    private int volume;
    private String tempDirectory;
    private String outputFile;

    private AudioDecoder audioDecoder1;
    private AudioDecoder audioDecoder2;

    private boolean isInputFile1Decoded = false;
    private boolean isInputFile2Decoded = false;

    private OnAudioMixerListener listener;

    public AudioMixer(String inputFile1, String inputFile2, int volume,
                      String tempDirectory, String outputFile, OnAudioMixerListener listener){
        this.inputFile1 = inputFile1;
        this.inputFile2 = inputFile2;
        this.volume = volume;
        this.tempDirectory = tempDirectory;
        this.outputFile = outputFile;
        this.listener = listener;

        cleanTempDirectory();
    }

    public void export() {

        String tempFileOne = tempDirectory + File.separator + "tempFile1.pcm";

        audioDecoder1 = new AudioDecoder(inputFile1, tempFileOne, this);
        audioDecoder1.decode();
        if(DEBUG) {
            String tempFileOneWav = tempDirectory + File.separator + "tempFile1.wav";
            UtilsAudio.copyWaveFile(tempFileOne, tempFileOneWav);
        }

        String tempFileTwo = tempDirectory + File.separator + "tempFile2.pcm";

        audioDecoder2 = new AudioDecoder(inputFile2, tempFileTwo, this);
        audioDecoder2.decode();
        if(DEBUG) {
            String tempFileTwoWav = tempDirectory + File.separator + "tempFile2.wav";
            UtilsAudio.copyWaveFile(tempFileTwo, tempFileTwoWav);
        }
    }

    @NonNull
    private String mixTwoSounds() {

        MixSound mixSound = new MixSound(this);
        String tempMixAudio = tempDirectory + File.separator + "mixAudio.pcm";

        try {
            mixSound.mixTwoSound(audioDecoder1.getOutputFile(), audioDecoder2.getOutputFile(),
                    (float)volume, tempMixAudio);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(DEBUG) {
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
        UtilsAudio.cleanDirectory( new File(tempDirectory));
    }


    @Override
    public void OnFileDecodedSuccess(String inputFile) {
        if(inputFile.compareTo(inputFile1) == 0){
            isInputFile1Decoded = true;
            listener.onAudioMixerProgress("Decoded file 1");
        }

        if(inputFile.compareTo(inputFile2) == 0){
            isInputFile2Decoded = true;
            listener.onAudioMixerProgress("Decoded file 2");
        }

        if(isInputFile1Decoded && isInputFile2Decoded){
            mixTwoSounds();
        }
    }

    @Override
    public void OnFileDecodedError(String error) {
        listener.onAudioMixerError(error);
    }

    @Override
    public void OnMixSoundSuccess(String outputFile) {
        listener.onAudioMixerProgress("Audio files mixed");
        encodeAudio(outputFile);
    }


    @Override
    public void OnMixSoundError(String error) {
        listener.onAudioMixerError(error);
    }

    @Override
    public void OnFileEncodedSuccess(String outputFile) {
        listener.onAudioMixerProgress("Transcoded completed");
        listener.onAudioMixerSuccess(outputFile);
        cleanTempDirectory();
    }

    @Override
    public void OnFileEncodedError(String error) {
        listener.onAudioMixerError(error);
    }
}
