package com.videonasocialmedia.transcoder.audio;

import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.google.common.util.concurrent.SettableFuture;
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


  public AudioMixer() {
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
                                  long durationOutputFile, FFmpeg ffmpeg) throws IOException,
      TranscodingException {
    if (mediaList.size() == 0) {
      return false;
    }
    this.tempDirectory = tempDirectory;
    String outputTempMixAudioPath = this.tempDirectory + File.separator + "mixAudio.pcm";
    this.outputFile = outputFile;
    this.durationOutputFile = durationOutputFile;
    cleanTempDirectory();
    audioHelperList = new ArrayList<>();
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

    mixAudioWithFFmpeg(audioHelperList, outputTempMixAudioPath,
        ffmpeg);

    encodeAudio(outputTempMixAudioPath + ".wav");
    return true;
  }

  private void saveDebugWavFile(String tempDirectory, String pcmFilePath) {
    if (DEBUG) {
      String debugWavFile = tempDirectory + File.separator
          + new File(pcmFilePath).getName() + ".wav";
      UtilsAudio.copyWaveFile(pcmFilePath, debugWavFile);
    }
  }

  public void mixAudioWithFFmpeg(List<AudioHelper> audioHelperList, String outputTempMixAudioPath,
                                 FFmpeg ffmpeg) throws FileNotFoundException, TranscodingException {
    // From pcm to wav
    for (AudioHelper audioHelper : audioHelperList) {
      UtilsAudio.copyWaveFile(audioHelper.getAudioDecodePcm(), audioHelper.getAudioWav());
    }
    // Apply volume to every media
    for (AudioHelper audioHelper : audioHelperList) {
      try {
        if (applyFFmpegVolume(ffmpeg, audioHelper).get()) {
          Log.d(LOG_TAG, "audio volume success ");
        } else {
          Log.d(LOG_TAG, "audio volume fail ");
          throw new TranscodingException("Error applying volume audio FFmpeg failure");
        }
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
      if (applyFFmpegMixAudio(ffmpeg, audioHelperList, outputTempMixAudioPath).get()) {
        Log.d(LOG_TAG, "audio volume success ");
      } else {
        Log.d(LOG_TAG, "Error mixing audio failure");
        throw new TranscodingException("Error mixing audio FFmpeg failure");
      }
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

  private SettableFuture<Boolean> applyFFmpegVolume(FFmpeg ffmpeg, AudioHelper audioHelper) {

    String cmd = "-i " + audioHelper.getAudioWav() + " -filter:a volume=" + audioHelper.getVolume()
        + " " + audioHelper.getAudioWavWithVolume();
    String[] command = cmd.split(" ");

    final SettableFuture<Boolean> settableFutureApplyVolume = SettableFuture.create();

    try {
      ffmpeg.execute(command, new FFmpegExecuteResponseHandler() {
        @Override
        public void onStart() {
          Log.d(LOG_TAG, "executeFFmpeg, start");
        }

        @Override
        public void onProgress(String message) {
          Log.d(LOG_TAG, "executeFFmpeg, progress " + message);
        }

        @Override
        public void onFailure(String message) {
          Log.d(LOG_TAG, "executeFFmpeg, failure " + message);
          settableFutureApplyVolume.set(false);
        }

        @Override
        public void onSuccess(String message) {
          Log.d(LOG_TAG, "executeFFmpeg, success " + message);
          settableFutureApplyVolume.set(true);
        }

        @Override
        public void onFinish() {
          Log.d(LOG_TAG, "executeFFmpeg, finish");
        }
      });
    } catch (FFmpegCommandAlreadyRunningException e) {
      // Handle if FFmpeg is already running
      Log.d(LOG_TAG, "executeFFmpeg, FFmpegCommandAlreadyRunningException " + e.getMessage());
    }

    return settableFutureApplyVolume;
  }

  private SettableFuture<Boolean> applyFFmpegMixAudio(FFmpeg ffmpeg,
                                                      List<AudioHelper> audioHelperList,
                                                      String outputTempMixAudioPath)
      throws TranscodingException {

    String audioWAV_output = outputTempMixAudioPath + ".wav";
    String cmdInit = "-i " + audioHelperList.get(0).getAudioWavWithVolume();
    int mediaListSize = audioHelperList.size();
    for (int i = 1; i < mediaListSize; i++) {
      cmdInit += " -i " + audioHelperList.get(i).getAudioWavWithVolume();
    }
    String cmd = cmdInit + " -filter_complex amix=inputs=" + mediaListSize +
        ":duration=first:dropout_transition=" + mediaListSize + " " + audioWAV_output;
    String[] command = cmd.split(" ");

    final SettableFuture<Boolean> settableFutureMixAudio = SettableFuture.create();

    try {
      // to execute "ffmpeg -version" command you just need to pass "-version"
      ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {

        @Override
        public void onStart() {
          Log.d(LOG_TAG, "executeFFmpeg, start");
        }

        @Override
        public void onProgress(String message) {
          Log.d(LOG_TAG, "executeFFmpeg, progress " + message);
        }

        @Override
        public void onFailure(String message) {
          Log.d(LOG_TAG, "executeFFmpeg, failure " + message);
          settableFutureMixAudio.set(false);
        }

        @Override
        public void onSuccess(String message) {
          Log.d(LOG_TAG, "executeFFmpeg, success " + message);
          settableFutureMixAudio.set(true);
        }

        @Override
        public void onFinish() {
          Log.d(LOG_TAG, "executeFFmpeg, finish");
        }
      });
    } catch (FFmpegCommandAlreadyRunningException e) {
      // Handle if FFmpeg is already running
      Log.d(LOG_TAG, "executeFFmpeg, FFmpegCommandAlreadyRunningException " + e.getMessage());
      throw new TranscodingException("Error mixing audio FFmpegCommandAlreadyRunningException ");
    }

    return settableFutureMixAudio;
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
