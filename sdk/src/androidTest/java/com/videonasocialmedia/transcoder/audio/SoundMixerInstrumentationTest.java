package com.videonasocialmedia.transcoder.audio;

import com.videonasocialmedia.integration.AssetManagerAndroidTest;
import com.videonasocialmedia.videonamediaframework.model.Constants;
import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.model.media.Video;

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
 * Created by jliarte on 3/10/17.
 */
public class SoundMixerInstrumentationTest extends AssetManagerAndroidTest {

  private String testPath;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    testPath = getInstrumentation().getTargetContext().getExternalCacheDir()
            .getAbsolutePath();
  }

  @Test
  public void testMixAudio() throws IOException {
    String audio1Path = getAssetPath("AUD1.pcm");
    String audio2Path = getAssetPath("AUD2.pcm");
    Video audio1 = new Video(audio1Path, Video.DEFAULT_VOLUME);
    Video audio2 = new Video(audio2Path, Video.DEFAULT_VOLUME);
    ArrayList<Media> mediaListDecoded = new ArrayList<>();
    mediaListDecoded.add(audio1);
    mediaListDecoded.add(audio2);
    SoundMixer soundMixer = new SoundMixer();
    String outputFilePath = testPath + File.separator + Constants.MIXED_AUDIO_FILE_NAME;

    soundMixer.mixAudio(mediaListDecoded, outputFilePath);

    File outputFile = new File(outputFilePath);
    assertThat(outputFile.exists(), is(true));
  }
}