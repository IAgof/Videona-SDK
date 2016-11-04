package com.videonasocialmedia.transcoder;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.videonasocialmedia.transcoder.audio_mixer.listener.OnAudioEffectListener;
import com.videonasocialmedia.transcoder.audio_mixer.listener.OnAudioMixerListener;
import com.videonasocialmedia.transcoder.format.VideonaFormat;
import com.videonasocialmedia.transcoder.overlay.Filter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class TranscoderActivity extends Activity implements OnAudioMixerListener,
    OnAudioEffectListener {

  private static final String TAG = "TranscoderActivity";
  private static final int REQUEST_CODE_TRIM_VIDEO = 1;
  private static final int REQUEST_CODE_TRANSCODE_VIDEO = 2;
  private static final int REQUEST_CODE_OVERLAY_VIDEO = 3;
  private static final int REQUEST_CODE_FADE_INOUT_AUDIO = 4;
  private static final int PROGRESS_BAR_MAX = 1000;
  private Future<Void> mFuture;

  String externalDir = Environment.getExternalStoragePublicDirectory(
      Environment.DIRECTORY_MOVIES) + File.separator + "VideonaSDK";
  String tempDir = externalDir + File.separator + ".temp";

  @BindView(R.id.btnMixAudio) Button mixAudio;
  @BindView(R.id.textViewConsoleInfo) TextView textViewInfoProgress;
  @BindView(R.id.cancel_button) Button cancelButton;
  @BindView(R.id.transcode_trim_video) Button trimVideo;
  @BindView(R.id.transcode_video) Button transcodeVideo;
  @BindView(R.id.transcode_add_overlay_video) Button overlayVideo;
  @BindView(R.id.transcode_audio_fade_in_out) Button fadeInOutAudio;
  @BindView(R.id.transcode_video_fade_in_out) Button fadeInOutVideo;

  String outputAudioFadeInOut = externalDir + File.separator + "AudioFadeInOut_" +
      System.currentTimeMillis() + ".m4a";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_transcoder);

    Dexter.initialize(this);
    ButterKnife.bind(this);

    checkManifestPermission();
    checkPaths();

  }

  private void checkManifestPermission() {
    Dexter.checkPermissions(new MultiplePermissionsListener() {
      @Override
      public void onPermissionsChecked(MultiplePermissionsReport report) {/* ... */}

      @Override
      public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions,
                                                     PermissionToken token) {/* ... */}
    }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
  }

  private void checkPaths() {
    File externalDirPath = new File(externalDir);
    if (!externalDirPath.exists()) {
      externalDirPath.mkdir();
    }

    File tempDirPath = new File(tempDir);
    if (!tempDirPath.exists()) {
      tempDirPath.mkdir();
    } else {
      Utils.cleanDirectory(tempDirPath);
    }
  }

  @OnClick(R.id.transcode_trim_video)
  public void onClickTrimVideo(){
    startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"),
        REQUEST_CODE_TRIM_VIDEO);
  }

  @OnClick(R.id.transcode_video)
  public void onClickTranscodeVideo(){
    startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"),
        REQUEST_CODE_TRANSCODE_VIDEO);
  }

  @OnClick(R.id.transcode_add_overlay_video)
  public void onClickOverlayVideo(){
    startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"),
        REQUEST_CODE_OVERLAY_VIDEO);
  }

  @OnClick(R.id.transcode_audio_fade_in_out)
  public void onClickFadeInOutAudio(){
    startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"),
        REQUEST_CODE_FADE_INOUT_AUDIO);
  }

  @OnClick(R.id.cancel_button)
  public void onClickCancelButton(){
    mFuture.cancel(true);
  }

  @OnClick(R.id.btnMixAudio)
  public void onClickMixAudio(){
    textViewInfoProgress.setText("Mezclando audio ...");
    mixAudio();
  }

  private void mixAudio() {

    String inputVideo = externalDir + File.separator + "input_video_1.mp4";
    String inputVideo2 = externalDir + File.separator + "input_video_2.mp4";

    String outputAudio = externalDir + File.separator + "AudioMixed_" + System.currentTimeMillis()
        + ".m4a";

    File inputFile1 = new File(inputVideo);
    if (!inputFile1.exists()) {
      try {
        textViewInfoProgress.setText("Copiando vídeo 1 a sdcard");
        Utils.copyResourceToTemp(this, externalDir, "input_video_1", R.raw.video_uno, ".mp4");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    File inputFile2 = new File(inputVideo2);
    if (!inputFile2.exists()) {
      try {
        textViewInfoProgress.setText("Copiando vídeo 2 a sdcard");
        Utils.copyResourceToTemp(this, externalDir, "input_video_2", R.raw.video_dos, ".mp4");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      mFuture = MediaTranscoder.getInstance().mixAudioTwoFiles(inputVideo, inputVideo2, 0.90f,
          tempDir, outputAudio, this);
    } catch (IOException e) {
      e.printStackTrace();
    }


  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(resultCode == RESULT_OK) {
        final File file;
        String externalDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES) + File.separator;
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        file = new File(externalDir, "transcode_test_" + timeStamp + ".mp4");

        final String inPath = getPathFromUri(data.getData());
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setMax(PROGRESS_BAR_MAX);
        final long startTime = SystemClock.uptimeMillis();
        MediaTranscoderListener listener = new MediaTranscoderListener() {
          @Override
          public void onTranscodeProgress(double progress) {
            if (progress < 0) {
              progressBar.setIndeterminate(true);
            } else {
              progressBar.setIndeterminate(false);
              progressBar.setProgress((int) Math.round(progress * PROGRESS_BAR_MAX));
            }
          }

          @Override
          public void onTranscodeCompleted() {
            Log.d(TAG, "transcoding took " + (SystemClock.uptimeMillis() - startTime) + "ms");
            onTranscodeFinished(true, "transcoded file placed on " + file.getAbsolutePath());
            startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(file),
                "video/mp4"));
          }

          @Override
          public void onTranscodeCanceled() {
            onTranscodeFinished(false, "Transcoder canceled. " + inPath);
          }

          @Override
          public void onTranscodeFailed(Exception exception) {
            onTranscodeFinished(false, "Transcoder error occurred." + inPath);
          }
        };

      switchButtonEnabled(true);

      switch (requestCode) {
        case REQUEST_CODE_TRIM_VIDEO:{

          try {
            mFuture = MediaTranscoder.getInstance().transcodeAndTrimVideo(inPath,
                     file.getAbsolutePath(), new VideonaFormat(), listener, 5000, 10000);
          } catch (IOException e) {
            e.printStackTrace();
          }

          break;
        }
        case REQUEST_CODE_TRANSCODE_VIDEO: {

          try {
            mFuture = MediaTranscoder.getInstance().transcodeOnlyVideo(inPath,
                file.getAbsolutePath(), new VideonaFormat(), listener);
          } catch (IOException e) {
            e.printStackTrace();
          }

          break;
        }
        case REQUEST_CODE_OVERLAY_VIDEO: {

          /**
           Example to overlay image from file
           String pathName = "/sdcard/DCIM/tempV1.png";
           Drawable drawable = Drawable.createFromPath(pathName);
           Image imageText = new Image(pathName, 1280, 720, 0, 0);
           */
          VideonaFormat videonaFormat = new VideonaFormat(5000 * 1000, 1920, 1080);
          Filter imageFilter = new Filter(getDrawable(R.drawable.overlay_filter_bollywood),
              videonaFormat.getVideoWidth(), videonaFormat.getVideoHeight());

          try {
            MediaTranscoder.getInstance().transcodeAndOverlayImageToVideo(inPath,
                file.getAbsolutePath(), videonaFormat, listener, imageFilter);
          } catch (IOException e) {
            e.printStackTrace();
          }
          break;
        }

        case REQUEST_CODE_FADE_INOUT_AUDIO: {

          try {
            mFuture = MediaTranscoder.getInstance().audioFadeInFadeOutToFile(inPath, 500, 500,
                tempDir, outputAudioFadeInOut, this);
          } catch (IOException e) {
            e.printStackTrace();
          }
          break;
        }
        default:
          super.onActivityResult(requestCode, resultCode, data);
      }
    }
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  private String getPathFromUri(Uri uri) {

    String wholeID = DocumentsContract.getDocumentId(uri);

    // Split at colon, use second item in the array
    String id = wholeID.split(":")[1];


    String filePath = Environment.getExternalStorageDirectory() + File.separator + id;


    return filePath;
  }

  private void onTranscodeFinished(boolean isSuccess, String toastMessage) {
    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
    progressBar.setIndeterminate(false);
    progressBar.setProgress(isSuccess ? PROGRESS_BAR_MAX : 0);
    switchButtonEnabled(false);
    Toast.makeText(TranscoderActivity.this, toastMessage, Toast.LENGTH_LONG).show();
  }

  private void switchButtonEnabled(boolean isProgress) {
    trimVideo.setEnabled(!isProgress);
    transcodeVideo.setEnabled(!isProgress);
    overlayVideo.setEnabled(!isProgress);
    fadeInOutAudio.setEnabled(!isProgress);
    mixAudio.setEnabled(!isProgress);
    fadeInOutVideo.setEnabled(!isProgress);
    cancelButton.setEnabled(isProgress);
  }

  @Override
  public void onAudioMixerSuccess(String outputFile) {
    textViewInfoProgress.setText("Success " + outputFile);
    textViewInfoProgress.setTextColor(Color.GREEN);
  }

  @Override
  public void onAudioMixerProgress(String progress) {
    textViewInfoProgress.setText(progress);
    textViewInfoProgress.setTextColor(Color.BLUE);
  }

  @Override
  public void onAudioMixerError(String error) {
    textViewInfoProgress.setText(error);
    textViewInfoProgress.setTextColor(Color.RED);
  }

  @Override
  public void onAudioMixerCanceled() {

  }

  @Override
  public void onAudioEffectSuccess(String outputFile) {
    textViewInfoProgress.setText("Success " + outputFile);
    textViewInfoProgress.setTextColor(Color.GREEN);
  }

  @Override
  public void onAudioEffectProgress(String progress) {

  }

  @Override
  public void onAudioEffectError(String error) {

  }

  @Override
  public void onAudioEffectCanceled() {

  }
}
