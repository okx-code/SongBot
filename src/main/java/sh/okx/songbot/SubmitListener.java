package sh.okx.songbot;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubmitListener extends ListenerAdapter {
  private final static String YOUTUBE_REGEX = "((?:https?:)?//)?((?:www|m)\\.)?((?:youtube\\.com|youtu.be))(/(?:[\\w\\-]+\\?v=|embed/|v/)?)([\\w\\-]+)(\\S+)?";
  private final String youtubeApiKey;
  private final String invalidUrl;
  private final String submissionSuccess;
  private final String announcementSuccess;
  private final long submissions;
  private final long submit;
  private final long announcements;
  private final long announceRoleId;

  public SubmitListener(Properties config) {
    this.youtubeApiKey = config.getProperty("youtube-api-key");
    this.invalidUrl = config.getProperty("invalid-url");
    this.submissionSuccess = config.getProperty("submission-success");
    this.announcementSuccess = config.getProperty("announcement-success");
    this.submissions = Long.parseLong(config.getProperty("submissions"));
    this.submit = Long.parseLong(config.getProperty("submit"));
    this.announcements = Long.parseLong(config.getProperty("announcements"));
    this.announceRoleId = Long.parseLong(config.getProperty("announce-role-id"));
  }

  @Override
  public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
    TextChannel channel = event.getChannel();
    Message message = event.getMessage();
    String[] parts = message.getContentRaw().split(" ", 2);
    if (channel.getIdLong() != submit
        || message.getAuthor().isBot()
        || message.isWebhookMessage()) {
      return;
    }

    if (parts.length > 1 && parts[0].equalsIgnoreCase("!submit")) {
      handleSubmit(parts[1], message);
    } else if (parts.length > 0 && parts[0].equalsIgnoreCase("!announce") && hasRoleById(message.getMember(), announceRoleId)) {
      handleAnnounce(message);
    }
  }

  private void handleAnnounce(Message message) {
    TextChannel announcements = message.getGuild().getTextChannelById(this.announcements);
    TextChannel submissions = message.getGuild().getTextChannelById(this.submissions);
    long selfId = message.getJDA().getSelfUser().getIdLong();

    double highestRating = Double.MIN_VALUE;
    Message highestMessage = null;

    for (Message submission : submissions.getIterableHistory()) {
      // check the message was made by us
      if (submission.isWebhookMessage() || submission.getAuthor().getIdLong() != selfId) {
        return;
      }

      double rating = calculateRating(submission.getReactions());
      if (rating > highestRating) {
        highestRating = rating;
        highestMessage = submission;
      }

      submission.delete().queue();
    }

    if (highestMessage == null) {
      message.getChannel().sendMessage("There are no submissions.").queue();
      return;
    }

    // since we made the message we know it will have only one embed.
    MessageEmbed embed = highestMessage.getEmbeds().get(0);

    announcements.sendMessage(new EmbedBuilder(embed)
        .setFooter("Winner of song competition", null)
        .build()).queue();
    message.getChannel().sendMessage(announcementSuccess).queue();
  }

  private void handleSubmit(String url, Message message) {
    TextChannel channel = message.getTextChannel();
    Pattern pattern = Pattern.compile(YOUTUBE_REGEX);
    Matcher matcher = pattern.matcher(url);
    if (!matcher.matches()) {
      channel.sendMessage(invalidUrl).queue();
      return;
    }

    YouTubeVideo video;
    try {
      video = new YouTubeVideo(matcher.group(5));
    } catch (VideoNotFoundException e) {
      channel.sendMessage("Could not find video.").queue();
      e.printStackTrace();
      return;
    } catch (IOException e) {
      channel.sendMessage("Error while trying to get video.").queue();
      e.printStackTrace();
      return;
    }

    User author = message.getAuthor();

    // check if user has posted before
    TextChannel submissions = channel.getGuild().getTextChannelById(this.submissions);
    long selfId = channel.getJDA().getSelfUser().getIdLong();
    for(Message submission : submissions.getIterableHistory()) {
      // go through the submissions we posted only
      if (message.isWebhookMessage()
          || selfId != submission.getAuthor().getIdLong()) {
        continue;
      }

      MessageEmbed embed = submission.getEmbeds().get(0);
      String userId = embed.getFooter().getText();
      if(userId.equals(author.getId())) {
        submission.delete().queue();
        break;
      }
    }

    submissions.sendMessage(new EmbedBuilder()
      .setAuthor(author.getName(), null, author.getEffectiveAvatarUrl())
      .setThumbnail(video.getThumbnail())
      .setTitle(video.getTitle(), video.getUrl())
      .addField("Channel", video.getChannel(), false)
      .setFooter(author.getId(), null)
      .build()).queue(submission -> {
        submission.addReaction("1⃣").queue();
        submission.addReaction("2⃣").queue();
        submission.addReaction("3⃣").queue();
        submission.addReaction("4⃣").queue();
        submission.addReaction("5⃣").queue();
      });
    channel.sendMessage(submissionSuccess).queue();
  }

  private boolean hasRoleById(Member member, long roleId) {
    for (Role role : member.getRoles()) {
      if (role.getIdLong() == roleId) {
        return true;
      }
    }
    return false;
  }

  private double calculateRating(List<MessageReaction> reactions) {
    double sum = 0;
    OUTER:
    for (MessageReaction reaction : reactions) {
      int amount;
      switch (reaction.getReactionEmote().getName()) {
        case "1⃣":
          amount = 1;
          break;
        case "2⃣":
          amount = 2;
          break;
        case "3⃣":
          amount = 3;
          break;
        case "4⃣":
          amount = 4;
          break;
        case "5⃣":
          amount = 5;
          break;
        default:
          continue OUTER;
      }
      sum += reaction.getCount() * amount;
    }

    return sum / reactions.size();
  }
}
