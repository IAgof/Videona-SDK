package com.videonasocialmedia.transcoder.audio;

import android.content.Context;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.videonasocialmedia.transcoder.TranscodingException;
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
public class AudioMixer {
    private static final String LOG_TAG = AudioMixer.class.getSimpleName();
    // TODO(jliarte): 5/10/17 seems to be a bug in gradle plugin that always set libraries as release
    // see https://issuetracker.google.com/issues/36967265
//    private boolean DEBUG = BuildConfig.DEBUG;
    private boolean DEBUG = true;

    private String tempDirectory;
    private String outputFile;

    // TODO(jliarte): 5/10/17 is this still necessary?
    private long durationOutputFile;

    private List<Media> mediaListDecoded;

    public AudioMixer() {
        mediaListDecoded = new ArrayList<>();
    }

    public boolean export(List<Media> mediaList, String tempDirectory, String outputFile,
                          long durationOutputFile) throws IOException, TranscodingException {
        if (mediaList.size() == 0) {
            return false;
        }

        this.tempDirectory = tempDirectory;
        String outputTempMixAudioPath = this.tempDirectory + File.separator + "mixAudio.pcm";
        this.outputFile = outputFile;
        this.durationOutputFile = durationOutputFile;
        cleanTempDirectory();

        for (Media media : mediaList) {
            // TODO(jliarte): 5/10/17 should we move this parameters to method call, as they aren't
            // collaborators
            Log.d(LOG_TAG, "AudioMixer export, decoding " + media.getMediaPath() + " volume " +
            media.getVolume());
            AudioDecoder decoder = new AudioDecoder(media, tempDirectory, durationOutputFile);
            String decodedFilePath = decoder.decode();
            mediaListDecoded.add(new Video(decodedFilePath, media.getVolume()));
            saveDebugWavFile(tempDirectory, decodedFilePath);
        }
        SoundMixer soundMixer = new SoundMixer();
        soundMixer.mixAudio(mediaListDecoded, outputTempMixAudioPath);
        saveDebugWavFile(tempDirectory, outputTempMixAudioPath);
        encodeAudio(outputTempMixAudioPath);
        return true;
    }

    public boolean exportWithFFmpeg(List<Media> mediaList, String tempDirectory, String outputFile,
                                    long durationOutputFile, FFmpeg ffmpeg) throws IOException {
        if (mediaList.size() == 0) {
            return false;
        }

        this.tempDirectory = tempDirectory;
        String outputTempMixAudioPath = this.tempDirectory + File.separator + "mixAudio.pcm";
        this.outputFile = outputFile;
        this.durationOutputFile = durationOutputFile;
        cleanTempDirectory();

        for (Media media : mediaList) {
            // TODO(jliarte): 5/10/17 should we move this parameters to method call, as they aren't
            // collaborators
            Log.d(LOG_TAG, "AudioMixer export, decoding " + media.getMediaPath() + " volume " +
                media.getVolume());
            AudioDecoder decoder = new AudioDecoder(media, tempDirectory, durationOutputFile);
            String decodedFilePath = decoder.decode();
            mediaListDecoded.add(new Video(decodedFilePath, media.getVolume()));
            saveDebugWavFile(tempDirectory, decodedFilePath);
        }
        SoundMixer soundMixer = new SoundMixer();
        soundMixer.mixAudioWithFFmpeg(mediaListDecoded, outputTempMixAudioPath, ffmpeg);
      //  AudioDecoder decoder = new AudioDecoder(outputTempMixAudioPath + ".", tempDirectory, durationOutputFile);
      //  String decodedFilePath = decoder.decode();

        return true;
    }

    private void saveDebugWavFile(String tempDirectory, String pcmFilePath) {
        if (DEBUG) {
            String debugWavFile = tempDirectory + File.separator
                    + new File(pcmFilePath).getName() + ".wav";
            UtilsAudio.copyWaveFile(pcmFilePath, debugWavFile);
        }
    }

    private void encodeAudio(String inputFileRaw) throws IOException, TranscodingException {
        AudioEncoder encoder = new AudioEncoder();
        encoder.encodeToMp4(inputFileRaw, outputFile);
        cleanTempDirectory();
    }

    private void cleanTempDirectory() {
        if (!DEBUG) {
            FileUtils.cleanDirectory(new File(tempDirectory));
        }
    }
}
