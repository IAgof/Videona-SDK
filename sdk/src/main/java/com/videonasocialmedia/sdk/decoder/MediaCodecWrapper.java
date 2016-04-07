package com.videonasocialmedia.sdk.decoder;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Simplifies the MediaCodec interface by wrapping around the buffer processing operations.
 */
public class MediaCodecWrapper {

    private String TAG = "MediaCodecWrapper";
    private static SurfaceTexture surfaceTexture;
    private OutputFormatChangedListener outputFormatChangedListener = null;
    private OutputSampleListener outputSampleListener;

    /**
     * Handler to use for {@code OutputSampleListener} and {code OutputFormatChangedListener}
     * callbacks.
     */
    private Handler handler;

    /**
     * The {@link MediaCodec} that is managed by this class.
     */
    private MediaCodec decoder;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private Queue<Integer> availableInputBuffers;
    private Queue<Integer> availableOutputBuffers;
    private MediaCodec.BufferInfo[] outputBufferInfo;
    private static MediaCodec.CryptoInfo cryptoInfo = new MediaCodec.CryptoInfo();

    public interface OutputFormatChangedListener {
        void outputFormatChanged(MediaCodecWrapper sender, MediaFormat newFormat);
    }

    /**
     * Callback for decodes frames. Observers can register a listener for optional stream
     * of decoded data
     */
    public interface OutputSampleListener {
        void outputSample(MediaCodecWrapper sender, MediaCodec.BufferInfo info, ByteBuffer buffer,
                          SurfaceTexture surfaceTexture);
    }

    private MediaCodecWrapper(MediaCodec codec) {
        Log.d(TAG, "MediaCodecWrapper");
        decoder = codec;
        codec.start();
        inputBuffers = codec.getInputBuffers();
        outputBuffers = codec.getOutputBuffers();
        outputBufferInfo = new MediaCodec.BufferInfo[outputBuffers.length];
        availableInputBuffers = new ArrayDeque<>(outputBuffers.length);
        availableOutputBuffers = new ArrayDeque<>(inputBuffers.length);
    }

    /**
     * Ends the decoding session.
     */
    public void stop() {
        Log.d(TAG, "stop");
        decoder.stop();
    }

    /**
     * Releases resources.
     */
    public void release() {
        Log.d(TAG, "release");
        decoder.release();
        decoder = null;
        inputBuffers = null;
        outputBuffers = null;
        outputBufferInfo = null;
        availableInputBuffers = null;
        availableOutputBuffers = null;
        handler = null;
    }

    /**
     * Getter for the registered {@link OutputFormatChangedListener}
     */
    public OutputFormatChangedListener getOutputFormatChangedListener() {
        return outputFormatChangedListener;
    }

    public void setOutputSampleListener(OutputSampleListener sampleListener){
        Log.d(TAG, "setOutputSampleListener");
        outputSampleListener = sampleListener;
    }

    /**
     *
     * @param outputFormatChangedListener the listener for callback.
     * @param handler message handler for posting the callback.
     */
    public void setOutputFormatChangedListener(final OutputFormatChangedListener
                                                       outputFormatChangedListener, Handler handler) {
        Log.d(TAG, "setOutputFormatChangedListener");
        this.outputFormatChangedListener = outputFormatChangedListener;

        // Making sure we don't block ourselves due to a bad implementation of the callback by
        // using a handler provided by client.
        Looper looper;
        this.handler = handler;
        if (outputFormatChangedListener != null && this.handler == null) {
            if ((looper = Looper.myLooper()) != null) {
                this.handler = new Handler();
            } else {
                throw new IllegalArgumentException(
                        "Looper doesn't exist in the calling thread");
            }
        }
    }

    /**
     * Constructs the {@link MediaCodecWrapper} wrapper object around the video codec.
     * The codec is created using the encapsulated information in the
     * {@link MediaFormat} object.
     *
     * @param trackFormat The format of the media object to be decoded.
     * @param texture Surface to render the decoded frames.
     * @return
     */
    public static MediaCodecWrapper fromVideoFormat(final MediaFormat trackFormat,
                                                    SurfaceTexture texture) throws IOException {
        Log.d("MediaCodecWrapper", "fromVideoFormat");
        surfaceTexture = texture;
        MediaCodecWrapper result = null;
        MediaCodec videoCodec = null;

        final String mimeType = trackFormat.getString(MediaFormat.KEY_MIME);
        if (mimeType.contains("video/")) {
            videoCodec = MediaCodec.createDecoderByType(mimeType);
            videoCodec.configure(trackFormat, new Surface(texture), null,  0);
        }
        if (videoCodec != null) {
            result = new MediaCodecWrapper(videoCodec);
        }

        return result;
    }

