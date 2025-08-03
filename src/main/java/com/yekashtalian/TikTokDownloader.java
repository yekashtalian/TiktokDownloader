package com.yekashtalian;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;

public class TikTokDownloader {
    public static File downloadWithWatermark(String tiktokUrl) throws Exception {
        String rapidApiKey = getRapidApiKey();
        String json = fetchVideoInfoJson(tiktokUrl, rapidApiKey);
        String videoUrl = extractDownloadUrl(json);
        return downloadVideoToFile(videoUrl);
    }

    private static String getRapidApiKey() {
        String rapidApiKey = AppProperties.get("rapidapi.key");
        if (rapidApiKey == null || rapidApiKey.isBlank()) {
            throw new IllegalStateException("RapidAPI ключ не найден. Добавьте rapidapi.key в application.properties");
        }
        return rapidApiKey;
    }

    private static String fetchVideoInfoJson(String tiktokUrl, String rapidApiKey) throws Exception {
        String apiUrl = "https://tiktok-video-downloader-api.p.rapidapi.com/media";
        String charset = "UTF-8";
        String query = String.format("videoUrl=%s", URLEncoder.encode(tiktokUrl, charset));
        URL url = new URL(apiUrl + "?" + query);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("x-rapidapi-key", rapidApiKey);
        conn.setRequestProperty("x-rapidapi-host", "tiktok-video-downloader-api.p.rapidapi.com");
        conn.setRequestProperty("Accept", "application/json");
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IllegalStateException("Ошибка RapidAPI: код ответа " + responseCode);
        }
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return response.toString();
    }

    private static String extractDownloadUrl(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        String videoUrl = root.path("downloadUrl").asText();
        if (videoUrl == null || videoUrl.isEmpty()) {
            throw new IllegalStateException("Не удалось получить ссылку на видео через RapidAPI");
        }
        return videoUrl;
    }

    private static File downloadVideoToFile(String videoUrl) throws Exception {
        URL video = new URL(videoUrl);
        try (BufferedInputStream bis = new BufferedInputStream(video.openStream())) {
            Path tempFile = Files.createTempFile("tiktok-", ".mp4");
            try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                byte[] buffer = new byte[1024];
                int count;
                while ((count = bis.read(buffer, 0, 1024)) != -1) {
                    fos.write(buffer, 0, count);
                }
            }
            return tempFile.toFile();
        }
    }
}
