package sh.okx.songbot;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;
import java.util.Properties;

public class SubmissionsListener extends ListenerAdapter {
  private long submissions;

  public SubmissionsListener(Properties config) {
    this.submissions = Long.parseLong(config.getProperty("submissions"));
  }

  @Override
  public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
    TextChannel channel = event.getChannel();
    MessageReaction reaction = event.getReaction();
    String emote = reaction.getReactionEmote().getName();
    if (channel.getIdLong() != submissions
        || !(emote.equals("1⃣") || emote.equals("2⃣") || emote.equals("3⃣") || emote.equals("4⃣") || emote.equals("5⃣"))) {
      return;
    }

    User user = event.getUser();
    long selfId = event.getJDA().getSelfUser().getIdLong();
    channel.getMessageById(event.getMessageId()).queue(message -> {
      List<MessageEmbed> embeds = message.getEmbeds();
      if (message.isWebhookMessage()
          || selfId != message.getAuthor().getIdLong()
          || selfId == user.getIdLong()) {
        return;
      }
      MessageEmbed embed = embeds.get(0);
      String id = embed.getFooter().getText();

      // don't let users react to their own submission
      if (id.equals(user.getId())) {
        event.getReaction().removeReaction(user).queue();
      }
    });
  }
}
