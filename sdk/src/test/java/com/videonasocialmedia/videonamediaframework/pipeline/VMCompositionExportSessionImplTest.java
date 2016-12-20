package com.videonasocialmedia.videonamediaframework.pipeline;

import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.videonasocialmedia.videonamediaframework.model.VMComposition;
import com.videonasocialmedia.videonamediaframework.model.media.Music;
import com.videonasocialmedia.videonamediaframework.model.media.Profile;
import com.videonasocialmedia.videonamediaframework.model.media.exceptions.IllegalItemOnTrack;
import com.videonasocialmedia.videonamediaframework.model.media.utils.VideoFrameRate;
import com.videonasocialmedia.videonamediaframework.model.media.utils.VideoQuality;
import com.videonasocialmedia.videonamediaframework.model.media.utils.VideoResolution;
import com.videonasocialmedia.videonamediaframework.muxer.Appender;
import com.videonasocialmedia.videonamediaframework.muxer.Trimmer;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Created by jliarte on 19/12/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class VMCompositionExportSessionImplTest {
  @Mock private VMCompositionExportSession.OnExportEndedListener mockedExportEndedListener;
  @Mock private Trimmer mockedAudioTrimmer;
  private final Profile profile = new Profile(VideoResolution.Resolution.HD720, VideoQuality.Quality.GOOD,
          VideoFrameRate.FrameRate.FPS25);
  @Mock private Appender mockedAppender;

//  @Test
//  public void addAudioAppendsNewTrackToMovie() throws Exception {
//    VMComposition vmComposition = new VMComposition();
//    VMCompositionExportSessionImpl exporter = new VMCompositionExportSessionImpl("tmp/path", vmComposition, profile,
//            mockedExportEndedListener);
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
    VMCompositionExportSessionImpl exporter = new VMCompositionExportSessionImpl("tmp/path", vmComposition, profile,
            mockedExportEndedListener);
    exporter.appender = mockedAppender;
    String videoPath = "video/path";
    ArrayList<String> videoList = new ArrayList<>(Arrays.asList(videoPath));
    Movie appendedMovie = new Movie();
    doReturn(appendedMovie).when(mockedAppender).appendVideos(videoList, true);

    Movie resultMovie = exporter.createMovieFromComposition(videoList);

    verify(mockedAppender).appendVideos(videoList, true);
    assertThat(resultMovie, is(appendedMovie));
  }

  @Test
  public void createMovieFromCompositionCallsAppenderWithoutAudioIfMusicIsSet()
          throws IOException, IllegalItemOnTrack {
    VMComposition vmComposition = new VMComposition();
    Music music = new Music("music/path", 1f);
    vmComposition.getAudioTracks().get(0).insertItem(music);
    VMCompositionExportSessionImpl exporter = new VMCompositionExportSessionImpl("tmp/path", vmComposition, profile,
            mockedExportEndedListener);
    exporter.appender = mockedAppender;
    URL url = Thread.currentThread().getContextClassLoader().getResource("test-pod.m4a");
    ArrayList<String> videoList = new ArrayList<>(Arrays.asList(url.getPath()));
    Movie appendedMovie = MovieCreator.build(url.getPath());
    doReturn(appendedMovie).when(mockedAppender).appendVideos(videoList, false);
    int originalNumberOfTracks = appendedMovie.getTracks().size();

    Movie resultMovie = exporter.createMovieFromComposition(videoList);

    verify(mockedAppender).appendVideos(videoList, false);
    assertThat(resultMovie, is(appendedMovie));
    assertThat(resultMovie.getTracks().size(), is(originalNumberOfTracks));
  }

}