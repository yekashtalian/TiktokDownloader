package com.yekashtalian;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppProperties {
  private static final Properties props = new Properties();

  static {
    try (InputStream input =
        AppProperties.class.getClassLoader().getResourceAsStream("application.properties")) {
      props.load(input);
      // Подстановка переменных окружения
      Pattern envPattern = Pattern.compile("\\$\\{([A-Za-z0-9_]+)\\}");
      for (String key : props.stringPropertyNames()) {
        String value = props.getProperty(key);
        if (value != null) {
          Matcher matcher = envPattern.matcher(value);
          StringBuffer sb = new StringBuffer();
          while (matcher.find()) {
            String envVar = matcher.group(1);
            String envValue = System.getenv(envVar);
            matcher.appendReplacement(sb, envValue != null ? Matcher.quoteReplacement(envValue) : "");
          }
          matcher.appendTail(sb);
          props.setProperty(key, sb.toString());
        }
      }
    } catch (IOException ex) {
      throw new RuntimeException("Error while loading configuration properties");
    }
  }

  public static String get(String key) {
    return props.getProperty(key);
  }
}
