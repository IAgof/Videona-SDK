 
package com.videonasocialmedia.transcoder.audio_mixer;
 
/**
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
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.videonasocialmedia.decoder.BuildConfig;
import com.videonasocialmedia.transcoder.audio_mixer.listener.OnAudioEncoderListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioEncoder implements Runnable {

    // Source: http://codingmaadi.blogspot.com.es/2014/01/how-to-convert-file-with-raw-pcm.html

    public static final String MIME_TYPE = "audio/mp4a-latm";
    public static final int BIT_RATE = 192000;
    public static final int SAMPLING_RATE = 48000; //44100;
    public static final int CODEC_TIMEOUT_IN_MS = 1000; //5000;
    public static final int BUFFER_SIZE = 96000; // 88200; 2*SAMPLE_RATE
    public static final int NUM_CHANNELS = 1; // 2;
    public static final int CHANNELS_COUNT = AudioFormat.CHANNEL_IN_MONO;//AudioFormat.CHANNEL_IN_STEREO; //

    int minBufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNELS_COUNT, AudioFormat.ENCODING_PCM_16BIT);

    private boolean DEBUG = BuildConfig.DEBUG;
    private String LOGTAG = "AudioEncoder";

    private MediaCodec mediaCodec;
    private MediaFormat mediaFormat;
    private MediaMuxer mediaMuxer;

    private File inputFile;
    private File outputFile;

    protected MediaCodec.BufferInfo mBufferInfo;
    protected int mTrackIndex;
    private boolean hasMoreData = true;
    private FileInputStream fis;
    private  byte[] tempBuffer = new byte[BUFFER_SIZE];
    private double presentationTimeUs;
    private int totalBytesRead = 0;
    private ByteBuffer[] codecInputBuffers;
    private ByteBuffer[] codecOutputBuffers;
    private int audioTrackIdx = 0;

    private OnAudioEncoderListener listener;

    public AudioEncoder(String inputFile, String outputFile, OnAudioEncoderListener listener){

        this.inputFile = new File(inputFile);
        this.outputFile = new File(outputFile);

        this.listener = listener;

        setUpMediaEncoder();

    }

    @Override
    public void run(){

        int percentComplete;

        mBufferInfo = new MediaCodec.BufferInfo();

        codecInputBuffers = mediaCodec.getInputBuffers(); // Note: Array of buffers
        codecOutputBuffers = mediaCodec.getOutputBuffers();


        try {
            fis = new FileInputStream(inputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            listener.OnFileEncodedError(String.valueOf(e));
        }


        while (mBufferInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM) {

            sendAudio();
            drainAudio();

            //TODO use in a progress seekbar or similar
            percentComplete = (int) Math.round(((float) totalBytesRead / (float) inputFile.length()) * 100.0);
            if(DEBUG)
                Log.v(LOGTAG, "Conversion % - " + percentComplete);

        }

        try {
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaMuxer.stop();
        mediaMuxer.release();
        if(DEBUG)
            Log.v(LOGTAG, "Compression done ...");
        listener.OnFileEncodedSuccess(outputFile.getAbsolutePath());

    }

    private void sendAudio(){

        int inputBufIndex = 0;
        while (inputBufIndex != -1 && hasMoreData) {
            inputBufIndex = mediaCodec.dequeueInputBuffer(CODEC_TIMEOUT_IN_MS);

            if (inputBufIndex >= 0) {
                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                dstBuf.clear();

                int bytesRead = 0;
                try {
                    bytesRead = fis.read(tempBuffer, 0, dstBuf.limit());
                } catch (IOException e) {
                    e.printStackTrace();
                    listener.OnFileEncodedError(String.valueOf(e));
                }
                if (bytesRead == -1) { // -1 implies EOS
                    hasMoreData = false;
                    mediaCodec.queueInputBuffer(inputBufIndex, 0, 0, (long) presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    totalBytesRead += bytesRead;
                    dstBuf.put(tempBuffer, 0, bytesRead);
                    mediaCodec.queueInputBuffer(inputBufIndex, 0, bytesRead, (long) presentationTimeUs, 0);

                    presentationTimeUs = 1000000l * (totalBytesRead / (2*NUM_CHANNELS)) / SAMPLING_RATE;
                }
            }
        }
    }

    private void drainAudio(){

        // Drain audio
        int outputBufIndex = 0;
        while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {

            outputBufIndex = mediaCodec.dequeueOutputBuffer(mBufferInfo, CODEC_TIMEOUT_IN_MS);
            if (outputBufIndex >= 0) {
                ByteBuffer encodedData = codecOutputBuffers[outputBufIndex];
                encodedData.position(mBufferInfo.offset);
                encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && mBufferInfo.size != 0) {
                    mediaCodec.releaseOutputBuffer(outputBufIndex, false);
                } else {
                    mediaMuxer.writeSampleData(audioTrackIdx, codecOutputBuffers[outputBufIndex], mBufferInfo);
                    mediaCodec.releaseOutputBuffer(outputBufIndex, false);
                }
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mediaFormat = mediaCodec.getOutputFormat();
                Log.v(LOGTAG, "Output format changed - " + mediaFormat);
                audioTrackIdx = mediaMuxer.addTrack(mediaFormat);
                mediaMuxer.start();
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                Log.e(LOGTAG, "Output buffers changed during encode!");
                listener.OnFileEncodedError("Output buffers changed during encode!");
            } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // NO OP
            } else {
                Log.e(LOGTAG, "Unknown return code from dequeueOutputBuffer - " + outputBufIndex);
                listener.OnFileEncodedError("Unknown return code from dequeueOutputBuffer - " + outputBufIndex);
            }
        }
    }


    private void setUpMediaEncoder(){

        try {
            mediaMuxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
            listener.OnFileEncodedError(String.valueOf(e));
        }

        mediaFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLING_RATE, CHANNELS_COUNT);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        // https://github.com/google/ExoPlayer/issues/178 change to AACObjectLTP, // AACObjectERLC
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC); // AACObjectLC)
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLING_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, NUM_CHANNELS);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        try {
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
            listener.OnFileEncodedError(String.valueOf(e));
        }
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //Log.d(TAG, "mEncoder info " + codec.getCodecInfo().getName());
        mediaCodec.start();

        mTrackIndex = -1;

    }
}
 
