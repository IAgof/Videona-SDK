package com.videonasocialmedia.transcoder.audio;
/*
 * Copyright (C) 2015 Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 *
 * Authors:
 * Álvaro Martínez Marco
 *
 */

import android.support.annotation.NonNull;
import android.util.Log;

import com.videonasocialmedia.videonamediaframework.model.media.Media;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class SoundMixer {
  // INSPIRATION:
  // http://fixabc.xyz/question/32019246/how-to-mix-pcm-audio-sources-java
  // http://stackoverflow.com/questions/19021484/how-to-remove-noise-mix-audio-in-android
  // Source: http://mobilengineering.blogspot.com.es/2012/06/audio-mix-and-record-in-android.html
  // https://www.reddit.com/r/AskProgramming/comments/3h18t5/java_how_to_mix_pcm_audio_sources/
  // http://stackoverflow.com/questions/18675801/signed-16-bit-pcm-transformations-arent-working-why?rq=1
  // http://stackoverflow.com/questions/2398000/audio-playback-creating-nested-loop-for-fade-in-out?rq=1

  private static String LOG_TAG = SoundMixer.class.getSimpleName();
  private ArrayList<File> inputFileList;
  private ArrayList<RandomAccessFile> randomAccessFileList;
  private ArrayList<FileChannel> fileChannelList;
  private ArrayList<ByteBuffer> bufferList;
  public static final int SIZE_4_MB = 1024 * 1024 * 4;

  private byte[] output;

  public void mixAudio(List<Media> mediaList, String outputTempMixAudioPath)
      throws IOException {
    setupDataLists(mediaList);

    int maxLength = bufferList.get(0).limit(); // Video fix buffer length, first list element always is Video
    int indexOutput = 0;
    long maxSizeOutput = randomAccessFileList.get(0).length();
    output = new byte[(int) maxSizeOutput];

    float normalizeVolume = getNormalizeVolume(mediaList);

    while (fileChannelList.get(0).read(bufferList.get(0)) > 0) {
      if (indexOutput + maxLength > output.length) {
        maxLength = output.length - indexOutput;
      }
      prepareInputChunk();
      List<byte[]> dataList = normalizeSoundSamples(mediaList, normalizeVolume);
      byte[] chunkOutputSamples = mixSoundSamples(dataList);
      appendSamplesToOutputAt(chunkOutputSamples, indexOutput, maxLength);
      indexOutput = indexOutput + maxLength;
      clearBuffers();
    }
    closeChannels();
    saveBytesToFile(output, outputTempMixAudioPath);

    Log.d(LOG_TAG, "onMixSoundSuccess");
    // (jliarte): 5/10/17 no more need of calling success as we've reached end of method successfully
//    listener.OnMixSoundSuccess(outputTempMixAudioPath);
  }

  private void clearBuffers() {
    for (ByteBuffer buffer : bufferList) {
      buffer.clear();
    }
  }

  private void appendSamplesToOutputAt(byte[] chunkOutputSamples, int indexOutput, int maxLength) {
    for (int j = 0; j < maxLength; j++) {
      output[indexOutput + j] = chunkOutputSamples[j];
    }
  }

  @NonNull
  private List<byte[]> normalizeSoundSamples(List<Media> mediaList, float normalizeVolume) {
    List<byte[]> dataList = new ArrayList<>(mediaList.size());
    for (Media media : mediaList) {
      byte[] data = adjustVolume(bufferList.get(mediaList.indexOf(media)).array(),
          media.getVolume() / normalizeVolume);
      dataList.add(data);
    }
    return dataList;
  }

  private void prepareInputChunk() throws IOException {
    bufferList.get(0).flip();
    for (int i = 1; i < fileChannelList.size(); i++) {
      fileChannelList.get(i).read(bufferList.get(i));
      bufferList.get(i).flip();
    }
  }

  private void setupDataLists(List<Media> mediaList) throws FileNotFoundException {
    int mediaListSize = mediaList.size();
    inputFileList = new ArrayList<>(mediaListSize);
    for (Media media : mediaList) {
      File file = new File(media.getMediaPath());
      inputFileList.add(file);
    }
    randomAccessFileList = new ArrayList<>(mediaListSize);
    for (File file : inputFileList) {
      RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
      randomAccessFileList.add(randomAccessFile);
    }

    fileChannelList = new ArrayList<>(mediaListSize);
    for (RandomAccessFile randomAccessFile : randomAccessFileList) {
      fileChannelList.add(randomAccessFile.getChannel());
    }

    bufferList = new ArrayList<>(mediaListSize);
    for (int i = 0; i < mediaListSize; i++) {
      bufferList.add(ByteBuffer.allocate(SIZE_4_MB));
    }
  }

  private void closeChannels() throws IOException {
    for (FileChannel fileChannel : fileChannelList) {
      fileChannel.close();
    }
    for (RandomAccessFile randomAccessFile : randomAccessFileList) {
      randomAccessFile.close();
    }
    Log.d(LOG_TAG, "Close");
  }

  private float getNormalizeVolume(List<Media> mediaList) {
    float normalizeVolume = 0;
    if (mediaList.size() == 1) {
      normalizeVolume = 1;
    } else {
      for (Media media : mediaList) {
        normalizeVolume = normalizeVolume + media.getVolume();
      }
    }
    normalizeVolume = normalizeVolume / mediaList.size();
    Log.d(LOG_TAG, "normalize volume " + normalizeVolume);
    return normalizeVolume;
  }

  private byte[] mixSoundSamples(List<byte[]> dataList) {
    Log.d(LOG_TAG, "mixSoundSamples ...");

    // Convert byte[] to short[] (16 bit) to float[] (32 bit) (End result: Big Endian)
    List<ShortBuffer> shortBufferList = new ArrayList<>(dataList.size());
    for (byte[] data : dataList) {
      shortBufferList.add(ByteBuffer.wrap(data).asShortBuffer());
    }

    List<short[]> audioShortsList = new ArrayList<>(dataList.size());
    for (ShortBuffer shortBuffer : shortBufferList) {
      audioShortsList.add(new short[shortBuffer.capacity()]);
    }

    for (int i = 0; i < dataList.size(); i++) {
      shortBufferList.get(i).get(audioShortsList.get(i));
    }

    int length = audioShortsList.get(0).length; // Video always first element, bigger time, size.
    float[] audioFloats = new float[length];

    for (int j = 0; j < length; j++) {
      for (short[] audioShort : audioShortsList) {
        audioFloats[j] = audioFloats[j] + ((float) Short.reverseBytes(audioShort[j]) / 0x8000);
      }
    }

    // Convert float[] to short[] to byte[] (End result: Little Endian)
    short[] audioShortsOut = new short[audioFloats.length];

    for (int i = 0; i < audioFloats.length; i++) {
      audioShortsOut[i] = Short.reverseBytes((short) ((audioFloats[i]) * 0x8000));
    }

    byte byteArray[] = new byte[length * 2];
    ByteBuffer buffer = ByteBuffer.wrap(byteArray);
    ShortBuffer sbuf = buffer.asShortBuffer();
    sbuf.put(audioShortsOut);
    byte[] dataOutput = buffer.array();

    return dataOutput;
  }

  private byte[] adjustVolume(byte[] audioSamples, float volume) {
    byte[] array = new byte[audioSamples.length];
    for (int i = 0; i < array.length; i += 2) {
      // convert byte pair to int
      short buf1 = audioSamples[i + 1];
      short buf2 = audioSamples[i];

      buf1 = (short) ((buf1 & 0xff) << 8);
      buf2 = (short) (buf2 & 0xff);

      short res = (short) (buf1 | buf2);
      res = (short) (res * volume);

      // convert back
      array[i] = (byte) res;
      array[i + 1] = (byte) (res >> 8);
    }
    return array;
  }

  public static void saveBytesToFile(byte[] fileBytes, String outputFile)
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

//  public static byte[] getBytesFromFile(File file) throws IOException {
//    InputStream is = new FileInputStream(file);
//
//    long length = file.length();
//    if (length > Integer.MAX_VALUE) {
//    }
//
//    byte[] bytes = new byte[(int) length];
//    int offset = 0;
//    int numRead = 0;
//    while (offset < bytes.length && (numRead = is.read(bytes, offset, Math.min(bytes.length - offset, 512 * 1024))) >= 0) {
//      offset += numRead;
//    }
//
//    if (offset < bytes.length) {
//      Log.i(LOG_TAG, "Could not completely read file");
//    }
//    is.close();
//
//    return bytes;
//  }

}
