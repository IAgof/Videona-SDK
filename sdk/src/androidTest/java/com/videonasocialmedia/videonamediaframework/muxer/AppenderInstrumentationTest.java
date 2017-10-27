package com.videonasocialmedia.videonamediaframework.muxer;

import android.media.MediaMetadataRetriever;

import com.googlecode.mp4parser.authoring.Movie;
import com.videonasocialmedia.videonamediaframework.muxer.utils.Utils;
import com.videonasocialmedia.integration.AssetManagerAndroidTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by alvaro on 26/10/17.
 */

public class AppenderInstrumentationTest extends AssetManagerAndroidTest {

  private String testPath;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    testPath = getInstrumentation().getTargetContext().getExternalCacheDir()
        .getAbsolutePath();
  }

  @Test
  public void appendVideoRecordedDoNotIncreaseVideoDuration() throws IOException, IntermediateFileException {
    List<String> videoPaths = new ArrayList<>();
    String videoRecordedTemp = getAssetPath("videoRecordedTemp.mp4");
    videoPaths.add(videoRecordedTemp);
    videoPaths.add(videoRecordedTemp);
    videoPaths.add(videoRecordedTemp);
    videoPaths.add(videoRecordedTemp);
    videoPaths.add(videoRecordedTemp);
    videoPaths.add(videoRecordedTemp);
    videoPaths.add(videoRecordedTemp);
    String pathFinalVideoAppended = new File(videoRecordedTemp).getParent() + File.separator +
        "V_AppendedFilesRecorded.mp4";
    Appender appender = new Appender();

    Movie movie = appender.appendVideos(videoPaths);
    Utils.createFile(movie, pathFinalVideoAppended);

    File fileVideoAppended = new File(pathFinalVideoAppended);
    assertThat(fileVideoAppended.exists(), is(true));
    int videoAppendedDuration = Integer.parseInt(getVideoDuration(pathFinalVideoAppended));
    int videoRecordedTempDuration = Integer.parseInt(getVideoDuration(new File(videoRecordedTemp)
        .getAbsolutePath()));
    assertThat(videoAppendedDuration, is(videoRecordedTempDuration*7));
  }

  @Test
  public void appendVideoAdaptedDoNotIncreaseVideoDuration() throws IOException, IntermediateFileException {
    List<String> videoPaths = new ArrayList<>();
    String videoRecordedTemp = getAssetPath("videoAdaptedMaster.mp4");
    videoPaths.add(videoRecordedTemp);
    videoPaths.add(videoRecordedTemp);
    videoPaths.add(videoRecordedTemp);
    videoPaths.add(videoRecordedTemp);
    videoPaths.add(videoRecordedTemp);
    videoPaths.add(videoRecordedTemp);
    videoPaths.add(videoRecordedTemp);
    String pathFinalVideoAppended = new File(videoRecordedTemp).getParent() + File.separator +
        "V_AppendedFilesAdaptedMaster.mp4";
    Appender appender = new Appender();

    Movie movie = appender.appendVideos(videoPaths);
    Utils.createFile(movie, pathFinalVideoAppended);

    File fileVideoAppended = new File(pathFinalVideoAppended);
    assertThat(fileVideoAppended.exists(), is(true));
    int videoAppendedDuration = Integer.parseInt(getVideoDuration(pathFinalVideoAppended));
    int videoRecordedTempDuration = Integer.parseInt(getVideoDuration(new File(videoRecordedTemp)
        .getAbsolutePath()));
    assertThat(videoAppendedDuration, is(videoRecordedTempDuration*7));
  }

  @Test
  public void append4VideosDoNotIncreaseVideoDuration() throws IOException,
      IntermediateFileException {
    List<String> videoPaths = new ArrayList<>();
    String videoTemp1Path = getAssetPath("videoTemp1.mp4");
    String videoTemp2Path = getAssetPath("videoTemp2.mp4");
    String videoTemp3Path = getAssetPath("videoTemp3.mp4");
    String videoTempEndPath = getAssetPath("videoTempEnd.mp4");
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTempEndPath);
    String pathFinalVideoAppended = new File(videoTemp1Path).getParent() + File.separator +
        "V_Appended4Files.mp4";
    Appender appender = new Appender();

    Movie movie = appender.appendVideos(videoPaths);
    Utils.createFile(movie, pathFinalVideoAppended);

    File fileVideoAppended = new File(pathFinalVideoAppended);
    assertThat(fileVideoAppended.exists(), is(true));
    int videoAppendedDuration = Integer.parseInt(getVideoDuration(pathFinalVideoAppended));
    int videoTemp1Duration = Integer.parseInt(getVideoDuration(new File(videoTemp1Path)
        .getAbsolutePath()));
    int videoTemp2Duration = Integer.parseInt(getVideoDuration(new File(videoTemp2Path)
        .getAbsolutePath()));
    int videoTemp3Duration = Integer.parseInt(getVideoDuration(new File(videoTemp3Path)
        .getAbsolutePath()));
    int videoTempEndDuration = Integer.parseInt(getVideoDuration(new File(videoTempEndPath)
        .getAbsolutePath()));
    int totalVideoAppendedDuration = videoTemp1Duration + videoTemp2Duration + videoTemp3Duration
        + videoTempEndDuration;
    assertThat(videoAppendedDuration, is(totalVideoAppendedDuration));
  }

  @Test
  public void append16VideosDoNotIncreaseVideoDuration() throws IOException,
      IntermediateFileException {
    List<String> videoPaths = new ArrayList<>();
    String videoTemp1Path = getAssetPath("videoTemp1.mp4");
    String videoTemp2Path = getAssetPath("videoTemp2.mp4");
    String videoTemp3Path = getAssetPath("videoTemp3.mp4");
    String videoTempEndPath = getAssetPath("videoTempEnd.mp4");
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTempEndPath);
    String pathFinalVideoAppended = new File(videoTemp1Path).getParent() + File.separator +
        "V_Appended16Files.mp4";
    Appender appender = new Appender();

    Movie movie = appender.appendVideos(videoPaths);
    Utils.createFile(movie, pathFinalVideoAppended);

    File fileVideoAppended = new File(pathFinalVideoAppended);
    assertThat(fileVideoAppended.exists(), is(true));
    int videoAppendedDuration = Integer.parseInt(getVideoDuration(pathFinalVideoAppended));
    int videoTemp1Duration = Integer.parseInt(getVideoDuration(new File(videoTemp1Path)
        .getAbsolutePath()));
    int videoTemp2Duration = Integer.parseInt(getVideoDuration(new File(videoTemp2Path)
        .getAbsolutePath()));
    int videoTemp3Duration = Integer.parseInt(getVideoDuration(new File(videoTemp3Path)
        .getAbsolutePath()));
    int videoTempEndDuration = Integer.parseInt(getVideoDuration(new File(videoTempEndPath)
        .getAbsolutePath()));
    int totalVideoAppendedDuration = 5*videoTemp1Duration + 5*videoTemp2Duration +
        5*videoTemp3Duration + videoTempEndDuration;
    assertThat(videoAppendedDuration, is(totalVideoAppendedDuration));
  }

  @Test
  public void append25VideosDoNotIncreaseVideoDuration() throws IOException,
      IntermediateFileException {
    List<String> videoPaths = new ArrayList<>();
    String videoTemp1Path = getAssetPath("videoTemp1.mp4");
    String videoTemp2Path = getAssetPath("videoTemp2.mp4");
    String videoTemp3Path = getAssetPath("videoTemp3.mp4");
    String videoTempEndPath = getAssetPath("videoTempEnd.mp4");
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp1Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp2Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTemp3Path);
    videoPaths.add(videoTempEndPath);
    String pathFinalVideoAppended = new File(videoTemp1Path).getParent() + File.separator +
        "V_Appended25Files.mp4";
    Appender appender = new Appender();

    Movie movie = appender.appendVideos(videoPaths);
    Utils.createFile(movie, pathFinalVideoAppended);

    File fileVideoAppended = new File(pathFinalVideoAppended);
    assertThat(fileVideoAppended.exists(), is(true));
    int videoAppendedDuration = Integer.parseInt(getVideoDuration(pathFinalVideoAppended));
    int videoTemp1Duration = Integer.parseInt(getVideoDuration(new File(videoTemp1Path)
        .getAbsolutePath()));
    int videoTemp2Duration = Integer.parseInt(getVideoDuration(new File(videoTemp2Path)
        .getAbsolutePath()));
    int videoTemp3Duration = Integer.parseInt(getVideoDuration(new File(videoTemp3Path)
        .getAbsolutePath()));
    int videoTempEndDuration = Integer.parseInt(getVideoDuration(new File(videoTempEndPath)
        .getAbsolutePath()));
    int totalVideoAppendedDuration = 8*videoTemp1Duration + 8*videoTemp2Duration +
        8*videoTemp3Duration + videoTempEndDuration;
    assertThat(videoAppendedDuration, is(totalVideoAppendedDuration));
  }

  private String getVideoDuration(String videoPath) {
    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
    mediaMetadataRetriever.setDataSource(videoPath);
    return mediaMetadataRetriever
        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
  }
}
