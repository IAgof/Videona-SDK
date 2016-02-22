package com.videonasocialmedia.demosdk;
/*
 * Copyright (C) 2015 Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 *
 * Authors:
 * Álvaro Martínez Marco
 *
 */

import android.app.Activity;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.videonasocialmedia.sdk.format.SessionConfig;
import com.videonasocialmedia.sdk.format.VideonaFormat;
import com.videonasocialmedia.sdk.format.VideonaFormatStrategy;
import com.videonasocialmedia.sdk.transcoder.VideonaMediaTranscoder;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

public class VideoTranscoderActivity extends Activity {

    Button selectVideoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transcoder);

        selectVideoButton = (Button) findViewById(R.id.select_video_button);

        selectVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String videoExample = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MOVIES) + File.separator + "InputVideo.mp4";
                File example = new File(videoExample);
               // String outputPath = example.getAbsolutePath();
                ContentResolver resolver = getContentResolver();
                final ParcelFileDescriptor parcelFileDescriptor;
                try {
                    parcelFileDescriptor = ParcelFileDescriptor.open(example, ParcelFileDescriptor.MODE_READ_ONLY); //resolver.openFileDescriptor(data.getData(), "r");
                    Log.d("Activity", "File open '" + example.getAbsolutePath());
                } catch (FileNotFoundException e) {
                    Log.w("Could not open '" + example.getAbsolutePath() + "'", e);
                    Toast.makeText(VideoTranscoderActivity.this, "File not found.", Toast.LENGTH_LONG).show();
                    return;
                }

                final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

                String outputPath = example.getParent() + File.separator + "OutputVideo.mp4";
                Log.d("Activity", " outputPath " + outputPath);

                File testOutput = new File(outputPath);
                testOutput.deleteOnExit();

                VideonaFormatStrategy formatStrategy = new VideonaFormat(new SessionConfig());

                VideonaMediaTranscoder.getInstance().setDataSource(fileDescriptor);
                try {
                    VideonaMediaTranscoder.getInstance().transcodeVideoFile(outputPath, formatStrategy);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

    }
}
