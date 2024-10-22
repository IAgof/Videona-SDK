package com.videonasocialmedia.videonamediaframework.pipeline;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.videonasocialmedia.transcoder.video.overlay.Image;
import com.videonasocialmedia.videonamediaframework.model.Constants;
import com.videonasocialmedia.videonamediaframework.model.VMComposition;
import com.videonasocialmedia.videonamediaframework.model.media.Music;
import com.videonasocialmedia.videonamediaframework.model.media.Profile;
import com.videonasocialmedia.videonamediaframework.model.media.exceptions.IllegalItemOnTrack;
import com.videonasocialmedia.videonamediaframework.muxer.Appender;
import com.videonasocialmedia.videonamediaframework.muxer.IntermediateFileException;
import com.videonasocialmedia.videonamediaframework.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mp4parser.muxer.Movie;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by jliarte on 19/12/16.
 */
@RunWith(PowerMockRunner.class)
  @PrepareForTest({FileUtils.class, TranscoderHelper.class, Log.class})
public class VMCompositionExportSessionImplTest {
  @Mock private VMCompositionExportSession.ExportListener mockedExportEndedListener;
  @Mock private Appender mockedAppender;
  @Mock Image mockedWatermarkImage;

  @Mock Profile mockedProfile;
  @Mock Movie mockedMovie;
  @Mock ListenableFuture<Void> mockedListenableFutureVoid;
  @Mock ListenableFuture<Long> mockedListenableFutureLong;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    PowerMockito.mockStatic(Log.class);
  }

//  @Test
//  public void addAudioAppendsNewTrackToMovie() throws Exception {
//    VMComposition vmComposition = new VMComposition();
//    VMCompositionExportSessionImpl exporter = new VMCompositionExportSessionImpl("tmp/path",
//        vmComposition, profile, mockedExportEndedListener);
//    String resName = "test-pod.m4a";
//    URL url = Thread.currentThread().getContextClassLoader().getResource(resName);
//    String audioPath = url.getPath();
//    int movieDuration = 2;
//    Movie mockedMovie = Mockito.mock(Movie.class);
//
//    exporter.addAudio(mockedMovie, audioPath, movieDuration);
//
//    ArgumentCaptor<Track> trackCaptor = ArgumentCaptor.forClass(Track.class);
//    verify(mockedMovie).addTrack(trackCaptor.capture());
//    Track trackCaptorValue = ArgumentCaptor.getValue();
//    assertThat(trackCaptorValue, CoreMatchers.notNullValue());
//    assertThat(trackCaptorValue, instanceOf(AppendTrack.class));
//    assertThat(trackCaptorValue.getName(), containsString(resName));
//  }

  @Test
  public void createMovieFromCompositionCallsAppender() throws IOException,
          IntermediateFileException, ExecutionException, InterruptedException {
    VMComposition vmComposition = new VMComposition();
    VMCompositionExportSessionImpl exporter = getVmCompositionExportSession(vmComposition);
    VMCompositionExportSessionImpl exportSessionSpy = spy(exporter);
    exportSessionSpy.appender = mockedAppender;
    String videoPath = "video/path";
    ArrayList<String> videoList = new ArrayList<>(Arrays.asList(videoPath));
    Movie appendedMovie = new Movie();
    doReturn(appendedMovie).when(mockedAppender).appendVideos(videoList, true);
    doReturn(2.0).when(exportSessionSpy).getMovieDuration(any(Movie.class));

    Movie resultMovie = exportSessionSpy.createMovieFromComposition(videoList);

    verify(mockedAppender).appendVideos(videoList, true);
    assertThat(resultMovie, is(appendedMovie));
  }

  @Test
  public void createMovieFromCompositionNeverCallsAppenderWithoutAudioIfMusicIsSet()
          throws IOException, IllegalItemOnTrack, IntermediateFileException {
    VMComposition vmComposition = new VMComposition();
    Music music = new Music("music/path", 1f, 0);
    vmComposition.getAudioTracks().get(Constants.INDEX_AUDIO_TRACK_MUSIC).insertItem(music);
    VMCompositionExportSessionImpl exporter = getVmCompositionExportSession(vmComposition);
    VMCompositionExportSessionImpl exportSessionSpy = spy(exporter);
    exportSessionSpy.appender = mockedAppender;
    URL url = Thread.currentThread().getContextClassLoader().getResource("test-pod.m4a");
    ArrayList<String> videoList = new ArrayList<>(Arrays.asList(url.getPath()));

    verify(mockedAppender, never()).appendVideos(videoList, true);

  }


  @Test
  public void exportCallsCreateMovieFromComposition() throws IOException, IntermediateFileException,
      ExecutionException, InterruptedException, TimeoutException {
    VMComposition vmComposition = new VMComposition();
    VMCompositionExportSessionImpl vmCompositionExportSession =
            getVmCompositionExportSession(vmComposition);
    VMCompositionExportSessionImpl exportSessionSpy = spy(vmCompositionExportSession);
    PowerMockito.mockStatic(FileUtils.class);
    when(FileUtils.getDurationFileAsync(anyString())).thenReturn(mockedListenableFutureLong);
    PowerMockito.when(mockedListenableFutureLong.get(anyInt(), any(TimeUnit.class))).thenReturn((long) 55000);
    doCallRealMethod().when(exportSessionSpy).export(anyString());
    doNothing().when(exportSessionSpy).saveFinalVideo(any(Movie.class), anyString());

    exportSessionSpy.export(anyString());

    verify(exportSessionSpy).createMovieFromComposition((ArrayList<String>) any(ArrayList.class));
  }

  @Test
  public void createMovieFromCompositionCallsAppenderWithOriginalVideoIfCompositionHasNotMusicAndHasNotVoiceOver()
          throws IOException, IntermediateFileException, ExecutionException, InterruptedException {
    VMComposition vmComposition = new VMComposition();
    assert ! vmComposition.hasMusic();
    assert ! vmComposition.hasVoiceOver();
    VMCompositionExportSessionImpl vmCompositionExportSession =
            getVmCompositionExportSession(vmComposition);
    vmCompositionExportSession.appender = mockedAppender;
    ArrayList<String> videoPaths = new ArrayList<>();

    vmCompositionExportSession.createMovieFromComposition(videoPaths);

    verify(mockedAppender).appendVideos(videoPaths, true);
  }
