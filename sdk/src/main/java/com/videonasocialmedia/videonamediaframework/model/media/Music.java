/*
 * Copyright (c) 2015. Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 */

package com.videonasocialmedia.videonamediaframework.model.media;

public class Music extends Audio {
    public static final float DEFAULT_MUSIC_VOLUME = 1f;
    //TODO en el futuro no será un recurso sino que se obtendrá
    private int musicResourceId;
    private int colorResourceId;
    //TODO refactorizar nombre
    private String musicTitle;
    private String author;
    // TODO(jliarte): 15/12/16 we set a string with the music duration in mm:ss, could lead to
    //                inconsistencies with actual length
    private String musicDuration;
    private int iconResourceId;

    // TODO(jliarte): 15/12/16 pull to Audio class
    private float volume = DEFAULT_MUSIC_VOLUME;

    public Music(int iconResourceId, String musicTitle, int musicResourceId, int colorResourceId,
                 String author, String musicDuration) {
        super(musicResourceId, "", "", musicTitle, "", 0, 0, null, null, null, null);

        this.musicResourceId = musicResourceId;
        this.colorResourceId = colorResourceId;
        this.musicTitle = musicTitle;
        this.iconResourceId = iconResourceId;
        this.author = author;
        this.musicDuration = musicDuration;
    }

    public Music(int iconResourceId, String musicTitle, int musicResourceId, String musicPath,
                 int colorResourceId, String author, String musicDuration) {
        super(musicResourceId, "", "", musicTitle, musicPath, 0, 0, null, null, null, null);
        this.musicResourceId = musicResourceId;
        this.colorResourceId = colorResourceId;
        this.musicTitle = musicTitle;
        this.iconResourceId = iconResourceId;
        this.author = author;
        this.musicDuration = musicDuration;
    }

    public Music(String musicPath){
        super(0,"","", "", musicPath, 0, 0, null, null, null, null);
    }

    public Music(String musicPath, float volume) {
        super(0,"","", "", musicPath, 0, 0, null, null, null, null);
        this.volume = volume;
    }

  public Music(Music item) {
    super(item.getMusicResourceId(), item.getIconPath(), item.getSelectedIconPath(),
        item.getMusicTitle(), item.getMediaPath(), item.getStartTime(), item.getStopTime(),
        null, null, null, null);
    this.colorResourceId = item.getColorResourceId();
    this.iconResourceId = item.getIconResourceId();
    this.author = item.getAuthor();
    this.musicDuration = item.getMusicDuration();
  }

  public float getVolume() {
        return volume;
    }

    public int getMusicResourceId() {
        return musicResourceId;
    }

    public void setMusicResourceId(int musicResourceId) {
        this.musicResourceId = musicResourceId;
    }

    public int getColorResourceId() {
        return colorResourceId;
    }

    public void setColorResourceId(int colorResourceId) {
        this.colorResourceId = colorResourceId;
    }

    public String getMusicTitle() {
        return musicTitle;
    }

    public void setMusicTitle(String name) {
        this.musicTitle = name;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    public void setIconResourceId(int iconResourceId) {
        this.iconResourceId = iconResourceId;
    }

    public String getAuthor() {
        return author;
    }

    public String getMusicDuration() {
        return musicDuration;
    }

    @Override
    public void createIdentifier() {
        identifier = musicResourceId;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }
}
