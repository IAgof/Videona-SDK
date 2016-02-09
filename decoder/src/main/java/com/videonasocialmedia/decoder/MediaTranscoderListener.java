package com.videonasocialmedia.decoder;
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

public interface MediaTranscoderListener {

    /**
     * Called to notify progress.
     *
     * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
     */
    void onTranscodeProgress(double progress);

    /**
     * Called when transcode completed.
     */
    void onTranscodeCompleted();

    /**
     * Called when transcode canceled.
     */
    void onTranscodeCanceled();

    /**
     * Called when transcode failed.
     *
     * @param exception Exception thrown from {@link MediaTranscoderEngine#transcodeVideo(String, MediaFormatStrategy)}.
     *                  Note that it IS NOT {@link java.lang.Throwable}. This means {@link java.lang.Error} won't be caught.
     */
    void onTranscodeFailed(Exception exception);
}
