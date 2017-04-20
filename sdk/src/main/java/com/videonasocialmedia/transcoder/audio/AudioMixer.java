package com.videonasocialmedia.transcoder.audio;

import com.videonasocialmedia.transcoder.audio.listener.OnAudioDecoderListener;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioEncoderListener;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioMixerListener;
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

    public void setOnAudioMixerListener(OnAudioMixerListener listener) {
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
    public void onFileDecodedError(String error) {
        // do something
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
    public void OnFileDecodedSuccess(String outputFile) {

    }

    @Override
    public void OnMixSoundSuccess(String outputFile) {
        if (DEBUG) {
            String tempMixAudioWav = tempDirectory + File.separator + "mixAudio.wav";
            UtilsAudio.copyWaveFile(outputFile, tempMixAudioWav);
        }

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
        if (listener != null) {
            listener.onAudioMixerProgress("Transcoding completed");
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
