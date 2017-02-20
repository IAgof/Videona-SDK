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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.videonasocialmedia.transcoder.audio.listener.OnMixSoundListener;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;

public class MixSound {


    // INSPIRATION:
    // http://fixabc.xyz/question/32019246/how-to-mix-pcm-audio-sources-java
    // http://stackoverflow.com/questions/19021484/how-to-remove-noise-mix-audio-in-android
    // Source: http://mobilengineering.blogspot.com.es/2012/06/audio-mix-and-record-in-android.html
    // https://www.reddit.com/r/AskProgramming/comments/3h18t5/java_how_to_mix_pcm_audio_sources/
    // http://stackoverflow.com/questions/18675801/signed-16-bit-pcm-transformations-arent-working-why?rq=1
    // http://stackoverflow.com/questions/2398000/audio-playback-creating-nested-loop-for-fade-in-out?rq=1

    private static String LOG_TAG = "MixSound";

    private OnMixSoundListener listener;

    public MixSound(OnMixSoundListener listener){
        this.listener = listener;
    }

    private byte[] output;

    public void mixAudioTwoFiles(String inputFileOne, String inputFileTwo, float scaleFactor,
                                 String outputFile) throws IOException {

        File inputFile1 = new File(inputFileOne);
        File inputFile2 = new File(inputFileTwo);

        RandomAccessFile accessFileOne = new RandomAccessFile (inputFile1, "r");
        FileChannel inChannel1 = accessFileOne.getChannel();

        RandomAccessFile accessFileTwo   = new RandomAccessFile (inputFile2, "r");
        FileChannel inChannel2 = accessFileTwo.getChannel();

        int SIZE_4MB = 1024*1024*4;

        ByteBuffer buffer1 = ByteBuffer.allocate(SIZE_4MB);
        ByteBuffer buffer2 = ByteBuffer.allocate(SIZE_4MB);

        int maxLength = buffer1.limit();
        int indexOutput = 0;
        int sizeChannel1 = (int) inChannel1.size();
        int sizeChannel2 = (int) inChannel2.size();
        long maxSizeOutput = Math.max(accessFileOne.length(), accessFileTwo.length());
        output = new byte[(int)maxSizeOutput];

        while(inChannel1.read(buffer1) > 0)
        {

            if(indexOutput + maxLength > output.length){
                maxLength = output.length - indexOutput;
                Log.d(LOG_TAG, "last Flip");

            }

            if(indexOutput + buffer2.array().length < sizeChannel2) {

                inChannel2.read(buffer2);

                buffer1.flip();
                buffer2.flip();

                byte[] tempOutput = manipulateSamples(adjustVolume(buffer1.array(), (1-scaleFactor)),
                        adjustVolume(buffer2.array(), scaleFactor));


                for (int j = 0; j < maxLength; j++) {
                    output[indexOutput + j] = tempOutput[j];
                }

                buffer1.clear(); // do something with the data and clear/compact it.
                buffer2.clear();

            } else {

                inChannel2.read(buffer2);

                int leftOverBytes = (int) (inChannel2.size() - indexOutput);

                buffer1.flip();
                buffer2.flip();

                byte[] leftOver = manipulateSamples(adjustVolume(buffer1.array(), (1-scaleFactor)),
                        adjustVolume(buffer2.array(), scaleFactor));

                byte[] aux = manipulateSamples(adjustVolume(buffer1.array(), (1-scaleFactor)),
                        adjustVolume(buffer2.array(), 0.0f));

                for (int j = 0; j < maxLength; j++) {
                    if( j<leftOverBytes){
                        output[indexOutput + j] = leftOver[j];
                    } else {
                        output[indexOutput + j] = aux[j];
                    }
                }

                buffer1.clear();
                buffer2.clear();
            }

            indexOutput = indexOutput + maxLength;

        }

        inChannel1.close();
        accessFileOne.close();
        inChannel2.close();
        accessFileTwo.close();

        convertByteToFile(output, outputFile);

        listener.OnMixSoundSuccess(outputFile);

    }

    private byte[] manipulateSamples(byte[] data1, byte[] data2) {

        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                48000, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, 48000, AudioTrack.MODE_STREAM);

       // audioTrack.play();


        // Convert byte[] to short[] (16 bit) to float[] (32 bit) (End result: Big Endian)
        ShortBuffer sbuf1 = ByteBuffer.wrap(data1).asShortBuffer();
        short[] audioShorts1 = new short[sbuf1.capacity()];
        sbuf1.get(audioShorts1);
        int length1 = audioShorts1.length;

        ShortBuffer sbuf2 = ByteBuffer.wrap(data2).asShortBuffer();
        short[] audioShorts2 = new short[sbuf2.capacity()];
        sbuf2.get(audioShorts2);
        int length2 = audioShorts2.length;

        int length_audioFloat = Math.max(length1, length2);

        float[] audioFloats = new float[length_audioFloat];

        if(length1 > length2){

            for (int j = 0; j < length2; j++) {

                audioFloats[j] = ((float) Short.reverseBytes(audioShorts1[j]) / 0x8000)
                        + ((float) Short.reverseBytes(audioShorts2[j]) / 0x8000);
            }

            for (int i = audioShorts2.length; i < audioShorts1.length; i++) {
                audioFloats[i] = ((float) Short.reverseBytes(audioShorts1[i]) / 0x8000);
            }

        } else {

                for (int j = 0; j < length1; j++) {

                    audioFloats[j] = ((float) Short.reverseBytes(audioShorts1[j]) / 0x8000)
                            + ((float) Short.reverseBytes(audioShorts2[j]) / 0x8000);
                }

                for (int i = length1; i < length2; i++) {
                    audioFloats[i] = ((float) Short.reverseBytes(audioShorts2[i]) / 0x8000);
                }

        }


        // Convert float[] to short[] to byte[] (End result: Little Endian)
        short [] audioShortsOut = new short[audioFloats.length];

        for (int i = 0; i < audioFloats.length; i++) {
            audioShortsOut[i] = Short.reverseBytes((short) ((audioFloats[i])*0x8000));
        }

        byte byteArray[] = new byte[Math.max(audioShorts1.length,audioShorts2.length) * 2];
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        sbuf1 = buffer.asShortBuffer();
        sbuf1.put(audioShortsOut);
        byte[] dataOutput = buffer.array();

        audioTrack.write(dataOutput, 0, dataOutput.length);

        return dataOutput;

    }

    private byte[] adjustVolume(byte[] audioSamples, float volume) {
        byte[] array = new byte[audioSamples.length];
        for (int i = 0; i < array.length; i+=2) {
            // convert byte pair to int
            short buf1 = audioSamples[i+1];
            short buf2 = audioSamples[i];

            buf1 = (short) ((buf1 & 0xff) << 8);
            buf2 = (short) (buf2 & 0xff);

            short res= (short) (buf1 | buf2);
            res = (short) (res * volume);

            // convert back
            array[i] = (byte) res;
            array[i+1] = (byte) (res >> 8);

        }
        return array;
    }


    public static void convertByteToFile(byte[] fileBytes, String outputFile) throws FileNotFoundException {

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


    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        long length = file.length();
        if (length > Integer.MAX_VALUE) {
        }

        byte[] bytes = new byte[(int)length];
        int offset   = 0;
        int numRead  = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, Math.min(bytes.length - offset, 512*1024))) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            Log.i(LOG_TAG, "Could not completely read file");
        }
        is.close();

        return bytes;
    }

}
