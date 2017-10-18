package com.videonasocialmedia.videonamediaframework.utils;

import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Created by jliarte on 27/10/16.
 */

public class FileUtils {
  private static final String LOG_TAG = FileUtils.class.getSimpleName();

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

  public static ListenableFuture<Long> getDurationFileAsync(final String filePath) {
    return MoreExecutors.newDirectExecutorService().submit(new Callable<Long>() {
      @Override
      public Long call() throws Exception {
        return getDurationFile(filePath);
      }
    });
  }

  public static long getDurationFile(String filePath) {
    /**
     * Other solution to get duration, continue dependency with MediaRetriever, Android.
     Video exportedVideo = new Video(filePath);
     return exportedVideo.getFileDuration();
     */
    Log.d(LOG_TAG, "getDurationFile init");
    long duration = 0;
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    retriever.setDataSource(filePath);
    duration = Integer.parseInt(retriever.extractMetadata(
        MediaMetadataRetriever.METADATA_KEY_DURATION));
    Log.d(LOG_TAG, "getDurationFile end");
    return duration*1000;
  }

  public static void removeFile(String filePath) {
    File f = new File(filePath);
    f.delete();
  }

  public static void moveFile(String origPath, String dstPath) {
    File orig = new File(origPath);
    File dest = new File(dstPath);
    orig.renameTo(dest);
  }

}
