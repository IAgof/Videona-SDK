package com.videonasocialmedia.transcoder.audio;


import com.videonasocialmedia.transcoder.audio.listener.OnAudioDecoderListener;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioEffectListener;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioEncoderListener;
import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.utils.FileUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by alvaro on 22/10/16.
 */

public class AudioEffect implements OnAudioDecoderListener, OnAudioEncoderListener {

  private static final String LOG_TAG = "AudioEffect";
  private static final boolean DEBUG = true;

  private final String inputFile;
  private final int timeFadeInMs;
  private final int timeFadeOutMs;
  private final String tempDirectory;
  private final String outputFile;
  private final String tempFilePcm;
  private final String tempFileFadeInOut;
  private AudioDecoder audioDecoder;

  private byte[] output;
  private OnAudioEffectListener listener;
  private long maxSizeOutput;


  public AudioEffect(String inputFile, int timeFadeInMs, int timeFadeOutMs,
                     String tempDirectory, String outputFile) {
    this.inputFile = inputFile;
    this.timeFadeInMs = timeFadeInMs;
    this.timeFadeOutMs = timeFadeOutMs;
    this.tempDirectory = tempDirectory;
    tempFilePcm = tempDirectory + File.separator + "tempFilePcm_" + System.currentTimeMillis() +
        ".pcm";
    tempFileFadeInOut = tempDirectory + File.separator + "tempFileFadeInOut_" +
        System.currentTimeMillis() + ".pcm";
    this.outputFile = outputFile;
  }

  public void setOnAudioEffectListener(OnAudioEffectListener listener) {
    this.listener = listener;
  }

  public void transitionFadeInOut() {
    audioDecoder = new AudioDecoder(inputFile, tempFilePcm, this);
    audioDecoder.decode();
  }

