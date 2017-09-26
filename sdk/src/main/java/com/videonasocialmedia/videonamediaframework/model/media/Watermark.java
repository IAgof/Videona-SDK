package com.videonasocialmedia.videonamediaframework.model.media;

/**
 * Created by alvaro on 27/02/17.
 */

public class Watermark {

  private String resourceWatermarkFilePath;

  public Watermark(String resourceWatermarkFilePath){
    this.resourceWatermarkFilePath = resourceWatermarkFilePath;
  }

  public String getResourceWatermarkFilePath() {
    return resourceWatermarkFilePath;
  }
}
