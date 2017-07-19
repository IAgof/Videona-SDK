package com.videonasocialmedia.videonamediaframework.model.media.utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

public class ChangeNotifier {
  public final ArrayList<WeakReference<ElementChangedListener>> changeListeners =
          new ArrayList<>();

  public ChangeNotifier() {
  }

  public void addListener(ElementChangedListener listener) {
    changeListeners.add(new WeakReference<>(listener));
  }

  public void removeListener(ElementChangedListener listener) {
    Iterator<WeakReference<ElementChangedListener>> iterator = changeListeners.iterator();
    while (iterator.hasNext()) {
      WeakReference<ElementChangedListener> listenerRef = iterator.next();
      if (listenerRef.get() == listener) {
        changeListeners.remove(listenerRef);
      }
    }
  }

  public void notifyChanges() {
    for (WeakReference<ElementChangedListener> listenerRef : changeListeners) {
      ElementChangedListener listener = listenerRef.get();
      listener.onObjectUpdated();
    }
  }

}