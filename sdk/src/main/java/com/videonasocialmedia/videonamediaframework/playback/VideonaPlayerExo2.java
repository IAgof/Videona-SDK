package com.videonasocialmedia.videonamediaframework.playback;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.videonasocialmedia.videonamediaframework.model.media.Music;
import com.videonasocialmedia.videonamediaframework.model.media.Video;

import java.util.List;

/**
 * Created by Alvaro on 26/12/2017.
 */

public class VideonaPlayerExo2 extends RelativeLayout implements VideonaPlayer, VideonaAudioPlayer,
        SeekBar.OnSeekBarChangeListener {

    public VideonaPlayerExo2(Context context) {
        super(context);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onShown(Context context) {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void setListener(VideonaPlayerListener videonaPlayerListener) {

    }

    @Override
    public void initPreview(int instantTime) {

    }

    @Override
    public void initPreviewLists(List<Video> videoList) {

    }

    @Override
    public void bindVideoList(List<Video> videoList) {

    }

    @Override
    public void updatePreviewTimeLists() {

    }

    @Override
    public void playPreview() {

    }

    @Override
    public void pausePreview() {

    }

    @Override
    public void seekTo(int timeInMsec) {

    }

    @Override
    public void seekClipToTime(int seekTimeInMsec) {

    }

    @Override
    public void seekToClip(int position) {

    }

    @Override
    public void setMusic(Music music) {

    }

    @Override
    public void setVoiceOver(Music voiceOver) {

    }

    @Override
    public void setVideoVolume(float volume) {

    }

    @Override
    public void setMusicVolume(float volume) {

    }

    @Override
    public void setVoiceOverVolume(float volume) {

    }

    @Override
    public void setVideoTransitionFade() {

    }

    @Override
    public void setAudioTransitionFade() {

    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }

    @Override
    public void setSeekBarProgress(int progress) {

    }

    @Override
    public void setSeekBarLayoutEnabled(boolean seekBarEnabled) {

    }

    @Override
    public void resetPreview() {

    }

    @Override
    public void playAudio() {

    }

    @Override
    public void pauseAudio() {

    }

    @Override
    public void releaseAudio() {

    }

    @Override
    public void seekAudioTo(long timeInMs) {

    }
}
