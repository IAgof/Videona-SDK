 
package com.videonasocialmedia.transcoder.audio;
 
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

import com.videonasocialmedia.sdk.BuildConfig;
import com.videonasocialmedia.transcoder.TranscodingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioEncoder {
    // Source: http://codingmaadi.blogspot.com.es/2014/01/how-to-convert-file-with-raw-pcm.html
    private String LOG_TAG = AudioEncoder.class.getSimpleName();
    public static final String MIME_TYPE = "audio/mp4a-latm";
    public static final int BIT_RATE = 192000;
    public static final int SAMPLING_RATE = 48000; //44100;
    public static final int CODEC_TIMEOUT_IN_MS = 1000; //5000;
    public static final int BUFFER_SIZE = 96000; // 88200; 2*SAMPLE_RATE
    public static final int NUM_CHANNELS = 1; // 2;

    public static final int CHANNELS_COUNT = AudioFormat.CHANNEL_IN_MONO; //AudioFormat.CHANNEL_IN_STEREO; //

    int minBufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNELS_COUNT, 
            AudioFormat.ENCODING_PCM_16BIT);
    private boolean DEBUG = BuildConfig.DEBUG;

    private MediaCodec mediaCodec;
    private MediaFormat mediaFormat;
    private MediaMuxer mediaMuxer;

    private File inputFile;
    private File outputFile;

    protected MediaCodec.BufferInfo mBufferInfo;
    protected int mTrackIndex;
    private boolean hasMoreData = true;
    private FileInputStream fileInputStream;
    private  byte[] tempBuffer = new byte[BUFFER_SIZE];
    private double presentationTimeUs;
    private int totalBytesRead = 0;
    private ByteBuffer[] codecInputBuffers;
    private ByteBuffer[] codecOutputBuffers;
    private int audioTrackIdx = 0;

    public void encodeToMp4(String inputFilePath, String outputFilePath) throws IOException,
            TranscodingException {
        this.inputFile = new File(inputFilePath);
        this.outputFile = new File(outputFilePath);
        setUpMediaEncoder();

        int percentComplete;
        mBufferInfo = new MediaCodec.BufferInfo();
        codecInputBuffers = mediaCodec.getInputBuffers(); // Note: Array of buffers
        codecOutputBuffers = mediaCodec.getOutputBuffers();
        fileInputStream = new FileInputStream(inputFilePath);
        while (mBufferInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
            sendAudio();
            drainAudio();
            //TODO use in a progress seekbar or similar
            percentComplete = (int) Math.round(((float) totalBytesRead / (float) inputFile.length())
                    * 100.0);
            if (DEBUG) {
                Log.v(LOG_TAG, "Conversion % - " + percentComplete);
            }
        }

        fileInputStream.close();
        mediaMuxer.stop();
        mediaMuxer.release();
        if (DEBUG) {
            Log.v(LOG_TAG, "Compression done ...");
        }
        // (jliarte): 5/10/17 no more need of calling success as we've reached end of method successfully
//        listener.OnFileEncodedSuccess(outputFile.getAbsolutePath());
        Log.d(LOG_TAG, "Encoding success!! path " + this.outputFile.getAbsolutePath());
        // TODO(jliarte): 5/10/17 maybe return?
//        return outputFile.getAbsolutePath();
    }

    private void sendAudio() throws IOException {
        int inputBufIndex = 0;
        while (inputBufIndex != -1 && hasMoreData) {
            inputBufIndex = mediaCodec.dequeueInputBuffer(CODEC_TIMEOUT_IN_MS);

            if (inputBufIndex >= 0) {
                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                dstBuf.clear();

                int bytesRead = 0;
                bytesRead = fileInputStream.read(tempBuffer, 0, dstBuf.limit());
                if (bytesRead == -1) { // -1 implies EOS
                    hasMoreData = false;
                    mediaCodec.queueInputBuffer(inputBufIndex, 0, 0, (long) presentationTimeUs,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    totalBytesRead += bytesRead;
                    dstBuf.put(tempBuffer, 0, bytesRead);
                    mediaCodec.queueInputBuffer(inputBufIndex, 0, bytesRead,
                            (long) presentationTimeUs, 0);

                    presentationTimeUs = 1000000l * (totalBytesRead / (2 * NUM_CHANNELS)) 
                            / SAMPLING_RATE;
                }
            }
        }
    }

    private void drainAudio() throws TranscodingException {
        // Drain audio
        int outputBufIndex = 0;
        while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {

            outputBufIndex = mediaCodec.dequeueOutputBuffer(mBufferInfo, CODEC_TIMEOUT_IN_MS);
            if (outputBufIndex >= 0) {
                ByteBuffer encodedData = codecOutputBuffers[outputBufIndex];
                encodedData.position(mBufferInfo.offset);
                encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 
                        && mBufferInfo.size != 0) {
                    mediaCodec.releaseOutputBuffer(outputBufIndex, false);
                } else {
                    mediaMuxer.writeSampleData(audioTrackIdx, codecOutputBuffers[outputBufIndex],
                            mBufferInfo);
                    mediaCodec.releaseOutputBuffer(outputBufIndex, false);
                }
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mediaFormat = mediaCodec.getOutputFormat();
                Log.v(LOG_TAG, "Output format changed - " + mediaFormat);
                audioTrackIdx = mediaMuxer.addTrack(mediaFormat);
                mediaMuxer.start();
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                Log.e(LOG_TAG, "Output buffers changed during encode!");
                throw new TranscodingException("Output buffers changed during encode!");
            } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // NO OP
            } else {
                Log.e(LOG_TAG, "Unknown return code from dequeueOutputBuffer - " + outputBufIndex);
                throw new TranscodingException("Output buffers changed during encode!");
            }
        }
    }


    private void setUpMediaEncoder() throws IOException {
        mediaMuxer = new MediaMuxer(outputFile.getAbsolutePath(),
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mediaFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLING_RATE, CHANNELS_COUNT);

        // Set some properties. Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        // https://github.com/google/ExoPlayer/issues/178 change to AACObjectLTP, // AACObjectERLC
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC); // AACObjectLC)
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLING_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, NUM_CHANNELS);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
        mTrackIndex = -1;
    }
}
 
