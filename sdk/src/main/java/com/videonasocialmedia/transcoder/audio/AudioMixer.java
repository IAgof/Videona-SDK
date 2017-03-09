package com.videonasocialmedia.transcoder.audio;

import android.support.annotation.NonNull;


import com.videonasocialmedia.transcoder.audio.listener.OnAudioDecoderListener;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioEncoderListener;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioMixerListener;
import com.videonasocialmedia.transcoder.audio.listener.OnMixSoundListener;
import com.videonasocialmedia.videonamediaframework.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

    private OnAudioMixerListener listener;

    private String tempFileTwo;
    private String tempFileOne;

    private long durationOutputFile;

    private List<AudioToExport> mediaList;

    public AudioMixer(String inputFile1, String inputFile2, float volume,
                      String tempDirectory, String outputFile, long durationOutputFile) {
        this.inputFile1 = inputFile1;
        this.inputFile2 = inputFile2;
        this.volume = volume;
        this.tempDirectory = tempDirectory;
        tempFileOne  = tempDirectory + File.separator + "tempFile1.pcm";
        tempFileTwo = tempDirectory + File.separator + "tempFile2.pcm";
        this.outputFile = outputFile;
        this.durationOutputFile = durationOutputFile;
        cleanTempDirectory();
    }

    public AudioMixer(List<AudioToExport> mediaList, String tempDirectory, String outputFile,
                      long durationOutputFile){
        this.mediaList = mediaList;
        this.tempDirectory = tempDirectory;
        this.outputFile = outputFile;
        this.durationOutputFile = durationOutputFile;
        cleanTempDirectory();
    }

    public void setOnAudioMixerListener(OnAudioMixerListener listener) {
        this.listener = listener;
    }

    public void export() {
        /*audioDecoder1 = new AudioDecoder(inputFile1, tempFileOne, this);
        audioDecoder2 = new AudioDecoder(inputFile2, tempFileTwo, durationOutputFile, this);
        audioDecoder1.decode();
        audioDecoder2.decode();*/
        for(AudioToExport media: mediaList){
            AudioDecoder decoder = new AudioDecoder(media, tempDirectory, durationOutputFile, this);
            decoder.decode();
        }

        for(AudioToExport media: mediaList) {
            while (!media.isMediaAudioDecoded()) {
                try {
                    int countWaiting = 0;
                    if (countWaiting > 100) {
                        break;
                    }
                    countWaiting++;
                    Thread.sleep(1000);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        }
        mixAudio(mediaList);
    }

    private void mixAudio(List<AudioToExport> mediaList) {
        MixSound mixSound = new MixSound(this);
        String outputTempMixAudioPath = tempDirectory + File.separator + "mixAudio.pcm";

        try {
            mixSound.mixAudio(mediaList, outputTempMixAudioPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
      /*  try {
            mixSound.mixAudioTwoFiles(mediaList.get(0).getMediaDecodeAudioPath(),
                mediaList.get(1).getMediaDecodeAudioPath(), mediaList.get(1).getMediaVolume(),
                outputTempMixAudioPath);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
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
    public void OnFileDecodedError(String error) {
        // do something
    }

    @Override
    public void onFileDecodedMediaSuccess(AudioToExport media, String outputFile) {
        media.setMediaDecodeAudioPath(outputFile);
    }

    @Override
    public void OnFileDecodedSuccess(String outputFile) {

    }

    @Override
    public void OnMixSoundSuccess(String outputFile) {
        encodeAudio(outputFile);

        if (listener!= null) {
            listener.onAudioMixerProgress("Audio files mixed");
        }

    }

    @Override
    public void OnMixSoundError(String error) {
        if (listener!= null) {
            listener.onAudioMixerError(error);
        }
    }

    @Override
    public void OnFileEncodedSuccess(String outputFile) {
        if (listener!= null) {
            listener.onAudioMixerProgress("Transcoded completed");
            listener.onAudioMixerSuccess(outputFile);
        }
        if (!DEBUG) {
            cleanTempDirectory();
        }
    }

    @Override
    public void OnFileEncodedError(String error) {
        if(listener!= null)  listener.onAudioMixerError(error);
    }
}
