package com.videonasocialmedia.transcoder.audio;

import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.videonasocialmedia.ffmpeg.Command;
import com.videonasocialmedia.ffmpeg.CommandBuilder;
import com.videonasocialmedia.ffmpeg.ListenableFutureExecutor;
import com.videonasocialmedia.ffmpeg.VideoKit;
import com.videonasocialmedia.transcoder.TranscodingException;
import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.model.media.Video;
import com.videonasocialmedia.videonamediaframework.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


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

  private List<AudioHelper> audioHelperList;
  private List<Media> mediaListDecoded;
  private VideoKit videoKit;
  private ListenableFutureExecutor listenableFutureExecutor;


  public AudioMixer() {
    videoKit = new VideoKit();
    listenableFutureExecutor = new ListenableFutureExecutor();
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
    mediaListDecoded = new ArrayList<>();
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
                                  long durationOutputFile) throws IOException,
      TranscodingException {
    if (mediaList.size() == 0) {
      return false;
    }
    this.tempDirectory = tempDirectory;
    String outputTempMixAudioPath = this.tempDirectory + File.separator + "mixAudio.wav";
    this.outputFile = outputFile;
    this.durationOutputFile = durationOutputFile;
    cleanTempDirectory();
    audioHelperList = new ArrayList<>();
    // 1.- Decode audio files
    audioHelperList = decodeAudioMediaList(mediaList, tempDirectory, durationOutputFile);

    // 2.- Apply volume and mix audio files
    mixAudioWithFFmpeg(audioHelperList, outputTempMixAudioPath);

    // 3.- Transcode and generate exported Audio file
    encodeAudio(outputTempMixAudioPath);
    return true;
  }

  private List<AudioHelper> decodeAudioMediaList(List<Media> mediaList, String tempDirectory, long durationOutputFile) throws IOException {
    List<AudioHelper> audioHelperList = new ArrayList<>();
    for (Media media : mediaList) {
      // TODO(jliarte): 5/10/17 should we move this parameters to method call, as they aren't
      // collaborators
      Log.d(LOG_TAG, "AudioMixer export, decoding " + media.getMediaPath() + " volume " +
          media.getVolume());
      AudioHelper audioHelper = new AudioHelper(media.getMediaPath(), media.getVolume(),
          tempDirectory);
      AudioDecoder decoder = new AudioDecoder(audioHelper, durationOutputFile);
      decoder.decode();
      audioHelperList.add(audioHelper);
    }
    return audioHelperList;
  }

  private void saveDebugWavFile(String tempDirectory, String pcmFilePath) {
    if (DEBUG) {
      String debugWavFile = tempDirectory + File.separator
          + new File(pcmFilePath).getName() + ".wav";
      UtilsAudio.copyWaveFile(pcmFilePath, debugWavFile);
    }
  }

  public void mixAudioWithFFmpeg(List<AudioHelper> audioHelperList, String outputTempMixAudioPath)
      throws FileNotFoundException, TranscodingException {
    // From pcm to wav
    for (AudioHelper audioHelper : audioHelperList) {
      UtilsAudio.copyWaveFile(audioHelper.getAudioDecodePcm(), audioHelper.getAudioWav());
    }
    // Apply volume to every media
    for (AudioHelper audioHelper : audioHelperList) {
      try {
        applyFFmpegVolume(audioHelper).get();
      } catch (InterruptedException interruptedException) {
        Log.e(LOG_TAG, "Caught InterruptedException applyFFmpegVolume " +
            interruptedException.getClass().getName() + " while exporting, " +
            "message: " + interruptedException.getMessage());
      } catch (ExecutionException executionException) {
        Log.e(LOG_TAG, "Caught ExecutionException applyFFmpegVolume " +
            executionException.getClass().getName() + " while exporting, " +
            "message: " + executionException.getMessage());
      }
    }

    // Apply FFmpeg to get output mix WAV file
    try {
      applyFFmpegMixAudio(audioHelperList, outputTempMixAudioPath).get();
    } catch (InterruptedException interruptedException) {
      Log.e(LOG_TAG, "Caught InterruptedException applyFFmpegMixAudio " +
          interruptedException.getClass().getName() + " while exporting, " +
          "message: " + interruptedException.getMessage());
    } catch (ExecutionException executionException) {
      Log.e(LOG_TAG, "Caught ExecutionException applyFFmpegMixAudio " +
          executionException.getClass().getName() + " while exporting, " +
          "message: " + executionException.getMessage());
    }
  }

  private ListenableFuture applyFFmpegVolume(AudioHelper audioHelper) {
    // Apply volume
    final Command commandVolume = videoKit.createCommand()
        .overwriteOutput()
        .inputPath(audioHelper.getAudioWav())
        .outputPath(audioHelper.getAudioWavWithVolume())
        .customCommand("-filter:a volume=" + audioHelper.getVolume())
        .copyVideoCodec()
        .experimentalFlag()
        .build();
    ListenableFuture listenableFuture = listenableFutureExecutor.execute(new Runnable() {
      @Override
      public void run() {
        commandVolume.execute();
      }
    });

    return listenableFuture;
  }

  private ListenableFuture applyFFmpegMixAudio(List<AudioHelper> audioHelperList,
                                               String outputTempMixAudioPath)
      throws TranscodingException {
    CommandBuilder commandBuilder = videoKit.createCommand();
    for( AudioHelper audioHelper: audioHelperList) {
      commandBuilder.inputPath(audioHelper.getAudioWavWithVolume());
    }
    commandBuilder.overwriteOutput();
    commandBuilder.outputPath(outputTempMixAudioPath);
    commandBuilder.customCommand("-filter_complex");
    commandBuilder.customCommand("amix=inputs=" + audioHelperList.size() + ":duration=first:dropout_transition=3");
    commandBuilder.experimentalFlag();
    final Command commandMix = commandBuilder.build();
    ListenableFuture listenableFuture = listenableFutureExecutor.execute(new Runnable() {
      @Override
      public void run() {
        commandMix.execute();
      }
    });
    return listenableFuture;
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
