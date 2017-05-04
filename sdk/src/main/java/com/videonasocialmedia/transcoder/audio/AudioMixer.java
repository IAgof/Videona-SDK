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

    private long durationOutputFile;

    private List<Media> mediaList;

    private List<Media> mediaListDecoded;
    private MediaTranscoder.MediaTranscoderListener listener;

    public AudioMixer(List<Media> mediaList, String tempDirectory, String outputFile,
                      long durationOutputFile){
        this.mediaList = mediaList;
        mediaListDecoded = new ArrayList<>(mediaList.size());
        this.tempDirectory = tempDirectory;
        this.outputFile = outputFile;
        this.durationOutputFile = durationOutputFile;
        cleanTempDirectory();
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
    public void onFileDecodedMediaSuccess(Media media, String outputFile) {
        mediaListDecoded.add(new Video(outputFile, media.getVolume()));
        if (DEBUG) {
            String fileDecoded = tempDirectory + File.separator + new File(outputFile).getName() + ".wav";
            UtilsAudio.copyWaveFile(outputFile, fileDecoded);
        }
    }

    @Override
    public void onFileDecodedError(String error) {
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

    @Override
    public void OnFileDecodedSuccess(String outputFile) {

    }

    public void setListener(MediaTranscoder.MediaTranscoderListener listener) {
        this.listener = listener;
    }
}
