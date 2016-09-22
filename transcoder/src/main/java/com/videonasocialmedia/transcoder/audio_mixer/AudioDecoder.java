package com.videonasocialmedia.transcoder.audio_mixer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.videonasocialmedia.transcoder.audio_mixer.listener.OnAudioDecoderListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by Veronica Lago Fominaya on 19/08/2015.
 */
public class AudioDecoder {

    private final static String LOG_TAG = "AudioDecoder";

    private String inputFile;

    public String getOutputFile() {
        return outputFile;
    }

    private String outputFile;

    MediaCodec decoder;
    MediaFormat format;
    OutputStream outputStream;

    int numChannels;
    boolean isMono = false;
    int sampleRate = 0;
    int bitRate = 192 * 1024;

    private int audioTrackId;

    private OnAudioDecoderListener listener;

    public AudioDecoder(String inputFile, String outputFile, OnAudioDecoderListener listener){

        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.listener = listener;

    }

    private boolean setDecoder(MediaExtractor extractor) {
        boolean success = true;
        try {
            format = extractor.getTrackFormat(0);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if(mime.startsWith("audio/")) {
                audioTrackId = 0;
            } else {
                format = extractor.getTrackFormat(1);
                audioTrackId = 1;
            }

            numChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            if(numChannels == 1) isMono=true;
            Log.d(LOG_TAG, "numChannels " + numChannels);
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            Log.d(LOG_TAG, "sampleRate " + sampleRate);
            //decoder = MediaCodec.createDecoderByType("audio/mp4a-latm");
            decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            //format = extractor.getTrackFormat(0);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            decoder.configure(format, null, null, 0);
            decoder.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed creating decoder", e);
            listener.OnFileDecodedError(String.valueOf(e));
        }
        return success;
    }

    public void decode() {

        try {
            outputStream = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            listener.OnFileDecodedError(String.valueOf(e));
        }

        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(inputFile);

        } catch (IOException e) {
            e.printStackTrace();
            listener.OnFileDecodedError(String.valueOf(e));
        }
        setDecoder(extractor);
        ByteBuffer[] codecInputBuffers = decoder.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = decoder.getOutputBuffers();

        // Mode to use AudioTrack while decoding. Study, seekto and time position features
        /*
        // get the sample rate to configure AudioTrack
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        // create our AudioTrack instance
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(sampleRate,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);

        // start playing, we will feed you later
        audioTrack.play();
        */

        extractor.selectTrack(audioTrackId);

        // start decoding ¿Why 10000?
        final long kTimeOutUs = 10000;
        MediaCodec.BufferInfo BufInfo = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;

        int inputBufIndex;

        int counter=0;
        while (!sawOutputEOS) {


            counter++;
            if (!sawInputEOS) {
                inputBufIndex = decoder.dequeueInputBuffer(kTimeOutUs);
                // Log.d(LOG_TAG, " bufIndexCheck " + bufIndexCheck);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                    int sampleSize = extractor
                            .readSampleData(dstBuf, 0 /* offset */);

                    long presentationTimeUs = 0;

                    if (sampleSize < 0) {

                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {

                        presentationTimeUs = extractor.getSampleTime();
                    }
                    // can throw illegal state exception (???)

                    decoder.queueInputBuffer(inputBufIndex, 0 /* offset */,
                            sampleSize, presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    : 0);

                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                } else {
                    Log.e(LOG_TAG, "inputBufIndex " + inputBufIndex);
                    listener.OnFileDecodedError("inputBufIndex " + inputBufIndex);
                }
            }

            int res = decoder.dequeueOutputBuffer(BufInfo, kTimeOutUs);

            if (res >= 0) {
                Log.i(LOG_TAG, "decoding: deqOutputBuffer >=0, counter=" + counter);
                // Log.d(LOG_TAG, "got frame, size " + info.size + "/" +
                // info.presentationTimeUs);
                if (BufInfo.size > 0) {
                    // noOutputCounter = 0;
                }

                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                final byte[] chunk = new byte[BufInfo.size];
                buf.get(chunk);
                buf.clear();

                if (chunk.length > 0) {

                    // play audioTrack
                    //audioTrack.write(chunk, 0, chunk.length);

                    // write to file
                    try {

                        if(isMono) {

                            // Generate false stereo
                            int monoByteArrayLength = chunk.length;
                            byte[] stereoGeneratedSnd = new byte[monoByteArrayLength * 2];

                            for (int i = 0; i < monoByteArrayLength; i += 2) {
                                stereoGeneratedSnd[i * 2 + 0] = chunk[i];
                                stereoGeneratedSnd[i * 2 + 1] = chunk[i + 1];
                                stereoGeneratedSnd[i * 2 + 2] = chunk[i];
                                stereoGeneratedSnd[i * 2 + 3] = chunk[i + 1];
                            }

                            outputStream.write(stereoGeneratedSnd);

                        } else {

                            outputStream.write(chunk);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        listener.OnFileDecodedError(String.valueOf(e));
                    }

                }
                decoder.releaseOutputBuffer(outputBufIndex, false /* render */);
                if ((BufInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.i(LOG_TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = decoder.getOutputBuffers();

                Log.i(LOG_TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = decoder.getOutputFormat();

                Log.i(LOG_TAG, "output outputFormat has changed to " + oformat);
            } else {
                Log.i(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }
        }

        Log.d(LOG_TAG, "stopping...");

        // closing AudioTrack
        /*
        if (audioTrack != null) {
            audioTrack.flush();
            audioTrack.release();
            audioTrack = null;
        }
        */

        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            listener.OnFileDecodedError(String.valueOf(e));
        }
        decoder.stop();
        decoder.release();
        extractor.release();
        listener.OnFileDecodedSuccess(inputFile);
    }

}
