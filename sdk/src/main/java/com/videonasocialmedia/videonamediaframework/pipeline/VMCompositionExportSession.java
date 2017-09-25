package com.videonasocialmedia.videonamediaframework.pipeline;

import com.videonasocialmedia.videonamediaframework.model.media.Video;

/**
 * VMComposition Export Session exports a VMComposition to a file in disk.
 */
public interface VMCompositionExportSession {
    int EXPORT_STAGE_WAIT_FOR_TRANSCODING = 0;
    int EXPORT_STAGE_JOIN_VIDEOS = 1;
    int EXPORT_STAGE_WRITE_VIDEO_TO_DISK = 2;
    int EXPORT_STAGE_ADD_AUDIO_TRACKS = 3;
    int EXPORT_STAGE_MIX_AUDIO = 4;
    int EXPORT_STAGE_APPLY_AUDIO_MIXED = 5;
    int EXPORT_STAGE_APPLY_WATERMARK = 6;

  void exportAsyncronously();

    void export();

    /**
     * Created by jca on 27/5/15.
     */
    interface ExportListener {
        void onExportSuccess(Video video);
        void onExportProgress(String progressMsg, int exportStage);
        void onExportError(String error);
    }
}
