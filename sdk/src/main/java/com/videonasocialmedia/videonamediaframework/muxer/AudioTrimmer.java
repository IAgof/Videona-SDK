package com.videonasocialmedia.videonamediaframework.muxer;

import org.mp4parser.muxer.Movie;

import java.io.IOException;

/**
 * Created by Veronica Lago Fominaya on 25/06/2015.
 */
public class AudioTrimmer extends Trimmer {

    @Override
    public Movie trim(String audioPath, double startTime, double endTime) throws IOException {
        return super.trim(audioPath, startTime, endTime);
    }
}
