package com.videonasocialmedia.videonamediaframework.pipeline;

import com.videonasocialmedia.transcoder.MediaTranscoder;
import com.videonasocialmedia.transcoder.audio.listener.OnAudioMixerListener;
import com.videonasocialmedia.videonamediaframework.model.media.Media;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by alvaro on 22/09/16.
 */

/**
 * This use case mixes two audio files with relative volumes into a new one. It's used to mix
 * original audio tracks from video clips in project with a voice over track recorded by the user.
 *
 * Currently is called once, after the whole project has been exported, currently in ShareActivity,
 */
public class AudioMixer implements OnAudioMixerListener {
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

    public void mixAudio(List<Media> mediaList, String tempAudioPath,
                         long durationAudioFile, OnMixAudioListener listener) {
        this.listener = listener;
        try {
            Future<Void> mFuture = MediaTranscoder.getInstance().mixAudioFiles(mediaList,
                tempAudioPath, outputFilePath, durationAudioFile, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAudioMixerSuccess(String outputFileMixed) {
        this.outputFilePath = outputFileMixed;
        // TODO(jliarte): 17/12/16 new implementation call onMixAudioSuccess to finally update
        //                video in ShareActivity
       // FileUtils.cleanDirectory(new File(tempAudioPath));
        listener.onMixAudioSuccess(outputFilePath);
    }

    @Override
    public void onAudioMixerProgress(String progress) {
    }

    @Override
    public void onAudioMixerError(String error) {
        listener.onMixAudioError();
    }

    @Override
    public void onAudioMixerCanceled() {
        // TODO(jliarte): 20/12/16 pass some error code?
        listener.onMixAudioError();
    }

    /**
     * Created by alvaro on 22/09/16.
     */

    public interface OnMixAudioListener {
        void onMixAudioSuccess(String path);
        void onMixAudioError();
    }
}
