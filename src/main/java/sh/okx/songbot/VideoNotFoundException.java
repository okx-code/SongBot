package sh.okx.songbot;

import java.io.IOException;

public class VideoNotFoundException extends IOException {
  private String video;

  public VideoNotFoundException(String video) {
    super("For video: " + video);
    this.video = video;
  }

  public String getVideo() {
    return video;
  }
}
