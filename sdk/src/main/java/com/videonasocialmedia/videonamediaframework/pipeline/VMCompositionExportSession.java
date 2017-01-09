package com.videonasocialmedia.videonamediaframework.pipeline;

import com.videonasocialmedia.videonamediaframework.model.media.Video;

/**
 * Created by jca on 27/5/15.
 */
public interface VMCompositionExportSession {
    void export();

    /**
     * Created by jca on 27/5/15.
     */
    interface OnExportEndedListener {
        void onExportSuccess(Video video);
        void onExportProgress(String progressMsg);
        void onExportError(String error);
    }
}
