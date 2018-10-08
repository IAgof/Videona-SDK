/*
 * Copyright (C) 2018 Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 */

package com.videonasocialmedia.ffmpeg;

public class VideoKit {
    static {
        try {
            System.loadLibrary("avutil");
            System.loadLibrary("swresample");
            System.loadLibrary("avcodec");
            System.loadLibrary("avformat");
            System.loadLibrary("swscale");
            System.loadLibrary("avfilter");
            System.loadLibrary("avdevice");
            System.loadLibrary("videokit");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    private LogLevel logLevel = LogLevel.FULL;

    public void setLogLevel(LogLevel level) {
        logLevel = level;
    }

    int process(String[] args) {
        return run(logLevel.getValue(), args);
    }

    private native int run(int loglevel, String[] args);

    public CommandBuilder createCommand() {
        return new VideoCommandBuilder(this);
    }
}
