package com.videonasocialmedia.sdk.decoder;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.opengl.GLSurfaceView;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Veronica Lago Fominaya on 04/04/2016.
 */
public class VideonaDecoder implements Decoder, Runnable, MediaCodecWrapper.OutputSampleListener {

    private static final String TAG = "VideonaDecoder";
    private VideonaDecoderListener videonaDecoderListener;
    private MediaExtractor extractor;
    private MediaCodecWrapper mediaCodecWrapper;
    private String inputSourcePath;
    private SurfaceTexture outputSurface;
    private final Object pauseLock = new Object();        // guards pause/running
    private boolean running;
    private boolean paused;
    private boolean finished;
    private boolean isDecodingForRequestedFrame;
    private int numFramesToRequestedFrame = 0;

    public VideonaDecoder(VideonaDecoderListener listener) {
        this.videonaDecoderListener = listener;
    }

    @Override
    public void setOutputSurface(GLSurfaceView textureView) {
        //TODO define this output surface correctly
        outputSurface = new SurfaceTexture(textureView.getId());
    }

    @Override
    public void inputSource(String path) {
        inputSourcePath = path;
    }

    @Override
    public void start() {
        extractor = new MediaExtractor();
        final ParcelFileDescriptor parcelFileDescriptor;
        final FileDescriptor fileDescriptor;
        File source = new File(inputSourcePath);
        try {
            parcelFileDescriptor = ParcelFileDescriptor.open(source,
                    ParcelFileDescriptor.MODE_READ_ONLY);
            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            extractor.setDataSource(fileDescriptor);
            int numOfTracks = extractor.getTrackCount();
            // Begin by unselecting all of the tracks in the extractor, so we won't see
            // any tracks that we haven't explicitly selected.
            for (int i = 0; i < numOfTracks; ++i) {
                extractor.unselectTrack(i);
            }
            selectTracks(numOfTracks);
            if(mediaCodecWrapper != null)
                mediaCodecWrapper.setOutputSampleListener(this);
            startDecodeThread();
        } catch (IOException e) {
            Log.w("Could not open '" + source.getAbsolutePath() + "'", e);
        }
    }

    private void selectTracks(int numOfTracks) throws IOException {
        for (int i = 0; i < numOfTracks; ++i) {
            mediaCodecWrapper = MediaCodecWrapper.fromVideoFormat(extractor.getTrackFormat(i),
                    outputSurface);
            if (mediaCodecWrapper != null) {
                extractor.selectTrack(i);
                break;
            }
        }
    }

    private void startDecodeThread() {
        synchronized (pauseLock) {
            if (running) {
                Log.w(TAG, "Decoder thread running when start requested");
                return;
            }
            running = true;
            new Thread(this, "VideonaDecoder").start();
        }
    }

    @Override
    public void outputSample(MediaCodecWrapper sender, MediaCodec.BufferInfo info, ByteBuffer buffer,
                             SurfaceTexture surfaceTexture) {
        if(!isDecodingForRequestedFrame)
            videonaDecoderListener.onFrameAvailable(surfaceTexture);
    }

    @Override
    public void pause() {
        synchronized (pauseLock) {
            paused = true;
        }
    }

    @Override
    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notify();
        }
    }

    @Override
    public void stop() {
        finished = true;
        pauseLock.notify();
        mediaCodecWrapper.stop();
    }

    @Override
    public void seekTo(long time) {
        pause();
        extractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        numFramesToRequestedFrame = 0;
        while(extractor.getSampleTime() < time) {
            extractor.advance();
            numFramesToRequestedFrame++;
        }
        extractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        resume();
    }

    @Override
    public void release() {
        extractor.release();
        extractor = null;
        if(!finished) {
            mediaCodecWrapper.stop();
            finished = true;
            running = false;
        }
        mediaCodecWrapper.release();
    }

    @Override
    public void run() {
        while (!finished) {
            synchronized (pauseLock) {
                while (paused) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Error", e);
                    }
                }
                checkAvailableFrame();
            }
        }
    }

    private void checkAvailableFrame() {
        boolean isEos = ((extractor.getSampleFlags() & MediaCodec
                .BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM);

        if (!isEos) {
            boolean result = mediaCodecWrapper.writeSample(extractor, false,
                    extractor.getSampleTime(), extractor.getSampleFlags());
            if (result) {
                extractor.advance();
                if(isDecodingForRequestedFrame) {
                    numFramesToRequestedFrame--;
                    if (numFramesToRequestedFrame <= 1)
                        isDecodingForRequestedFrame = false;
                }
            }
        }
        MediaCodec.BufferInfo out_bufferInfo = new MediaCodec.BufferInfo();

        if (out_bufferInfo.size <= 0 && isEos) {
            stop();
            release();
            videonaDecoderListener.onFinished();
        }  else
            mediaCodecWrapper.popSample(true);
    }
}
