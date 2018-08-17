package sh.okx.songbot;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class YouTubeVideo {
  private static YouTube youtube;
  private static String apiKey;
  private String id;
  private VideoSnippet video;
  public YouTubeVideo(String id) throws IOException {
    YouTube.Videos.List videosList = youtube.videos().list("snippet");
    videosList.setKey(apiKey);
    videosList.setId(id);

    List<Video> videos = videosList.execute().getItems();
    if (videos.size() < 1) {
      throw new VideoNotFoundException(id);
    }

    Video video = videos.get(0);
    this.id = video.getId();
    this.video = video.getSnippet();
  }

  public static void initialize(String apiKey) throws GeneralSecurityException, IOException {
    YouTubeVideo.apiKey = apiKey;
    youtube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), null)
        .setApplicationName("youtube-cmdline-search-sample").build();
  }

  public String getChannel() {
    return video.getChannelTitle();
  }

  public String getTitle() {
    return video.getTitle();
  }

  public String getThumbnail() {
    return video.getThumbnails().getMedium().getUrl();
  }

  public DateTime getUploadDate() {
    return video.getPublishedAt();
  }

  public String getUrl() {
    return "https://youtube.com/watch?v=" + id;
  }
}
