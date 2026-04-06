package ru.gazprombank.automation.akitagpb.modules.core.helpers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/** Класс для получения конфигов */
@Slf4j
public class ConfigLoader {

  private static Config defaultConfig = ConfigFactory.load();

  private ConfigLoader() {}

  public static <T> T getConfigValue(String configFile, String name) {
    try {
      return (T) ConfigFactory.load(configFile).getAnyRef(name);
    } catch (ConfigException e) {
      return null;
    }
  }

  public static <T> T getConfigValue(String name) {
    try {
      return (T) defaultConfig.getAnyRef(name);
    } catch (ConfigException e) {
      return null;
    }
  }

  public static <T> T getConfigValueOrDefault(String configFile, String name, T defaultValue) {
    T configValue = getConfigValue(configFile, name);
    return configValue != null ? configValue : defaultValue;
  }

  public static <T> T getConfigValueOrDefault(String name, T defaultValue) {
    T configValue = getConfigValue(name);
    return configValue != null ? configValue : defaultValue;
  }

  public static void resolveSecrets(Map<String, String> secrets) {
    Config config = defaultConfig;
    for (Entry<String, ConfigValue> entry : defaultConfig.entrySet()) {
      Object rawValue = entry.getValue().unwrapped();
      if (rawValue instanceof String parametrized && parametrized.matches(".*\\$\\{.+}.*")) {
        Pattern pattern = Pattern.compile("\\$\\{(.+?)}");
        Matcher matcher = pattern.matcher(parametrized);
        if (matcher.find()) {
          var varName = matcher.group(1);
          if (secrets.containsKey(varName)) {
            config =
                config.withValue(
                    entry.getKey(), ConfigValueFactory.fromAnyRef(secrets.get(varName)));
          }
        }
      }
    }
    defaultConfig = config;
  }
}