  @Override
  public void OnFileDecodedSuccess(String outputFile) {
    if (DEBUG) {
      String tempFileWav = tempDirectory + File.separator + "tempFilePcm_" +
          System.currentTimeMillis() + ".wav";
      UtilsAudio.copyWaveFile(tempFilePcm, tempFileWav);
    }

    try {
      setFadeInFadeOut(tempFilePcm, timeFadeInMs, timeFadeOutMs);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onFileDecodedError(String error) {
    if (listener != null) {
      listener.onAudioEffectError(error);
    }
  }

  @Override
  public void onFileDecodedMediaSuccess(Media media, String outputFile) {

  }

  @Override
  public void OnFileEncodedSuccess(String outputFile) {
    deleteTempFiles();
    if (listener != null) {
      listener.onAudioEffectProgress("Transcoded completed");
      listener.onAudioEffectSuccess(outputFile);
    }
  }

  private void deleteTempFiles() {
    FileUtils.removeFile(tempFilePcm);
    FileUtils.removeFile(tempFileFadeInOut);
  }

  @Override
  public void OnFileEncodedError(String error) {
    if (listener != null) {
      listener.onAudioEffectError(error);
    }
  }

  private void setFadeInFadeOut(String tempFilePcm, int timeFadeInMs, int timeFadeOutMs)
      throws IOException {
    File inputFile = new File(tempFilePcm);
    FileInputStream fis = new FileInputStream(inputFile);
    byte[] arrayAudio = createByteArray(fis);
    fis.close();

    output = manipulateFadeInOut(arrayAudio, timeFadeInMs, timeFadeOutMs);
    convertByteToFile(output, tempFileFadeInOut);

    encodeAudio(tempFileFadeInOut);
  }

  private void setFadeInFadeOutBigSizeFiles(String tempFilePcm) throws IOException {
    File inputFile1 = new File(tempFilePcm);

    RandomAccessFile accessFileOne = new RandomAccessFile(inputFile1, "r");
    FileChannel inChannel1 = accessFileOne.getChannel();

    int SIZE_4MB = 1024 * 512;

    ByteBuffer buffer1 = ByteBuffer.allocate(SIZE_4MB);
    int maxLength = buffer1.limit();
    int indexOutput = 0;

    int sizeChannel1 = (int) inChannel1.size();

    maxSizeOutput = accessFileOne.length();
    output = new byte[(int) maxSizeOutput];

    while (inChannel1.read(buffer1) > 0) {
      byte[] tempOutput = manipulateFadeInOut2(buffer1.array(), indexOutput);


      for (int j = 0; j < maxLength; j++) {
        output[indexOutput + j] = tempOutput[j];
      }

      buffer1.clear(); // do something with the data and clear/compact it.
      indexOutput = indexOutput + maxLength;
    }

    convertByteToFile(output, tempFileFadeInOut);

    encodeAudio(tempFileFadeInOut);
  }

  public byte[] createByteArray(FileInputStream is) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //byte[] buff = new byte[lenght_array_music];
    byte[] buff = new byte[10240];
    int i = Integer.MAX_VALUE;
    while ((i = is.read(buff, 0, buff.length)) > 0) {
      baos.write(buff, 0, i);
    }

    return baos.toByteArray(); // be sure to close InputStream in calling function
  }

  private void encodeAudio(String tempFileFadeInOut) {
    AudioEncoder encoder = new AudioEncoder(tempFileFadeInOut, outputFile, this);
    encoder.run();
  }

  public static void convertByteToFile(byte[] fileBytes, String outputFile)
      throws FileNotFoundException {

    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile));
    try {
      bos.write(fileBytes);
      bos.flush();
      bos.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private byte[] manipulateFadeInOut(byte[] data, int timeFadeInMs, int timeFadeOutMs) {
    // valores en un segundo 96000 = 48000muestras * 16bit / 8 (bytes)
    int fadeIn = (int) (96000 * timeFadeInMs * 0.001);
    int fadeOut = (int) (96000 * timeFadeOutMs * 0.001);

    // Convert byte[] to short[] (16 bit) to float[] (32 bit) (End result: Big Endian)
    ShortBuffer sbuf = ByteBuffer.wrap(data).asShortBuffer();
    short[] audioShorts = new short[sbuf.capacity()];
    sbuf.get(audioShorts);
    int lengthAudio = audioShorts.length;

    float gain = 0.0f;
    float deltaGainIn = (float) ((1.0 - gain) / (fadeIn));
    float deltaGainOut = (float) ((1.0 - gain) / (fadeOut));

    float[] audioFloats = new float[lengthAudio];

    int lengthOut = lengthAudio - fadeOut;

    for (int j = 0; j < lengthAudio; j++) {

      if (j < fadeIn) {
        gain += deltaGainIn;
        audioFloats[j] = ((float) gain * Short.reverseBytes(audioShorts[j]) / 0x8000);
      } else {

        if (j > lengthOut) {
          gain -= deltaGainOut;
          audioFloats[j] = ((float) gain * Short.reverseBytes(audioShorts[j]) / 0x8000);

        } else {

          audioFloats[j] = ((float) Short.reverseBytes(audioShorts[j]) / 0x8000);
        }
      }
    }

    // Convert float[] to short[] to byte[] (End result: Little Endian)
    short[] audioShortsOut = new short[audioFloats.length];

    for (int i = 0; i < audioFloats.length; i++) {
      audioShortsOut[i] = Short.reverseBytes((short) ((audioFloats[i]) * 0x8000));
    }

    byte byteArray[] = new byte[audioShorts.length * 2];
    ByteBuffer buffer = ByteBuffer.wrap(byteArray);
    sbuf = buffer.asShortBuffer();
    sbuf.put(audioShortsOut);
    byte[] dataOutput = buffer.array();

    return dataOutput;
  }

  private byte[] manipulateFadeInOut2(byte[] data, int indexOutput) {
    int fadeIn = 96000 * 4;
    int fadeOut = 96000 * 4;

    // Convert byte[] to short[] (16 bit) to float[] (32 bit) (End result: Big Endian)
    ShortBuffer sbuf1 = ByteBuffer.wrap(data).asShortBuffer();
    short[] audioShorts1 = new short[sbuf1.capacity()];
    sbuf1.get(audioShorts1);
    int lengthAudio = audioShorts1.length;

    float gain = 0.0f;
    float deltaGainIn = (float) ((1.0 - gain) / (fadeIn));
    float deltaGainOut = (float) ((1.0 - gain) / (fadeOut));

    float[] audioFloats = new float[lengthAudio];

    if (indexOutput < lengthAudio) {
      audioFloats = getAudioFloatFadeIn(fadeIn, audioShorts1, gain, deltaGainIn, lengthAudio);
    } else {
      if (indexOutput > (maxSizeOutput - lengthAudio)) {
        audioFloats = getAudioFloatFadeOut(fadeOut, audioShorts1, gain, deltaGainOut, lengthAudio);
      } else {
        audioFloats = getAudioFloat(audioShorts1, lengthAudio);
      }
    }


    // Convert float[] to short[] to byte[] (End result: Little Endian)
    short[] audioShortsOut = new short[audioFloats.length];

    for (int i = 0; i < audioFloats.length; i++) {
      audioShortsOut[i] = Short.reverseBytes((short) ((audioFloats[i]) * 0x8000));
    }

    byte byteArray[] = new byte[audioShorts1.length * 2];
    ByteBuffer buffer = ByteBuffer.wrap(byteArray);
    sbuf1 = buffer.asShortBuffer();
    sbuf1.put(audioShortsOut);
    byte[] dataOutput = buffer.array();

    return dataOutput;
  }

  private float[] getAudioFloatFadeIn(int fadeIn, short[] audioShorts1, float gain,
                                      float deltaGainIn, int lengthAudio) {
    float[] audioFloats = new float[lengthAudio];
    for (int j = 0; j < lengthAudio; j++) {

      if (j < fadeIn) {
        gain += deltaGainIn;
        audioFloats[j] = (gain * Short.reverseBytes(audioShorts1[j]) / 0x8000);
      } else {
        audioFloats[j] = ((float) Short.reverseBytes(audioShorts1[j]) / 0x8000);
      }
    }
    return audioFloats;
  }

  private float[] getAudioFloatFadeOut(int fadeOut, short[] audioShorts1, float gain,
                                       float deltaGainIn, int lengthAudio) {
    float[] audioFloats = new float[lengthAudio];

    for (int j = 0; j < lengthAudio; j++) {

      if (j > lengthAudio - fadeOut) {
        gain -= deltaGainIn;
        audioFloats[j] = (gain * Short.reverseBytes(audioShorts1[j]) / 0x8000);
      } else {
        audioFloats[j] = ((float) Short.reverseBytes(audioShorts1[j]) / 0x8000);
      }
    }
    return audioFloats;
  }

  private float[] getAudioFloat(short[] audioShorts1, int lengthAudio) {
    float[] audioFloats = new float[lengthAudio];

    for (int j = 0; j < lengthAudio; j++) {

      audioFloats[j] = ((float) Short.reverseBytes(audioShorts1[j]) / 0x8000);

    }
    return audioFloats;
  }
}
