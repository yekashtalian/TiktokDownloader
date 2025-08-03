package com.yekashtalian;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TwitterDownloader {
    /**
     * Скачивает видео или гифку по ссылке на x.com (twitter) через twdown.net
     * @param tweetUrl ссылка на твит
     * @return File с видео/гифкой
     * @throws Exception если не удалось скачать
     */
    public static File downloadMedia(String tweetUrl) throws Exception {
        // 1. Отправляем POST-запрос на twdown.net
        Document doc = Jsoup.connect("https://twdown.net/download.php")
                .data("URL", tweetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9,ru;q=0.8")
                .header("Referer", "https://twdown.net/")
                .header("Origin", "https://twdown.net")
                .header("Connection", "keep-alive")
                .timeout(20000)
                .post();

        // 2. Ищем прямую ссылку на видео (обычно кнопка Download Video)
        Elements links = doc.select("a[href][download]");
        String mediaUrl = null;
        for (Element link : links) {
            String href = link.attr("href");
            if (href.contains(".mp4")) {
                mediaUrl = href;
                break;
            }
        }
        if (mediaUrl == null) throw new IOException("Не удалось найти ссылку на медиа");
        // 3. Скачиваем файл
        URL url = new URL(mediaUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        InputStream in = conn.getInputStream();
        File tempFile = File.createTempFile("twitter_media_", ".mp4");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        in.close();
        return tempFile;
    }
}
