/*
 * Copyright (C) 2018 Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 */

package com.videonasocialmedia.ffmpeg;

import java.io.File;
import java.util.List;

class VideoCommand implements Command {
    private static final String FFMPEG_PROGRAM_NAME = "ffmpeg";

    private final List<String> arguments;
    private final String outputPath;
    private VideoKit videoKit = null;
    private VideoKitInvoke videoKitInvoke = null;

    VideoCommand(List<String> flags, String outputPath, VideoKit videoKit) {
        this.arguments = flags;
        this.outputPath = outputPath;
        this.videoKit = videoKit;

    }

    VideoCommand(List<String> flags, String outputPath, VideoKitInvoke videoKitInvoke) {
        this.arguments = flags;
        this.outputPath = outputPath;
        this.videoKitInvoke = videoKitInvoke;

    }

    @Override
    public VideoProcessingResult execute() {
        int returnCode = 0;
        if (videoKit != null) {
            videoKit.process(getArgumentsAsArray());
        } else {
            videoKitInvoke.process(videoKitInvoke.getLibPath(), getArgumentsAsArray());
        }
        if (returnCode == VideoProcessingResult.SUCCESSFUL_RESULT) {
            return new VideoProcessingResult(returnCode, outputPath);
        } else {
            deleteOutput();
            return new VideoProcessingResult(returnCode, null);
        }
    }

    private String[] getArgumentsAsArray() {
        final String ffmpegArguments[] = new String[arguments.size() + 1];
        for (int i = 0; i < arguments.size(); i++) {
            ffmpegArguments[i + 1] = arguments.get(i);
        }

        ffmpegArguments[0] = FFMPEG_PROGRAM_NAME;

        return ffmpegArguments;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteOutput() {
        final File output = new File(outputPath);
        if (output.exists()) {
            output.delete();
        }
    }
}