    /**
     * Write a media sample to the decoder.
     *
     * A "sample" here refers to a single atomic access unit in the media stream. The definition
     * of "access unit" is dependent on the type of encoding used, but it typically refers to
     * a single frame of video or a few seconds of audio. {@link MediaExtractor}
     * extracts data from a stream one sample at a time.
     *
     * @param input A ByteBuffer containing the input data for one sample. The buffer must be set
     * up for reading, with its position set to the beginning of the sample data and its limit
     * set to the end of the sample data.
     *
     * @param presentationTimeUs  The time, relative to the beginning of the media stream,
     * at which this buffer should be rendered.
     *
     * @param flags Flags to pass to the decoder. See {@link MediaCodec#queueInputBuffer(int,
     * int, int, long, int)}
     *
     * @throws MediaCodec.CryptoException
     */
    public boolean writeSample(final ByteBuffer input, final MediaCodec.CryptoInfo encryptedInfo,
                               final long presentationTimeUs,
                               final int flags) throws MediaCodec.CryptoException, WriteException {
        Log.d(TAG, "writeSample");
        boolean result = false;
        int size = input.remaining();

        if (size > 0 &&  !availableInputBuffers.isEmpty()) {
            int index = availableInputBuffers.remove();
            ByteBuffer buffer = inputBuffers[index];

            if (size > buffer.capacity()) {
                throw new MediaCodecWrapper.WriteException(String.format(
                        "Insufficient capacity in MediaCodec buffer: "
                                + "tried to write %d, buffer capacity is %d.",
                        input.remaining(),
                        buffer.capacity()));
            }
            buffer.clear();
            buffer.put(input);

            if (encryptedInfo == null)
                decoder.queueInputBuffer(index, 0, size, presentationTimeUs, flags);
            else
                decoder.queueSecureInputBuffer(index, 0, encryptedInfo, presentationTimeUs, flags);
            result = true;
        }
        return result;
    }

    /**
     * Write a media sample to the decoder.
     *
     * A "sample" here refers to a single atomic access unit in the media stream. The definition
     * of "access unit" is dependent on the type of encoding used, but it typically refers to
     * a single frame of video or a few seconds of audio. {@link MediaExtractor}
     * extracts data from a stream one sample at a time.
     *
     * @param extractor  Instance of {@link MediaExtractor} wrapping the media.
     *
     * @param presentationTimeUs The time, relative to the beginning of the media stream,
     * at which this buffer should be rendered.
     *
     * @param flags  Flags to pass to the decoder. See {@link MediaCodec#queueInputBuffer(int,
     * int, int, long, int)}
     *
     * @throws MediaCodec.CryptoException
     */
    public boolean writeSample(final MediaExtractor extractor, final boolean isEncrypted,
                               final long presentationTimeUs, int flags) {
        Log.d(TAG, "writeSample");
        boolean result = false;

        if (!availableInputBuffers.isEmpty()) {
            int index = availableInputBuffers.remove();
            ByteBuffer buffer = inputBuffers[index];

            int size = extractor.readSampleData(buffer, 0);
            if (size <= 0) {
                flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                Log.d(TAG, "writeSample endOfData");
            }
            if (!isEncrypted)
                decoder.queueInputBuffer(index, 0, size, presentationTimeUs, flags);
            else {
                extractor.getSampleCryptoInfo(cryptoInfo);
                decoder.queueSecureInputBuffer(index, 0, cryptoInfo, presentationTimeUs, flags);
            }
            result = true;
        }
        return result;
    }

    /**
     * Performs a peek() operation in the queue to extract media info for the buffer ready to be
     * released i.e. the head element of the queue.
     *
     * @param out_bufferInfo An output var to hold the buffer info.
     *
     * @return True, if the peek was successful.
     */
    public boolean peekSample(MediaCodec.BufferInfo out_bufferInfo) {
        Log.d(TAG, "peekSample");
        updateCodecState();
        boolean result = false;
        if (!availableOutputBuffers.isEmpty()) {
            int index = availableOutputBuffers.peek();
            MediaCodec.BufferInfo info = outputBufferInfo[index];
            // metadata of the sample
            out_bufferInfo.set(
                    info.offset,
                    info.size,
                    info.presentationTimeUs,
                    info.flags);
            result = true;
        }
        return result;
    }

    /**
     * Processes, releases and optionally renders the output buffer available at the head of the
     * queue. All observers are notified with a callback. See {@link
     * OutputSampleListener#outputSample(MediaCodecWrapper, MediaCodec.BufferInfo,
     * ByteBuffer, SurfaceTexture surfaceTexture)}
     *
     * @param render True, if the buffer is to be rendered on the {@link Surface} configured
     *
     */
    public void popSample(boolean render) {
        Log.d(TAG, "popSample");
        updateCodecState();
        if (!availableOutputBuffers.isEmpty()) {
            int index = availableOutputBuffers.remove();
            if (render && outputSampleListener != null) {
                ByteBuffer buffer = outputBuffers[index];
                MediaCodec.BufferInfo info = outputBufferInfo[index];
                outputSampleListener.outputSample(this, info, buffer, surfaceTexture);
            }
            decoder.releaseOutputBuffer(index, render);
        }
    }

    /**
     * Synchronize this object's state with the internal state of the wrapped
     * MediaCodec.
     */
    private void updateCodecState() {
        int index;
        while ((index = decoder.dequeueInputBuffer(0)) != MediaCodec.INFO_TRY_AGAIN_LATER) {
            availableInputBuffers.add(index);
        }

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while ((index = decoder.dequeueOutputBuffer(info, 0)) !=  MediaCodec.INFO_TRY_AGAIN_LATER) {
            switch (index) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    outputBuffers = decoder.getOutputBuffers();
                    outputBufferInfo = new MediaCodec.BufferInfo[outputBuffers.length];
                    availableOutputBuffers.clear();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    if (outputFormatChangedListener != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                outputFormatChangedListener
                                        .outputFormatChanged(MediaCodecWrapper.this,
                                                decoder.getOutputFormat());
                            }
                        });
                    }
                    break;
                default:
                    if(index >= 0) {
                        outputBufferInfo[index] = info;
                        availableOutputBuffers.add(index);
                    } else
                        throw new IllegalStateException("Unknown status from dequeueOutputBuffer");
                    break;
            }
        }
    }

    private class WriteException extends Throwable {
        private WriteException(final String detailMessage) {
            super(detailMessage);
        }
    }
}