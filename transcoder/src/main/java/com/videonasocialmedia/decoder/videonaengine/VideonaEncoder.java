package com.videonasocialmedia.decoder.videonaengine;
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

public interface VideonaEncoder {


    /**
     * Initialize and start Encoder
     */
    void setup();

    /**
     * Stop and release encoder
     */
    void release();

    /**
     *
     * Advance encoder
     *
     * @return false if there is not more info to decode.
     */
    boolean stepPipeline();

    /**
     *
     * Finished of encoder, end of stream
     *
     * @return
     */
    boolean isFinished();

    int drainEncoder(long i);

    long getWrittenPresentationTimeUs();

    InputSurface getEncoderInputSurface();
}
