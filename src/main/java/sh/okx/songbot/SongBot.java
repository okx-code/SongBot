package sh.okx.songbot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.logging.Logger;

public class SongBot {
  public static void main(String[] args) throws IOException, GeneralSecurityException {
    try {
      Files.copy(SongBot.class.getResourceAsStream("/config.properties"), new File("config.properties").toPath());
      getLogger().warning("Copied config - please change it and restart the bot");
      return;
    } catch (FileAlreadyExistsException ex) {
      // this should happen if we are to carry on
    }
    Properties config = new Properties();
    config.load(new FileReader("config.properties"));

    YouTubeVideo.initialize(config.getProperty("youtube-api-key"));

    JDA jda = new JDABuilder(AccountType.BOT)
        .setToken(config.getProperty("token"))
        .setGame(Game.of(Game.GameType.valueOf(config.getProperty("type").toUpperCase()), config.getProperty("game")))
        .setStatus(OnlineStatus.fromKey(config.getProperty("status")))
        .build();

    jda.addEventListener(new SubmitListener(config));
    jda.addEventListener(new SubmissionsListener(config));
  }

  private static Logger getLogger() {
    return Logger.getLogger("Song Bot");
  }
}