//
//  @Test
//  public void createMovieFromCompositionAppenderWithOriginalVideoIfCompositionHasMusic()
//          throws IllegalItemOnTrack, IOException, IntermediateFileException, ExecutionException,
//          InterruptedException {
//    VMComposition vmComposition = new VMComposition();
//    Music music = new Music("music/path", 0);
//    assert music.getVolume() == 0.5f; // default music volume 0.5f
//    music.setVolume(1f);
//    vmComposition.getAudioTracks().get(Constants.INDEX_AUDIO_TRACK_MUSIC).insertItem(music);
//    assert vmComposition.hasMusic();
//    VMCompositionExportSessionImpl vmCompositionExportSession =
//            getVmCompositionExportSession(vmComposition);
//    VMCompositionExportSessionImpl exportSessionSpy = spy(vmCompositionExportSession);
//    exportSessionSpy.appender = mockedAppender;
//    doReturn(2.0).when(exportSessionSpy).getMovieDuration(any(Movie.class));
//    doReturn(mockedMovie).when(exportSessionSpy).addAudio(any(Movie.class), anyString(), anyByte());
//    ArrayList<String> videoPaths = new ArrayList<>();
//
//    exportSessionSpy.createMovieFromComposition(videoPaths);
//
//    verify(mockedAppender).appendVideos(videoPaths, true);
//  }

  @Test
  public void createMovieFromCompositionCallsAppenderWithOriginalVideoMusicIfCompositionMusicVolumeLowerThan1()
          throws IllegalItemOnTrack, IOException, IntermediateFileException, ExecutionException,
          InterruptedException {
    VMComposition vmComposition = new VMComposition();
    Music voiceOver = new Music("voice/over/path", 0.9f, 0);
    vmComposition.getAudioTracks().get(Constants.INDEX_AUDIO_TRACK_MUSIC).insertItem(voiceOver);
    VMCompositionExportSessionImpl vmCompositionExportSession =
            getVmCompositionExportSession(vmComposition);
    vmCompositionExportSession.appender = mockedAppender;
    ArrayList<String> videoPaths = new ArrayList<>();

    vmCompositionExportSession.createMovieFromComposition(videoPaths);

    verify(mockedAppender).appendVideos(videoPaths, true);
  }

  @Test
  public void exportCallsAddWatermarkIfWatermarkIsSelectedInComposition() throws IOException,
      IntermediateFileException, ExecutionException, InterruptedException, TimeoutException {
    String resourceWatermarkFilePath = "fakePath";
    VMComposition vmComposition = new VMComposition(resourceWatermarkFilePath, mockedProfile);
    vmComposition.setWatermarkActivated(true);

    assertThat("Watermark is activated", vmComposition.hasWatermark(), is(true));

    VMCompositionExportSessionImpl vmCompositionExportSession =
        getVmCompositionExportSession(vmComposition);
    VMCompositionExportSessionImpl exportSessionSpy = spy(vmCompositionExportSession);
    PowerMockito.mockStatic(FileUtils.class);
    PowerMockito.when(FileUtils.getDurationFileAsync(anyString())).thenReturn(mockedListenableFutureLong);
    PowerMockito.when(mockedListenableFutureLong.get(anyInt(), any(TimeUnit.class))).thenReturn((long) 55000);
    when(FileUtils.getDurationFile(Mockito.anyString())).thenReturn((long) 55);
    doReturn(mockedMovie).when(exportSessionSpy)
        .createMovieFromComposition((ArrayList<String>) any(ArrayList.class));
    doNothing().when(exportSessionSpy).saveFinalVideo(any(Movie.class), anyString());
    doReturn(mockedListenableFutureVoid).when(exportSessionSpy).addWatermark(anyString(),
        any(Image.class));
    doReturn(mockedWatermarkImage).when(exportSessionSpy).getWatermarkImage();

    exportSessionSpy.export(anyString());

    verify(exportSessionSpy).addWatermark(anyString(), any(Image.class));
  }

  @Test
  public void exportDoesNotCallsAddWatermarkIfWatermarkIsNotSelectedInComposition()
      throws IOException, IntermediateFileException, ExecutionException, InterruptedException, TimeoutException {
    VMComposition vmComposition = new VMComposition();
    vmComposition.setWatermarkActivated(false);

    assertThat("Watermark is not activated", vmComposition.hasWatermark(), is(false));

    VMCompositionExportSessionImpl vmCompositionExportSession =
        getVmCompositionExportSession(vmComposition);
    VMCompositionExportSessionImpl exportSessionSpy = spy(vmCompositionExportSession);
    PowerMockito.mockStatic(FileUtils.class);
    when(FileUtils.getDurationFileAsync(anyString())).thenReturn(mockedListenableFutureLong);
    PowerMockito.when(mockedListenableFutureLong.get(anyInt(), any(TimeUnit.class))).thenReturn((long) 55000);
    doReturn(mockedMovie).when(exportSessionSpy)
        .createMovieFromComposition((ArrayList<String>) any(ArrayList.class));
    doNothing().when(exportSessionSpy).saveFinalVideo(any(Movie.class), anyString());

    exportSessionSpy.export(anyString());

    verify(exportSessionSpy, never()).addWatermark(anyString(), any(Image.class));
  }

  @NonNull
  private VMCompositionExportSessionImpl getVmCompositionExportSession(VMComposition vmComposition) {
    return new VMCompositionExportSessionImpl(vmComposition, "result/path",
            "temp/path", "tmp/audio/path", mockedExportEndedListener);
  }

}