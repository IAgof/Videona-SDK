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

public interface VideonaDecoder {

    /**
     * Update outputSurface.
     *
     * @param outputSurface
     */
    void setOutSurface(OutputSurface outputSurface);


    /**
     * Go to exact time in Decoder
     *
     * Advance mediaExtractor to previous I frame sync, and advance to specific time.
     *
     * @param timeMs
     */
    void seekTo(long timeMs);

    /**
     * Initialize and start Decoder
     */
    void setup();

    /**
     * Stop and release decoder and outputSurface
     */
    void release();

    /**
     *
     * Advance decoder
     *
     * @return false if there is not more info to decode.
     */
    boolean stepPipeline();

    int drainDecoder(long i);

    int drainExtractor(long timeoutUs);

    boolean isRender();
}
