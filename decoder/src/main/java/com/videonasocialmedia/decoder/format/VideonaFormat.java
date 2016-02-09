package com.videonasocialmedia.decoder.format;
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

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.videonasocialmedia.decoder.exceptions.OutputFormatUnavailableException;

public class VideonaFormat implements MediaFormatStrategy {

    private static final String TAG = "VideonaFormat";
    private static final int LONGER_LENGTH = 1280;
    private static final int SHORTER_LENGTH = 720;
    private static final int DEFAULT_BITRATE = 5000 * 1000;
    private final int DEFAULT_FRAME_RATE = 30;
    private final int DEFAULT_KEY_I_FRAME = 1;


    @Override
    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
        int width = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        int longer, shorter, outWidth, outHeight;
        if (width >= height) {
            longer = width;
            shorter = height;
            outWidth = LONGER_LENGTH;
            outHeight = SHORTER_LENGTH;
        } else {
            shorter = width;
            longer = height;
            outWidth = SHORTER_LENGTH;
            outHeight = LONGER_LENGTH;
        }
        if (longer * 9 != shorter * 16) {
            throw new OutputFormatUnavailableException("This video is not 16:9, and is not able to transcode. (" + width + "x" + height + ")");
        }
       /* Passthrough del vídeo.
        if (shorter <= SHORTER_LENGTH) {
            Log.d(TAG, "This video is less or equal to 720p, pass-through. (" + width + "x" + height + ")");
            return null;
        } */

        MediaFormat format = MediaFormat.createVideoFormat("video/avc", outWidth, outHeight);
        format.setInteger(MediaFormat.KEY_BIT_RATE, DEFAULT_BITRATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, DEFAULT_FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, DEFAULT_KEY_I_FRAME);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        return format;
    }

    @Override
    public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
        return null;
    }
}
