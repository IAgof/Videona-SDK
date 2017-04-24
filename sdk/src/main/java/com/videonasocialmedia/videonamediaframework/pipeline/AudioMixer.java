package com.videonasocialmedia.videonamediaframework.pipeline;

import com.google.common.util.concurrent.ListenableFuture;
import com.videonasocialmedia.transcoder.MediaTranscoder;

import java.io.File;

import static com.videonasocialmedia.transcoder.MediaTranscoder.MediaTranscoderListener;

/**
 * Created by alvaro on 22/09/16.
 */

/**
 * This use case mixes two audio files with relative volumes into a new one. It's used to mix
 * original audio tracks from video clips in project with a voice over track recorded by the user.
 *
 * Currently is called once, after the whole project has been exported, currently in ShareActivity,
 */
public class AudioMixer {
    private OnMixAudioListener listener;
    private String outputFilePath;

    public AudioMixer(String outputFilePath) {
        this.outputFilePath = outputFilePath;
       // cleanOutputFile();
    }

    private void cleanOutputFile() {
        File f = new File(outputFilePath);
        if (f.exists()) {
            f.delete();
        }
    }

    public void mixAudio(String inputFileOne, String inputFileTwo, float volume,
                         String tempAudioPath, long durationAudioFile,
                         final OnMixAudioListener listener) {
        this.listener = listener;
        // TODO(jliarte): 16/03/17 should we return the transcoding job?
        ListenableFuture<Void> transcodingJob = MediaTranscoder.getInstance()
                        .mixAudioTwoFiles(inputFileOne, inputFileTwo, volume,
                                tempAudioPath, outputFilePath, durationAudioFile,
                        new MediaTranscoderListener() {
                            @Override
                            public void onTranscodeSuccess(String outputFileMixed) {
                                outputFilePath = outputFileMixed;
                                // TODO(jliarte): 17/12/16 new implementation call onMixAudioSuccess to finally update
                                //                video in ShareActivity
                                // FileUtils.cleanDirectory(new File(tempAudioPath));
                                listener.onMixAudioSuccess(outputFilePath);
                            }

                            @Override
                            public void onTranscodeProgress(String progress) {

                            }

                            @Override
                            public void onTranscodeError(String error) {
                                // TODO(jliarte): 20/12/16 pass some error code?
                                listener.onMixAudioError();
                            }

                            @Override
                            public void onTranscodeCanceled() {
                                // TODO(jliarte): 20/12/16 pass some error code?
                                listener.onMixAudioError();
                            }
                        });
    }

    /**
     * Created by alvaro on 22/09/16.
     */
    public interface OnMixAudioListener {
        void onMixAudioSuccess(String path);
        void onMixAudioError();
    }
}
