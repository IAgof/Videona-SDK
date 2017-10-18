package com.videonasocialmedia.videonamediaframework.pipeline;

import android.graphics.drawable.Drawable;

import com.google.common.util.concurrent.ListenableFuture;
import com.videonasocialmedia.transcoder.video.format.VideonaFormat;
import com.videonasocialmedia.videonamediaframework.model.media.Video;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

/**
 * Created by jliarte on 19/09/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class TranscoderHelperTest {
  @Mock private Drawable mockedDrawable;

  @InjectMocks TranscoderHelper injectedTranscoderHelper;

  @Before
  public void initTestDoubles() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void updateIntermediateFileCallsGenerateOutputVideoWithOverlayImageAndTrimmingAsyncIfTextAndTrim()
          throws IOException {
    Video video = new Video("media/path", Video.DEFAULT_VOLUME);
    VideonaFormat videoFormat = new VideonaFormat();
    TranscoderHelper transcoderHelperSpy = Mockito.spy(injectedTranscoderHelper);
    ListenableFuture<Video> mockedTask = Mockito.mock(ListenableFuture.class);
    doReturn(mockedTask).when(transcoderHelperSpy).generateOutputVideoWithOverlayImageAndTrimmingAsync(
            mockedDrawable, false, false, video, videoFormat, "tmp/dir");
    video.setClipText("lala");
    video.setTrimmedVideo(true);

    transcoderHelperSpy.updateIntermediateFile(mockedDrawable, false, false, video, videoFormat,
            "tmp/dir");

    Mockito.verify(transcoderHelperSpy).generateOutputVideoWithOverlayImageAndTrimmingAsync(
            mockedDrawable, false, false, video, videoFormat, "tmp/dir");
  }

  @Test
  public void updateIntermediateFileCallsGenerateOutputVideoWithOverlayImageAsyncIfText()
          throws IOException {
    Video video = new Video("media/path", Video.DEFAULT_VOLUME);
    VideonaFormat videoFormat = new VideonaFormat();
    TranscoderHelper transcoderHelperSpy = Mockito.spy(injectedTranscoderHelper);
    ListenableFuture<Video> mockedTask = Mockito.mock(ListenableFuture.class);
    doReturn(mockedTask).when(transcoderHelperSpy).generateOutputVideoWithOverlayImageAsync(
            mockedDrawable, false, false, video, videoFormat, "tmp/dir");
    video.setClipText("lala");
    video.setTrimmedVideo(false);

    transcoderHelperSpy.updateIntermediateFile(mockedDrawable, false, false, video, videoFormat,
            "tmp/dir");

    Mockito.verify(transcoderHelperSpy).generateOutputVideoWithOverlayImageAsync(
            mockedDrawable, false, false, video, videoFormat, "tmp/dir");
  }

  @Test
  public void updateIntermediateFileCallsGenerateOutputVideoWithTrimmingAsyncIfNoText()
          throws IOException {
    Video video = new Video("media/path", Video.DEFAULT_VOLUME);
    VideonaFormat videoFormat = new VideonaFormat();
    TranscoderHelper transcoderHelperSpy = Mockito.spy(injectedTranscoderHelper);
    ListenableFuture<Video> mockedTask = Mockito.mock(ListenableFuture.class);
    doReturn(mockedTask).when(transcoderHelperSpy).generateOutputVideoWithTrimmingAsync(
            mockedDrawable, false, false, video, videoFormat, "tmp/dir");
    video.setTrimmedVideo(false);

    transcoderHelperSpy.updateIntermediateFile(mockedDrawable, false, false, video, videoFormat,
            "tmp/dir");

    Mockito.verify(transcoderHelperSpy).generateOutputVideoWithTrimmingAsync(
            mockedDrawable, false, false, video, videoFormat, "tmp/dir");

    video.setTrimmedVideo(true);

    transcoderHelperSpy.updateIntermediateFile(mockedDrawable, false, false, video, videoFormat,
            "tmp/dir");

    Mockito.verify(transcoderHelperSpy, times(2)).generateOutputVideoWithTrimmingAsync(
            mockedDrawable, false, false, video, videoFormat, "tmp/dir");
  }
}