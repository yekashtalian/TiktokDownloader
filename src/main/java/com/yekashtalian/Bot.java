package com.yekashtalian;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;

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
    String author = msg.getAuthor().getAsTag();
    String channel = event.getChannel().getName();
    String guild = event.isFromGuild() ? event.getGuild().getName() : "DM";

    System.out.println(
        "[DEBUG] Получено сообщение: "
            + content
            + " от "
            + author
            + " (IsFromGuild: "
            + event.isFromGuild()
            + ")");

    // Игнорировать сообщения от самого бота
    if (event.getAuthor().isBot()) {
      System.out.println("[DEBUG] Сообщение от бота, пропускаю.");
      return;
    }

    if (content.startsWith("https://www.tiktok.com/")) {
      String url = content.trim();
      event
          .getChannel()
          .sendMessage("⏳ Хлео, качаю відео жди бля")
          .queue(
              loadingMsg -> {
                new Thread(
                        () -> {
                          try {
                            var file = TikTokDownloader.downloadWithWatermark(url);
                            // Удаляем сообщение о загрузке
                            loadingMsg.delete().queue();
                            // Удаляем исходное сообщение с ссылкой
                            event.getMessage().delete().queue();
                            // Отправляем видео с упоминанием автора
                            event
                                .getChannel()
                                .sendMessage(event.getAuthor().getAsMention())
                                .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(file))
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
                    .start();
              });
    }
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
