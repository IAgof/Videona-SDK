package com.videonasocialmedia.ffmpeg;

/**
 * Created by alvaro on 11/10/18.
 */

public class VideoKitInvoke {
  static {
    try {
      System.loadLibrary("avutil");
      System.loadLibrary("swresample");
      System.loadLibrary("avcodec");
      System.loadLibrary("avformat");
      System.loadLibrary("swscale");
      System.loadLibrary("avfilter");
      System.loadLibrary("avdevice");
      //System.loadLibrary("videokit");
      System.loadLibrary("videokitinvoke");
    } catch (UnsatisfiedLinkError e) {
      e.printStackTrace();
    }
  }

  private LogLevel logLevel = LogLevel.FULL;

  public void setLogLevel(LogLevel level) {
    logLevel = level;
  }

  int process(String libPath, String[] args) {
    //return run(logLevel.getValue(), args);
    return run(libPath, args);
  }

  //private native int run(int loglevel, String[] args);

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
