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

import android.media.MediaFormat;

public interface VideonaTrack {

    void setup();

    /**
     * Get actual MediaFormat which is used to write to muxer.
     * To determine you should call {@link #stepPipeline()} several times.
     *
     * @return Actual output format determined by coder, or {@code null} if not yet determined.
     */
    MediaFormat getDeterminedFormat();

    /**
     * Step pipeline if output is available in any step of it.
     * It assumes muxer has been started, so you should call muxer.start() first.
     *
     * @return true if data moved in pipeline.
     */
    boolean stepPipeline();

    /**
     * Get presentation time of last sample written to muxer.
     *
     * @return Presentation time in micro-second. Return value is undefined if finished writing.
     */
    long getWrittenPresentationTimeUs();

    boolean isFinished();

    void release();

    /**
     * Time to advance transcoder to start, milliseconds
     * @param timeMs
     */
    void advanceStart(long timeMs);

    /**
     * End time of file to decode. End of trimming file
     * @param timeMs
     */
    void endOfDecoder(long timeMs);

    /**
     * End of videos encoded
     */
    boolean isEncodedFinished();
}
