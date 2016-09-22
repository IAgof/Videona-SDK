package com.videonasocialmedia.transcoder;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.videonasocialmedia.transcoder.audio_mixer.AudioMixer;
import com.videonasocialmedia.transcoder.audio_mixer.listener.OnAudioMixerListener;
import com.videonasocialmedia.transcoder.format.VideonaFormat;
import com.videonasocialmedia.transcoder.overlay.Filter;
import com.videonasocialmedia.transcoder.overlay.Image;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Future;


public class TranscoderActivity extends Activity implements OnAudioMixerListener {

    private static final String TAG = "TranscoderActivity";
    private static final int REQUEST_CODE_PICK = 1;
    private static final int PROGRESS_BAR_MAX = 1000;
    private Future<Void> mFuture;

    String externalDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES) + File.separator + "VideonaSDK";
    String tempDir = externalDir + File.separator + ".temp";

    Button btnMixAudio;
    TextView textViewInfoProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transcoder);
        findViewById(R.id.select_video_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"), REQUEST_CODE_PICK);
            }
        });
        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFuture.cancel(true);
            }
        });

        textViewInfoProgress = (TextView) findViewById(R.id.textViewProgressMixAudio);

        btnMixAudio = (Button) findViewById(R.id.btnMixAudio);
        btnMixAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mixAudio();
            }
        });

        File externalDirPath = new File(externalDir);
        if(!externalDirPath.exists()){
            externalDirPath.mkdir();
        }

        File tempDirPath = new File(tempDir);
        if(!tempDirPath.exists()){
            tempDirPath.mkdir();
        } else {
            Utils.cleanDirectory(tempDirPath);
        }

    }

    private void mixAudio() {

        String inputVideo = externalDir + File.separator + "input_video.mp4";
        String inputVideo2 = externalDir + File.separator + "input_video_2.mp4";
        String inputAudio = externalDir + File.separator + "audio_folk.m4a";
        String inputAudio2 = externalDir + File.separator + "audio_birthday.m4a";
        String outputAudio = externalDir + File.separator + "AudioMixed_" + System.currentTimeMillis() + ".m4a";

        File inputFile1 = new File(inputVideo);
        if(!inputFile1.exists()){
            try {
                Utils.copyResourceToTemp(this,externalDir, "input_video", R.raw.input_video, ".mp4");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File inputFile2 = new File(inputAudio2);
        if(!inputFile2.exists()){
            try {
                Utils.copyResourceToTemp(this,externalDir, "audio_birthday", R.raw.audio_birthday, ".m4a");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            MediaTranscoder.getInstance().mixTwoAudioFiles(inputVideo, inputAudio2, 0.5f, tempDir,outputAudio, this);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PICK: {
                final File file;
                if (resultCode == RESULT_OK) {
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
                            onTranscodeFinished(true, "transcoded file placed on " + file.getAbsolutePath() );
                            startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(file), "video/mp4"));
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
                    Log.d(TAG, "transcoding " + inPath + " into " + file);

                    String pathName = "/sdcard/DCIM/tempV1.png";
                    Drawable drawable = Drawable.createFromPath(pathName);

                    VideonaFormat videonaFormat = new VideonaFormat(5000*1000,1920,1080);
                    Image imageText = new Image(pathName,1280, 720, 0, 0);


                    Filter imageFilter = new Filter(drawable,videonaFormat.getVideoWidth(),videonaFormat.getVideoHeight());

                    try {
                        MediaTranscoder.getInstance().transcodeTrimAndOverlayImageToVideo(inPath,
                                file.getAbsolutePath(),videonaFormat, listener, imageText, 10000,20000);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                 /*   mFuture = MediaTranscoder.getInstance().transcodeAndOverlayImageToVideo(fileDescriptor, file.getAbsolutePath(),
                            new VideonaFormat(), listener, getDrawable(R.drawable.overlay_filter_party));*/

                  //  mFuture = MediaTranscoder.getInstance().transcodeAndTrimVideo(fileDescriptor,
                      //      file.getAbsolutePath(), new VideonaFormat(), listener, 10000, 15000);

                 //   mFuture = MediaTranscoder.getInstance().transcodeOnlyVideo(fileDescriptor,
                   //              file.getAbsolutePath(), new VideonaFormat(), listener);

                            switchButtonEnabled(true);
                }
                break;
            }
            default:
                super.onActivityResult(requestCode, resultCode, data);
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
        findViewById(R.id.select_video_button).setEnabled(!isProgress);
        findViewById(R.id.cancel_button).setEnabled(isProgress);
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
}
