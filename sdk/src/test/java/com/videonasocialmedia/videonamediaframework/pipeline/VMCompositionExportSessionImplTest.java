package com.videonasocialmedia.videonamediaframework.pipeline;

import android.support.annotation.NonNull;

import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.videonasocialmedia.transcoder.video.overlay.Image;
import com.videonasocialmedia.videonamediaframework.model.Constants;
import com.videonasocialmedia.videonamediaframework.model.VMComposition;
import com.videonasocialmedia.videonamediaframework.model.media.Music;
import com.videonasocialmedia.videonamediaframework.model.media.Profile;
import com.videonasocialmedia.videonamediaframework.model.media.Watermark;
import com.videonasocialmedia.videonamediaframework.model.media.exceptions.IllegalItemOnTrack;
import com.videonasocialmedia.videonamediaframework.model.media.utils.VideoFrameRate;
import com.videonasocialmedia.videonamediaframework.model.media.utils.VideoQuality;
import com.videonasocialmedia.videonamediaframework.model.media.utils.VideoResolution;
import com.videonasocialmedia.videonamediaframework.muxer.Appender;
import com.videonasocialmedia.videonamediaframework.muxer.Trimmer;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyByte;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by jliarte on 19/12/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class VMCompositionExportSessionImplTest {
  @Mock private VMCompositionExportSession.OnExportEndedListener mockedExportEndedListener;
  @Mock private Trimmer mockedAudioTrimmer;
  private final Profile profile = new Profile(VideoResolution.Resolution.HD720,
          VideoQuality.Quality.GOOD, VideoFrameRate.FrameRate.FPS25);
  @Mock private Appender mockedAppender;
  @Mock
  private Image mockedWatermark;

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
  public void createMovieFromCompositionCallsAppender() throws IOException {
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
  public void createMovieFromCompositionCallsAppenderWithoutAudioIfMusicIsSet()
          throws IOException, IllegalItemOnTrack {
    VMComposition vmComposition = new VMComposition();
    Music music = new Music("music/path", 1f, 0);
    vmComposition.getAudioTracks().get(Constants.INDEX_AUDIO_TRACKS_MUSIC).insertItem(music);
    VMCompositionExportSessionImpl exporter = getVmCompositionExportSession(vmComposition);
    VMCompositionExportSessionImpl exportSessionSpy = spy(exporter);
    exportSessionSpy.appender = mockedAppender;
    URL url = Thread.currentThread().getContextClassLoader().getResource("test-pod.m4a");
    ArrayList<String> videoList = new ArrayList<>(Arrays.asList(url.getPath()));
    Movie appendedMovie = MovieCreator.build(url.getPath());
    doReturn(appendedMovie).when(mockedAppender).appendVideos(videoList, false);
    doReturn(appendedMovie).when(exportSessionSpy)
            .addAudio(any(Movie.class), anyString(), anyByte());
    int originalNumberOfTracks = appendedMovie.getTracks().size();

    Movie resultMovie = exportSessionSpy.createMovieFromComposition(videoList);

    verify(mockedAppender).appendVideos(videoList, false);
    assertThat(resultMovie, is(appendedMovie));
    assertThat(resultMovie.getTracks().size(), is(originalNumberOfTracks));
  }

  @Test
  public void exportCallsCreateMovieFromComposition() throws IOException {
    VMComposition vmComposition = new VMComposition();
    VMCompositionExportSessionImpl vmCompositionExportSession =
            getVmCompositionExportSession(vmComposition);
    VMCompositionExportSessionImpl exportSessionSpy = spy(vmCompositionExportSession);
    doCallRealMethod().when(exportSessionSpy).export();
    doNothing().when(exportSessionSpy).saveFinalVideo(any(Movie.class), anyString());

    exportSessionSpy.export();

    verify(exportSessionSpy).createMovieFromComposition((ArrayList<String>) any(ArrayList.class));
  }

  @Test
  public void exportDoesNotCallMixAudioIfNoMusicInComposition() throws IOException {
    VMComposition vmComposition = new VMComposition();
    assert ! vmComposition.hasMusic();
    VMCompositionExportSessionImpl vmCompositionExportSession =
            getVmCompositionExportSession(vmComposition);
    VMCompositionExportSessionImpl exportSessionSpy = spy(vmCompositionExportSession);
    doReturn(new Movie()).when(exportSessionSpy).createMovieFromComposition((ArrayList<String>) any(ArrayList.class));
    doNothing().when(exportSessionSpy).saveFinalVideo(any(Movie.class), anyString());

    exportSessionSpy.export();

    verify(exportSessionSpy, never()).mixAudio();
  }

  @Ignore // Ignore until know what to do if composition have music and voice over
  @Test
  public void exportCallsMixAudioIfMusicVolumeIsLowerThan1()
          throws IOException, IllegalItemOnTrack {
    VMComposition vmComposition = new VMComposition();
    Music voiceOver = new Music("music/path", 0.8f, 0);
    vmComposition.getAudioTracks().get(Constants.INDEX_AUDIO_TRACKS_VOICE_OVER).insertItem(voiceOver);
    VMCompositionExportSessionImpl vmCompositionExportSession =
            getVmCompositionExportSession(vmComposition);
    VMCompositionExportSessionImpl exportSessionSpy = spy(vmCompositionExportSession);
    doReturn(new Movie()).when(exportSessionSpy)
            .createMovieFromComposition((ArrayList<String>) any(ArrayList.class));
    doNothing().when(exportSessionSpy).saveFinalVideo(any(Movie.class), anyString());
    doNothing().when(exportSessionSpy).mixAudio();
    doCallRealMethod().when(exportSessionSpy).export();

    exportSessionSpy.export();

    verify(exportSessionSpy).mixAudio();
  }

  @Test
  public void exportDoesNotCallMixAudioIfMusicVolumeIs1()
          throws IllegalItemOnTrack, IOException {
    VMComposition vmComposition = new VMComposition();
    Music music = new Music("music/path", 1f, 0);
    vmComposition.getAudioTracks().get(Constants.INDEX_AUDIO_TRACKS_MUSIC).insertItem(music);
    VMCompositionExportSessionImpl vmCompositionExportSession =
            getVmCompositionExportSession(vmComposition);
    VMCompositionExportSessionImpl exportSessionSpy = spy(vmCompositionExportSession);
    doReturn(new Movie()).when(exportSessionSpy).createMovieFromComposition(any(ArrayList.class));
    doNothing().when(exportSessionSpy).saveFinalVideo(any(Movie.class), anyString());
    doNothing().when(exportSessionSpy).mixAudio();

    exportSessionSpy.export();

    verify(exportSessionSpy, never()).mixAudio();
  }

  @Test
  public void createMovieFromCompositionCallsAppenderWithOriginalVideoMusicIfCompositionHasNotMusic()
          throws IOException {
    VMComposition vmComposition = new VMComposition();
    assert ! vmComposition.hasMusic();
    VMCompositionExportSessionImpl vmCompositionExportSession =
            getVmCompositionExportSession(vmComposition);
    vmCompositionExportSession.appender = mockedAppender;
    ArrayList<String> videoPaths = new ArrayList<>();

    vmCompositionExportSession.createMovieFromComposition(videoPaths);

    verify(mockedAppender).appendVideos(videoPaths, true);
  }

  @Test
  public void createMovieFromCompositionAppenderWithoutOriginalVideoMusicIfCompositionHasMusic()
          throws IllegalItemOnTrack, IOException {
    VMComposition vmComposition = new VMComposition();
    Music music = new Music("music/path", 0);
    assert music.getVolume() == 0.5f; // default music volume 0.5f
    // set music to 1f, exporter swap audio, not mixed
    music.setVolume(1f);
    vmComposition.getAudioTracks().get(Constants.INDEX_AUDIO_TRACKS_MUSIC).insertItem(music);
    assert vmComposition.hasMusic();
    VMCompositionExportSessionImpl vmCompositionExportSession =
            getVmCompositionExportSession(vmComposition);
    VMCompositionExportSessionImpl exportSessionSpy = spy(vmCompositionExportSession);
    exportSessionSpy.appender = mockedAppender;
    doReturn(2.0).when(exportSessionSpy).getMovieDuration(any(Movie.class));
    doReturn(new Movie()).when(exportSessionSpy).addAudio(any(Movie.class), anyString(), anyByte());
    ArrayList<String> videoPaths = new ArrayList<>();

    exportSessionSpy.createMovieFromComposition(videoPaths);

    verify(mockedAppender).appendVideos(videoPaths, false);
  }

  @Test
  public void createMovieFromCompositionCallsAppenderWithOriginalVideoMusicIfCompositionMusicVolumeLowerThan1()
          throws IllegalItemOnTrack, IOException {
    VMComposition vmComposition = new VMComposition();
    Music voiceOver = new Music("voice/over/path", 0.9f, 0);
    vmComposition.getAudioTracks().get(Constants.INDEX_AUDIO_TRACKS_MUSIC).insertItem(voiceOver);
    VMCompositionExportSessionImpl vmCompositionExportSession =
            getVmCompositionExportSession(vmComposition);
    vmCompositionExportSession.appender = mockedAppender;
    ArrayList<String> videoPaths = new ArrayList<>();

    vmCompositionExportSession.createMovieFromComposition(videoPaths);

    verify(mockedAppender).appendVideos(videoPaths, true);
  }

  @Test
  public void exportCallsAddWatermarkIfWatermarkIsSelectedInComposition() throws IOException {
    VMComposition vmComposition = new VMComposition();
    vmComposition.setWatermarkActivated(true);

    assertThat("Watermark is activated", vmComposition.hasWatermark(), is(true));

    VMCompositionExportSessionImpl vmCompositionExportSession =
        getVmCompositionExportSession(vmComposition);
    VMCompositionExportSessionImpl exportSessionSpy = spy(vmCompositionExportSession);
    doReturn(new Movie()).when(exportSessionSpy).createMovieFromComposition((ArrayList<String>) any(ArrayList.class));
    doNothing().when(exportSessionSpy).saveFinalVideo(any(Movie.class), anyString());

    exportSessionSpy.export();

    verify(exportSessionSpy).addWatermarkToGeneratedVideo(any(Watermark.class), anyString());
  }

  @Test
  public void exportDoesNotCallsAddWatermarkIfWatermarkIsNotSelectedInComposition() throws IOException {
    VMComposition vmComposition = new VMComposition();
    vmComposition.setWatermarkActivated(false);

    assertThat("Watermark is not activated", vmComposition.hasWatermark(), is(false));

    VMCompositionExportSessionImpl vmCompositionExportSession =
        getVmCompositionExportSession(vmComposition);
    VMCompositionExportSessionImpl exportSessionSpy = spy(vmCompositionExportSession);
    doReturn(new Movie()).when(exportSessionSpy).createMovieFromComposition((ArrayList<String>) any(ArrayList.class));
    doNothing().when(exportSessionSpy).saveFinalVideo(any(Movie.class), anyString());

    exportSessionSpy.export();

    verify(exportSessionSpy, never()).addWatermarkToGeneratedVideo(any(Watermark.class), anyString());
  }

  @NonNull
  private VMCompositionExportSessionImpl getVmCompositionExportSession(VMComposition vmComposition) {
    return new VMCompositionExportSessionImpl(vmComposition, "result/path",
            "temp/path", mockedExportEndedListener);
  }

}