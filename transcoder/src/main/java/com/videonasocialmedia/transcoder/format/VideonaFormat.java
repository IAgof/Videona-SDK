package com.videonasocialmedia.transcoder.format;
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
import android.util.Log;

import com.videonasocialmedia.transcoder.exceptions.OutputFormatUnavailableException;

public class VideonaFormat implements MediaFormatStrategy {

    public static final int AUDIO_BITRATE_AS_IS = -1;
    public static final int AUDIO_CHANNELS_AS_IS = -1;

    private static final String TAG = "VideonaFormat";
    private static final int DEFAULT_BITRATE = 5000 * 1000;
    private final int DEFAULT_FRAME_RATE = 30;
    private final int DEFAULT_KEY_I_FRAME = 1;

    private int videoWidth = 1280;
    private int videoHeight = 720;
    private int videoFrameRate = 30;
    private int videoBitrate = 5000 * 1000;
    private int audioBitrate = 192 * 1000;
    private int audioChannels = 2;

    public VideonaFormat(){
        this(DEFAULT_BITRATE);
    }

    public VideonaFormat(int videoBitrate){
        this(videoBitrate, AUDIO_BITRATE_AS_IS, AUDIO_CHANNELS_AS_IS);
    }

    public VideonaFormat(int videoWidth, int videoHeight){
        this(videoWidth, videoHeight, AUDIO_BITRATE_AS_IS, AUDIO_CHANNELS_AS_IS);
    }

    public VideonaFormat(int videoBitrate, int width, int height, int audioBitrate, int audioChannels){

        this.videoBitrate = videoBitrate;
        this.videoWidth = width;
        this.videoHeight = height;
        this.audioBitrate = audioBitrate;
        this.audioChannels = audioChannels;
    }

    public VideonaFormat(int videoBitrate, int audioBitrate, int audioChannels){
        this.videoBitrate = videoBitrate;
        this.audioBitrate = audioBitrate;
        this.audioChannels = audioChannels;
    }

    public VideonaFormat(int videoWidth, int videoHeight, int audioBitrate, int audioChannels){
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.audioBitrate = audioBitrate;
        this.audioChannels = audioChannels;
    }

    @Override
    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
        int width = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        int longer, shorter, outWidth, outHeight;
        if (width >= height) {
            longer = width;
            shorter = height;
            outWidth = videoWidth;
            outHeight = videoHeight;
        } else {
            shorter = width;
            longer = height;
            outWidth = videoHeight;
            outHeight = videoWidth;
        }
        if (longer * 9 != shorter * 16) {
            throw new OutputFormatUnavailableException("This video is not 16:9, and is not able to transcode. (" + width + "x" + height + ")");
        }

        MediaFormat format = MediaFormat.createVideoFormat("video/avc", outWidth, outHeight);
        format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, DEFAULT_FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, DEFAULT_KEY_I_FRAME);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        return format;
    }

    @Override
    public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
        if (audioBitrate == AUDIO_BITRATE_AS_IS || audioChannels == AUDIO_CHANNELS_AS_IS) return null;

        // Use original sample rate, as resampling is not supported yet.
        final MediaFormat format = MediaFormat.createAudioFormat(MediaFormatExtraConstants.MIMETYPE_AUDIO_AAC,
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE), audioChannels);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate);
        return format;
    }
}
