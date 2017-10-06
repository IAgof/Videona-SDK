package com.videonasocialmedia.transcoder.audio;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.videonasocialmedia.videonamediaframework.model.media.Media;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by Veronica Lago Fominaya on 19/08/2015.
 */
public class AudioDecoder {
    private final static String LOG_TAG = AudioDecoder.class.getSimpleName();
    public static final String DECODED_AUDIO_PREFIX = "AUD_DECOD_";
    private Media media;

    private String inputFile;
    public String getOutputFile() {
        return outputFile;
    }
    private String outputFile;

    private long durationFile = 0;

    MediaCodec decoder;
    MediaFormat format;
    OutputStream outputStream;

    int numChannels;
    boolean isMono = false;
    int sampleRate = 0;
    int bitRate = 192 * 1024;

    private int audioTrackId;

    /**
     * Should we unify constructors?
     * @param inputFile
     * @param outputFile
     */
    @Deprecated
    public AudioDecoder(String inputFile, String outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    public AudioDecoder(Media media, String tempDirectory, long durationFile) {
        this.media = media;
        this.inputFile = media.getMediaPath();
        String outputName = File.separator + DECODED_AUDIO_PREFIX + System.currentTimeMillis()
                + ".pcm";
        this.outputFile = tempDirectory + outputName;
        this.durationFile = durationFile;
    }

    private boolean setDecoder(MediaExtractor extractor) throws IOException {
        boolean success = true;
        try {
            format = extractor.getTrackFormat(0);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                audioTrackId = 0;
            } else {
                format = extractor.getTrackFormat(1);
                audioTrackId = 1;
            }

            numChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            if(numChannels == 1) isMono=false;
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
        } catch (IOException ioException) {
            Log.e(LOG_TAG, "Failed creating decoder", ioException);
            throw ioException;
//            listener.onFileDecodedError(String.valueOf(ioException));
        }
        return success;
    }

    public String decode() throws IOException {
        outputStream = new FileOutputStream(outputFile);
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(inputFile);
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

        // start decoding ¿Why 10000? - wait up to "timeoutUs" microseconds if timeoutUs > 0.
        final long decoderTimeOutUs = 10000;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;

        int inputBufIndex;

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                inputBufIndex = decoder.dequeueInputBuffer(decoderTimeOutUs);
                if (inputBufIndex >= 0) {
                    // fill inputBuffer with valid data
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    int sampleSize = extractor.readSampleData(dstBuf, 0 /* offset */);
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
                }
                // TODO(jliarte): 5/10/17 should this be an error?
//                else {
//                    Log.e(LOG_TAG, "inputBufIndex " + inputBufIndex);
//                    listener.onFileDecodedError("inputBufIndex " + inputBufIndex);
//                }
            }

            int outputBufferId = decoder.dequeueOutputBuffer(bufferInfo, decoderTimeOutUs);
            if (outputBufferId >= 0) {
                int outputBufIndex = outputBufferId;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                final byte[] chunk = new byte[bufferInfo.size];
                buf.get(chunk);
                buf.clear();

                if (chunk.length > 0) {
                    // write to file
//                    try {
                    if (isMono) {
                        byte[] stereoGeneratedSnd = getFalseStereoBytes(chunk);
                        outputStream.write(stereoGeneratedSnd);
                    } else {
                        outputStream.write(chunk);
                    }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        listener.onFileDecodedError(String.valueOf(e));
//                    }
                }
                decoder.releaseOutputBuffer(outputBufIndex, false /* render */);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.i(LOG_TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }

                if (durationFile > 0 && extractor.getSampleTime() > durationFile) {
                    Log.i(LOG_TAG, "file duration, end of decoder " + extractor.getSampleTime());
                    sawOutputEOS = true;
                }
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = decoder.getOutputBuffers();

                Log.i(LOG_TAG, "output buffers have changed.");
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat outputFormat = decoder.getOutputFormat();
                Log.i(LOG_TAG, "output outputFormat has changed to " + outputFormat);
            } else {
                Log.i(LOG_TAG, "dequeueOutputBuffer returned " + outputBufferId);
            }
        }

        Log.d(LOG_TAG, "stopping...");
//        try {
        outputStream.flush();
        outputStream.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            listener.onFileDecodedError(String.valueOf(e));
//        }
        decoder.stop();
        decoder.release();
        extractor.release();
        return outputFile;
        // (jliarte): 5/10/17 no more need of calling success as we've reached end of method successfully
//        listener.onFileDecodedMediaSuccess(media, outputFile);
    }

    private byte[] getFalseStereoBytes(byte[] chunk) {
        // Generate false stereo
        int monoByteArrayLength = chunk.length;
        byte[] stereoGeneratedSnd = new byte[monoByteArrayLength * 2];

        for (int i = 0; i < monoByteArrayLength; i += 2) {
            stereoGeneratedSnd[i * 2 + 0] = chunk[i];
            stereoGeneratedSnd[i * 2 + 1] = chunk[i + 1];
            stereoGeneratedSnd[i * 2 + 2] = chunk[i];
            stereoGeneratedSnd[i * 2 + 3] = chunk[i + 1];
        }
        return stereoGeneratedSnd;
    }
}
