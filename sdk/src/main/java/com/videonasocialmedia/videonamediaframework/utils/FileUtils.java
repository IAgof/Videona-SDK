package com.videonasocialmedia.videonamediaframework.utils;

import android.media.MediaMetadataRetriever;

import java.io.File;

/**
 * Created by jliarte on 27/10/16.
 */

public class FileUtils {
  public static void cleanDirectory(File directory) {
    cleanPath(directory, true);
  }

  public static void cleanDirectoryFiles(File directory) {
    cleanPath(directory, false);
  }

  private static void cleanPath(File directory, boolean cleanRecursively) {
    if (directory.exists()) {
      File[] files = directory.listFiles();
      if (files != null) { //some JVMs return null for empty dirs
        for (File f : files) {
          if (f.isDirectory()) {
            if (cleanRecursively) {
              cleanDirectory(f);
            }
          } else {
            f.delete();
          }
        }
      }
    }
  }

  public static boolean createDirectory(String directoryPath) {
    File dir = new File(directoryPath);
    return !dir.exists() && dir.mkdirs();
  }

  public static long getDurationFile(String filePath){
    /**
     * Other solution to get duration, continue dependency with MediaRetriever, Android.
     Video exportedVideo = new Video(filePath);
     return exportedVideo.getFileDuration();
     */
    long duration = 0;
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    retriever.setDataSource(filePath);
    duration = Integer.parseInt(retriever.extractMetadata(
        MediaMetadataRetriever.METADATA_KEY_DURATION));
    return duration*1000;
  }
}
