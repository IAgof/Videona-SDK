package com.videonasocialmedia.transcoder.audio;



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
    private boolean result = false;

    public AudioMixer() {
        mediaListDecoded = new ArrayList<>(mediaList.size());
    }


    public boolean export(List<Media> mediaList, String tempDirectory, String outputFile,
                          long durationOutputFile) {
        this.mediaList = mediaList;
        this.tempDirectory = tempDirectory;
        this.outputFile = outputFile;
        this.durationOutputFile = durationOutputFile;
        cleanTempDirectory();

        for (Media media : mediaList) {
            AudioDecoder decoder = new AudioDecoder(media, tempDirectory, durationOutputFile, this);
            decoder.decode();
        }
        mixAudio(mediaListDecoded);

        return result;
    }

    private void mixAudio(List<Media> mediaList) {
        SoundMixer soundMixer = new SoundMixer(this);
        String outputTempMixAudioPath = tempDirectory + File.separator + "mixAudio.pcm";
        try {
            soundMixer.mixAudio(mediaList, outputTempMixAudioPath);
        } catch (IOException e) {
            e.printStackTrace();
            result = false;
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
        result = false;
    }

    @Override
    public void OnMixSoundSuccess(String outputFile) {
        if (DEBUG) {
            String tempMixAudioWav = tempDirectory + File.separator + "mixAudio.wav";
            UtilsAudio.copyWaveFile(outputFile, tempMixAudioWav);
        }
        encodeAudio(outputFile);
    }

    @Override
    public void OnMixSoundError(String error) {
        result = false;
    }

    @Override
    public void OnFileEncodedSuccess(String outputFile) {
        result = true;
        if (!DEBUG) {
            cleanTempDirectory();
        }
    }

    @Override
    public void OnFileEncodedError(String error) {
        result = false;
    }
}
