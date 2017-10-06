package com.videonasocialmedia.transcoder.audio;

import android.media.MediaMetadataRetriever;

import com.videonasocialmedia.integration.AssetManagerAndroidTest;
import com.videonasocialmedia.transcoder.TranscodingException;
import com.videonasocialmedia.videonamediaframework.model.Constants;
import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.model.media.Music;
import com.videonasocialmedia.videonamediaframework.model.media.Video;
import com.videonasocialmedia.videonamediaframework.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by jliarte on 2/10/17.
 */
public class AudioMixerInstrumentationTest extends AssetManagerAndroidTest {

  private String testPath;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    testPath = getInstrumentation().getTargetContext().getExternalCacheDir()
            .getAbsolutePath();
  }

  @Test
  public void testAudioMixing() throws IOException, TranscodingException {
    ArrayList<Media> mediaList = new ArrayList<>();
    String videoPath = getAssetPath("longvid.mp4");
    String musicPath = getAssetPath("music.m4a");
    Video video = new Video(videoPath, Video.DEFAULT_VOLUME);
    Music audio = new Music(musicPath, 146000);
    mediaList.add(video);
    mediaList.add(audio);
    String outputFile = testPath + File.separator + Constants.MIXED_AUDIO_FILE_NAME;
    int videoDuration = Integer.parseInt(getVideoDuration(videoPath));
//    videoDuration = 10000;
    String tempDirectory = testPath + File.separator + "mixed";
    FileUtils.createDirectory(tempDirectory);
    AudioMixer mixer = new AudioMixer();

    mixer.export(mediaList, tempDirectory, outputFile, videoDuration);

    File mixedFile = new File(outputFile);
    assertThat(mixedFile.exists(), is(true));
    int mixedFileDuration = Integer.parseInt(getVideoDuration(outputFile));
    assertThat(mixedFileDuration, is(videoDuration));
  }

  private String getVideoDuration(String videoPath) {
    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
    mediaMetadataRetriever.setDataSource(videoPath);
    return mediaMetadataRetriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
  }


}