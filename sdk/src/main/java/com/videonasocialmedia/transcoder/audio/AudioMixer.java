package com.videonasocialmedia.transcoder.audio;

import android.support.annotation.NonNull;


import com.videonasocialmedia.transcoder.MediaTranscoder;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioDecoderListener;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioEncoderListener;
import com.videonasocialmedia.transcoder.audio.listener.OnMixSoundListener;
import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.model.media.Video;
import com.videonasocialmedia.videonamediaframework.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alvaro on 19/09/16.
 */
public class AudioMixer implements OnAudioDecoderListener, OnMixSoundListener,
        OnAudioEncoderListener {

    private boolean DEBUG = true;

    private String tempDirectory;
    private String outputFile;

    private OnAudioMixerListener listener;

    private long durationOutputFile;

    private List<Media> mediaList;

    private List<Media> mediaListDecoded;

    public AudioMixer(List<Media> mediaList, String tempDirectory, String outputFile,
                      long durationOutputFile){
        this.mediaList = mediaList;
        mediaListDecoded = new ArrayList<>(mediaList.size());
        this.tempDirectory = tempDirectory;
        this.outputFile = outputFile;
        this.durationOutputFile = durationOutputFile;
        cleanTempDirectory();
    }

    public void setOnAudioMixerListener(MediaTranscoder.MediaTranscoderListener listener) {
        this.listener = listener;
    }

    public void export() {
        // TODO:(alvaro.martinez) 20/04/17 Add ListenableFuture, decode files and make asynchronus
        for (Media media : mediaList) {
            AudioDecoder decoder = new AudioDecoder(media, tempDirectory, durationOutputFile, this);
            decoder.decode();
        }
        mixAudio(mediaListDecoded);
    }

    private void mixAudio(List<Media> mediaList) {
        MixSound mixSound = new MixSound(this);
        String outputTempMixAudioPath = tempDirectory + File.separator + "mixAudio.pcm";
        try {
            mixSound.mixAudio(mediaList, outputTempMixAudioPath);
        } catch (IOException e) {
            e.printStackTrace();
            if(listener!=null)
                listener.onTranscodeError(e.getMessage());
        }
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
            if(listener!= null) listener.onTranscodeProgress("Decoded 1st audio track");
          mediaListDecoded.add(new Video(outputFile, media.getVolume()));
            if (DEBUG) {
                String tempFileOneWav = tempDirectory + File.separator + "tempFile1.wav";
                UtilsAudio.copyWaveFile(tempFileOne, tempFileOneWav);
            }
        }

        if (inputFile.compareTo(inputFile2) == 0) {
            isInputFile2Decoded = true;
            if(listener!= null) listener.onTranscodeProgress("Decoded 2nd audio track");

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
        if (DEBUG) {
            String tempMixAudioWav = tempDirectory + File.separator + "mixAudio.wav";
            UtilsAudio.copyWaveFile(outputFile, tempMixAudioWav);
        }

        encodeAudio(outputFile);

        if (listener!= null) {
            listener.onTranscodeProgress("Audio files mixed");
        }
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
