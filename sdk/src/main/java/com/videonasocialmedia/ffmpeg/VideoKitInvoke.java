package com.videonasocialmedia.ffmpeg;

/**
 * Created by alvaro on 11/10/18.
 */

public class VideoKitInvoke {
  static {
    try {
      System.loadLibrary("videokitinvoke");
    } catch (UnsatisfiedLinkError e) {
      e.printStackTrace();
    }
  }

  int process(String libPath, String[] args) {
    return run(libPath, args);
  }

  private native int run(String libPath, String[] args);

  public CommandBuilder createCommand() {
    return new VideoCommandBuilder(this);
  }

  private String libPath;

  public String getLibPath() {
    return libPath;
  }

  public void setLibPath(String libPath) {
    this.libPath = libPath;
  }
}
