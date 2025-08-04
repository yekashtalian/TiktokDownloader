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

    /**
     * Результат скачивания медиа с x.com
     */
    public static class TwitterMediaResult {
        public final File file;
        public final String type; // "gif" или "mp4"
        public TwitterMediaResult(File file, String type) {
            this.file = file;
            this.type = type;
        }
    }

    /**
     * Скачивает видео или гифку по ссылке на x.com (twitter) через twdown.net
     * @param tweetUrl ссылка на твит
     * @return TwitterMediaResult с файлом и типом ("gif" или "mp4")
     * @throws Exception если не удалось скачать
     */
    public static TwitterMediaResult downloadMediaWithType(String tweetUrl) throws Exception {
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

        Elements links = doc.select("a[href][download]");
        String mediaUrl = null;
        String type = "mp4";
        for (Element link : links) {
            String href = link.attr("href");
            String text = link.text().toLowerCase();
            System.out.println("[DEBUG] TwitterDownloader: link text='" + text + "', href='" + href + "'");
            if (href.contains(".mp4")) {
                mediaUrl = href;
                // Определяем gif по tweet_video в ссылке
                if (href.contains("/tweet_video/")) {
                    type = "gif";
                    System.out.println("[DEBUG] TwitterDownloader: Detected GIF by /tweet_video/ in URL");
                }
                break;
            }
        }
        System.out.println("[DEBUG] TwitterDownloader: mediaUrl='" + mediaUrl + "', type='" + type + "'");
        if (mediaUrl == null) throw new IOException("Не удалось найти ссылку на медиа");
        URL url = new URL(mediaUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        InputStream in = conn.getInputStream();
        File tempFile = File.createTempFile("twitter_media_", type.equals("gif") ? ".gif" : ".mp4");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        in.close();
        System.out.println("[DEBUG] TwitterDownloader: saved file '" + tempFile.getAbsolutePath() + "' as type '" + type + "'");
        return new TwitterMediaResult(tempFile, type);
    }

    /**
     * Конвертирует mp4-файл в gif с помощью ffmpeg
     * @param mp4File исходный mp4-файл
     * @return File с gif-анимацией
     * @throws Exception если не удалось конвертировать
     */
    public static File convertMp4ToGif(File mp4File) throws Exception {
        System.out.println("[DEBUG] convertMp4ToGif: Start conversion. Input file: " + mp4File.getAbsolutePath() + ", size: " + mp4File.length() + " bytes");
        File gifFile = File.createTempFile("twitter_gif_", ".gif");
        String ffmpegPath = "ffmpeg";
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-y",
                "-i", mp4File.getAbsolutePath(),
                "-vf", "fps=15,scale=480:-1:flags=lanczos",
                "-loop", "0",
                gifFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[FFMPEG] " + line);
            }
        }
        int exitCode = process.waitFor();
        System.out.println("[DEBUG] convertMp4ToGif: ffmpeg exit code: " + exitCode);
        System.out.println("[DEBUG] convertMp4ToGif: Output file: " + gifFile.getAbsolutePath() + ", size: " + gifFile.length() + " bytes");
        if (exitCode != 0) {
            throw new IOException("ffmpeg завершился с кодом " + exitCode);
        }
        return gifFile;
    }
}
