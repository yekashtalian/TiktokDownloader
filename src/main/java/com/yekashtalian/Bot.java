package com.yekashtalian;

import com.yekashtalian.exception.InvalidMessageException;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.io.File;

public class Bot extends ListenerAdapter {
  static {
    System.out.println("[DEBUG] Bot класс загружен.");
  }

  public static void main(String[] args) throws LoginException {
    String token = AppProperties.get("discord.bot.token");
    if (token == null || token.isBlank()) {
      System.err.println(
          "[ERROR] Discord bot token not found! Проверьте application.properties или переменные окружения.");
      return;
    }
    System.out.println("[INFO] Запуск Discord-бота...");
    JDABuilder.createDefault(token)
        .enableIntents(
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.GUILD_MEMBERS)
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .disableCache(
            CacheFlag.ACTIVITY,
            CacheFlag.VOICE_STATE,
            CacheFlag.EMOJI,
            CacheFlag.STICKER,
            CacheFlag.SCHEDULED_EVENTS)
        .setActivity(net.dv8tion.jda.api.entities.Activity.playing("Dota 2"))
        .addEventListeners(new Bot())
        .build();
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    Message msg = event.getMessage();
    String content = msg.getContentRaw();

    // Игнорировать сообщения от самого бота
    if (event.getAuthor().isBot()) {
      return;
    }

    String url;
    boolean isTikTok;

    if (content.startsWith("https://www.tiktok.com/") || content.startsWith("https://vm.tiktok.com/")) {
        url = content.trim();
      isTikTok = true;
    } else {
        isTikTok = false;
        if (content.startsWith("https://x.com/")) {
          url = content.trim();
        } else {
            // Игнорировать все остальные сообщения
          return;
        }
    }

    event
        .getChannel()
        .sendMessage("⏳ Хлео, качаю відео...")
        .queue(
            loadingMsg -> new Thread(
                    () -> {
                      try {
                        File file;
                        String fileType = null;
                        if (isTikTok) {
                          file = TikTokDownloader.downloadWithWatermark(url);
                        } else {
                          TwitterDownloader.TwitterMediaResult result = TwitterDownloader.downloadMediaWithType(url);
                          file = result.file;
                          fileType = result.type;
                        }
                        loadingMsg.delete().queue();
                        event.getMessage().delete().queue();
                        // Проверка размера файла перед отправкой (лимит Discord 8 МБ)
                        long maxSize = 8 * 1024 * 1024; // 8 МБ
                        File fileToSend = file;
                        String fileNameToSend = isTikTok ? "tiktok.mp4" : "twitter.mp4";
                        if (!isTikTok && fileType != null && fileType.equals("gif")) {
                          System.out.println("[DEBUG] Bot: Detected Twitter GIF, converting mp4 to gif...");
                          File gifFile = TwitterDownloader.convertMp4ToGif(file);
                          System.out.println("[DEBUG] Bot: Converted GIF file path: " + gifFile.getAbsolutePath() + ", size: " + gifFile.length() + " bytes");
                          fileToSend = gifFile;
                          fileNameToSend = "twitter.gif";
                        }
                        if (fileToSend.length() > maxSize) {
                          System.out.println("[DEBUG] Bot: File too large for Discord (" + fileToSend.length() + " bytes)");
                          event.getChannel()
                              .sendMessage(event.getAuthor().getAsMention() + ", ❌ Файл дуже великий, ліміт 8 МБ")
                              .queue();
                          return;
                        }
                        event.getChannel()
                            .sendMessage(event.getAuthor().getAsMention())
                            .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(fileToSend, fileNameToSend))
                            .queue();
                      } catch (InvalidMessageException e) {
                        e.printStackTrace();
                        event
                            .getChannel()
                            .sendMessage(event.getAuthor().getAsMention() + e.getMessage())
                            .queue();
                      } catch (Exception e) {
                        e.printStackTrace();
                        loadingMsg.delete().queue();
                        event.getMessage().delete().queue();
                        event
                            .getChannel()
                            .sendMessage(
                                event.getAuthor().getAsMention()
                                    + ", ❌ Шось не так чіклео, не можу скачати відео")
                            .queue();
                      }
                    })
                .start());
  }

  @Override
  public void onReady(ReadyEvent event) {
    System.out.println("[INFO] Бот полностью загружен и готов к работе!");
    System.out.println("[INFO] Всего серверов (по данным Discord): " + event.getGuildTotalCount());

    // Выводим список доступных серверов
    System.out.println(
        "[INFO] Доступные серверы в кэше (" + event.getJDA().getGuilds().size() + "):");
    event
        .getJDA()
        .getGuilds()
        .forEach(
            guild -> {
              System.out.println(
                  " -> ДОСТУПЕН: " + guild.getName() + " (ID: " + guild.getId() + ")");
            });

    // Выводим список НЕДОСТУПНЫХ серверов
    if (!event.getJDA().getUnavailableGuilds().isEmpty()) {
      System.out.println("[WARN] Обнаружены недоступные серверы!");
      event
          .getJDA()
          .getUnavailableGuilds()
          .forEach(
              guildId -> {
                System.out.println(" -> НЕДОСТУПЕН: ID " + guildId);
              });
    }
  }
}
