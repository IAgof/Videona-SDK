package com.videonasocialmedia.videonamediaframework.pipeline;

import android.media.MediaMetadataRetriever;

import com.videonasocialmedia.integration.AssetManagerAndroidTest;
import com.videonasocialmedia.videonamediaframework.model.VMComposition;
import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.model.media.Profile;
import com.videonasocialmedia.videonamediaframework.model.media.Video;
import com.videonasocialmedia.videonamediaframework.model.media.exceptions.IllegalItemOnTrack;
import com.videonasocialmedia.videonamediaframework.model.media.utils.VideoFrameRate;
import com.videonasocialmedia.videonamediaframework.model.media.utils.VideoQuality;
import com.videonasocialmedia.videonamediaframework.model.media.utils.VideoResolution;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Created by jliarte on 22/09/17.
 */
public class VMCompositionExportSessionImplInstrumentationTest extends AssetManagerAndroidTest {

  private String testPath;
  @Mock private VMCompositionExportSession.ExportListener mockedExportListener;
  @Mock private TranscoderHelper mockedTranscoderHelper;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    testPath = getInstrumentation().getTargetContext().getExternalCacheDir()
            .getAbsolutePath();
  }

  @Test
  public void testExportDoesntMixAudioWithJustOneVideoAndDefaultVolume()
          throws IllegalItemOnTrack, IOException {
    String originalVideoPath = getAssetPath("vid_.mp4");
    Video video = new Video(originalVideoPath, Video.DEFAULT_VOLUME);
    Profile profile = new Profile(VideoResolution.Resolution.HD720, VideoQuality.Quality.GOOD,
            VideoFrameRate.FrameRate.FPS30);
    VMComposition composition = new VMComposition(null, profile);
    composition.getMediaTrack().insertItem(video);
//    int originalVideoDuration = Integer.parseInt(getVideoDuration(originalVideoPath));
    String tempFilesDirectory = testPath + "/tmp/";
    VMCompositionExportSessionImpl exportSession = new VMCompositionExportSessionImpl(composition,
            testPath, tempFilesDirectory, tempFilesDirectory, mockedExportListener);
    exportSession.transcoderHelper = mockedTranscoderHelper;

    exportSession.export();

    verify(mockedTranscoderHelper, never()).generateTempFileMixAudio(
            ArgumentMatchers.<Media>anyList(), anyString(), anyString(), anyLong());
    ArgumentCaptor<Object> videoCaptor = ArgumentCaptor.forClass(Video.class);
    verify(mockedExportListener).onExportSuccess((Video) videoCaptor.capture());
    Video exportedVideo = (Video) videoCaptor.getValue();
    assertThat(exportedVideo.getMediaPath(), not(startsWith(tempFilesDirectory)));
    assertThat(exportedVideo.getMediaPath(), not(containsString("V_Appended.mp4")));
  }

  @Test
  public void testExportMixAudioWithJustOneVideoAndNotDefaultVolume()
          throws IllegalItemOnTrack, IOException {
    String originalVideoPath = getAssetPath("vid_.mp4");
    Video video = new Video(originalVideoPath, 0.5f);
    Profile profile = new Profile(VideoResolution.Resolution.HD720, VideoQuality.Quality.GOOD,
            VideoFrameRate.FrameRate.FPS30);
    VMComposition composition = new VMComposition(null, profile);
    composition.getMediaTrack().insertItem(video);
//    int originalVideoDuration = Integer.parseInt(getVideoDuration(originalVideoPath));
    String tempFilesDirectory = testPath + "/tmp/";
    VMCompositionExportSessionImpl exportSession = new VMCompositionExportSessionImpl(composition,
            testPath, tempFilesDirectory, tempFilesDirectory, mockedExportListener);
    exportSession.transcoderHelper = mockedTranscoderHelper;

    exportSession.export();

    verify(mockedTranscoderHelper).generateTempFileMixAudio(
            ArgumentMatchers.<Media>anyList(), anyString(), anyString(), anyLong());
    ArgumentCaptor<Object> videoCaptor = ArgumentCaptor.forClass(Video.class);
    verify(mockedExportListener).onExportSuccess((Video) videoCaptor.capture());
    Video exportedVideo = (Video) videoCaptor.getValue();
    assertThat(exportedVideo.getMediaPath(), not(startsWith(tempFilesDirectory)));
    assertThat(exportedVideo.getMediaPath(), not(containsString("V_Appended.mp4")));
  }

  private String getVideoDuration(String videoPath) {
    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
    mediaMetadataRetriever.setDataSource(videoPath);
    return mediaMetadataRetriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
  }

}