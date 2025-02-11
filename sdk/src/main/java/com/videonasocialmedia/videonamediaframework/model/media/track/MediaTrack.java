/*
 * Copyright (c) 2015. Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 *
 * Authors:
 * Juan Javier Cabanas
 * Álvaro Martínez Marco
 * Danny R. Fonseca Arboleda
 */
package com.videonasocialmedia.videonamediaframework.model.media.track;

import com.videonasocialmedia.videonamediaframework.model.Constants;
import com.videonasocialmedia.videonamediaframework.model.media.Video;
import com.videonasocialmedia.videonamediaframework.model.media.effects.Effect;
import com.videonasocialmedia.videonamediaframework.model.media.exceptions.IllegalItemOnTrack;
import com.videonasocialmedia.videonamediaframework.model.media.exceptions.IllegalOrphanTransitionOnTrack;
import com.videonasocialmedia.videonamediaframework.model.media.Audio;
import com.videonasocialmedia.videonamediaframework.model.media.Media;
import com.videonasocialmedia.videonamediaframework.model.media.transitions.Transition;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * A media track instance is a track that can contain video and image media items but no audio
 * media items. There could be just one Media track for project.
 *
 * @see com.videonasocialmedia.videonamediaframework.model.media.track.Track
 * Created by dfa on 30/3/15.
 */
public class MediaTrack extends Track {

    /**
     * Constructor of minimum number of parameters. Default constructor.
     * Used when a new project is launched.
     *
     * @see com.videonasocialmedia.videonamediaframework.model.media.track.Track
     */
    public MediaTrack() {
        super(Constants.INDEX_MEDIA_TRACK, 1f, false, 0);
    }

    /**
     * Parametrized constructor. Used when a saved project is launched.
     *
     * @see com.videonasocialmedia.videonamediaframework.model.media.track.Track
     */
    public MediaTrack(LinkedList<Media> items, HashMap<Integer, LinkedList<Effect>> effects,
                      HashMap<String, Transition> transitions) {
        super(Constants.INDEX_MEDIA_TRACK, items, effects, transitions);
        this.checkItems();
    }

  /**
   * Copy constructor.
   *
   * @param mediaTrack mediaTrack object to copy from
   */
  public MediaTrack(MediaTrack mediaTrack) throws IllegalItemOnTrack {
        super(Constants.INDEX_MEDIA_TRACK);
        super.setVolume(mediaTrack.getVolume());
      for (Media media : mediaTrack.getItems()) {
          if (media instanceof Video) {
              this.insertItem(new Video((Video) media));
          } else {
              // TODO:(alvaro.martinez) 3/01/17 review model of tracks, as we don't currently use any other type than Video in this MediaTrack
              throw new IllegalItemOnTrack("Unused media subtype");
          }
      }
  }

  public MediaTrack(int id, float volume, boolean mute, int position) {
    super(id,volume,mute,position);
  }

  /**
     * Ensure there are only Media items on items list.
     */
    private void checkItems() {
        for (Media item : items) {
            if (item instanceof Audio) {
                //throw new IllegalItemOnMediaTrack("Cannot add media audio items to a media track");
                this.items.removeFirstOccurrence(item);
            }
        }
    }

    public int getNumVideosInProject() {
        return this.getItems().size();
    }

    /**
     * Insert a new Media item in the media track. Get sure it is not an Audio media item.
     *
     * @throws IllegalItemOnTrack - when trying to add a Audio item on
     * @see com.videonasocialmedia.videonamediaframework.model.media.track.Track
     */
    @Override
    public boolean insertItemAt(int position, Media itemToAdd) throws IllegalItemOnTrack {
        if (itemToAdd instanceof Audio) {
            throw new IllegalItemOnTrack("Cannot add an Audio media item to a MediaTrack.");
        }
        itemToAdd.createIdentifier();
        return super.insertItemAt(position, itemToAdd);
    }

    @Override
    public boolean insertItem(Media itemToAdd) throws IllegalItemOnTrack {
        if (itemToAdd instanceof Audio) {
            throw new IllegalItemOnTrack("Cannot add an Audio media item to a MediaTrack.");
        }
        // With super works, waiting merge model branch to dev return this.insertItem(itemToAdd);
        itemToAdd.createIdentifier();
        return super.insertItem(itemToAdd);
    }

    /**
     * Delete Media item. Get his position and deletes from the list.
     *
     * @param itemToDelete - Media item to be deleted.
     * @return TRUE if the list contained the specified element.
     */
    @Override
    public Media deleteItem(Media itemToDelete) throws IllegalOrphanTransitionOnTrack,
            NoSuchElementException, IndexOutOfBoundsException, IllegalItemOnTrack {
        return this.deleteItemAt(this.items.indexOf(itemToDelete));
    }

    /**
     * Delete Media item on the given position.
     *
     * @param position
     */
    @Override
    public Media deleteItemAt(int position) throws IllegalOrphanTransitionOnTrack,
            NoSuchElementException, IllegalItemOnTrack {
        if (this.items.get(position) instanceof Audio) {
            throw new IllegalItemOnTrack("Cannot add an Audio media item to a MediaTrack.");
        }
        return super.deleteItemAt(position);
    }

    /**
     * Moves Media item to the given position.
     *
     * @param newPosition - The new position in the track for the media item.
     * @param itemToMove  - The media item to ve moved.
     */
    @Override
    public boolean moveItemTo(int newPosition, Media itemToMove) throws IllegalItemOnTrack,
            IllegalOrphanTransitionOnTrack {
        if (itemToMove instanceof Audio) {
            throw new IllegalItemOnTrack("Cannot add an Audio media item to a MediaTrack.");
        }
        return super.moveItemTo(newPosition, itemToMove);
    }

    /**
     * @param items
     */
    @Override
    public void setItems(LinkedList<Media> items) {
        super.setItems(items);
        this.checkItems();
    }

}
