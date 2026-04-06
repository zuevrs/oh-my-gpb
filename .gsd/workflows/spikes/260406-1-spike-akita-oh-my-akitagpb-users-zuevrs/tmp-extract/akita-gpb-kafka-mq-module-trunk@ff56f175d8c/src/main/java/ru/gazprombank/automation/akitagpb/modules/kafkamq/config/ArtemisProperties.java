package ru.gazprombank.automation.akitagpb.modules.kafkamq.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Загружает параметры подключения к Artemis из artemis.properties в resources.
 *
 * <p>Приоритет значений: 1. System property (-D параметр Maven/CI) 2. artemis.properties файл 3.
 * Default значение
 *
 * <p>Пример artemis.properties: artemis.host=localhost artemis.port=61616 artemis.user=admin
 * artemis.password=admin artemis.protocol=tcp artemis.ssl.enabled=false
 * artemis.receive.timeout=5000
 */
public class ArtemisProperties {

  private static final String PROPERTIES_FILE = "artemis.properties";
  private final Properties props;

  public ArtemisProperties() {
    props = new Properties();

    try (InputStream is =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTIES_FILE)) {

      if (is == null) {
        throw new IllegalStateException(
            String.format(
                "Файл '%s' не найден в resources. " + "Создайте файл src/test/resources/%s",
                PROPERTIES_FILE, PROPERTIES_FILE));
      }

      props.load(is);

    } catch (IOException e) {
      throw new IllegalStateException("Ошибка при чтении файла " + PROPERTIES_FILE, e);
    }

    validateRequiredProperties();
  }

  // ═════════════════════════════════════════════════════════════════════════
  // ВАЛИДАЦИЯ
  // ═════════════════════════════════════════════════════════════════════════

  private void validateRequiredProperties() {
    String[] required = {"artemis.host", "artemis.port", "artemis.user", "artemis.password"};

    StringBuilder missing = new StringBuilder();
    for (String key : required) {
      String value = get(key, null);
      if (value == null || value.isBlank()) {
        missing.append("\n  - ").append(key);
      }
    }

    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "В файле " + PROPERTIES_FILE + " отсутствуют обязательные свойства:" + missing);
    }

    if (isSslEnabled()) {
      validateSslProperties();
    }
  }

  private void validateSslProperties() {
    String[] sslRequired = {
      "artemis.ssl.keyStore",
      "artemis.ssl.keyStorePassword",
      "artemis.ssl.trustStore",
      "artemis.ssl.trustStorePassword"
    };

    StringBuilder missing = new StringBuilder();
    for (String key : sslRequired) {
      String value = get(key, null);
      if (value == null || value.isBlank()) {
        missing.append("\n  - ").append(key);
      }
    }

    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "SSL включён (artemis.ssl.enabled=true), "
              + "но не заполнены обязательные свойства:"
              + missing);
    }
  }

  // ═════════════════════════════════════════════════════════════════════════
  // ГЕТТЕРЫ
  // ═════════════════════════════════════════════════════════════════════════

  public String getHost() {
    return get("artemis.host", null);
  }

  public int getPort() {
    return Integer.parseInt(get("artemis.port", "61616"));
  }

  public String getUser() {
    return get("artemis.user", null);
  }

  public String getPassword() {
    return get("artemis.password", null);
  }

  /** tcp по умолчанию если не указан */
  public String getProtocol() {
    return get("artemis.protocol", "tcp");
  }

  /**
   * Таймаут ожидания сообщения в миллисекундах. По умолчанию 5000 мс (5 секунд). Переопределить:
   * -Dartemis.receive.timeout=30000
   */
  public long getReceiveTimeout() {
    return Long.parseLong(get("artemis.receive.timeout", "30000"));
  }

  public boolean isSslEnabled() {
    return Boolean.parseBoolean(get("artemis.ssl.enabled", "false"));
  }

  public String getSslKeyStore() {
    return get("artemis.ssl.keyStore", null);
  }

  public String getSslKeyStorePassword() {
    return get("artemis.ssl.keyStorePassword", null);
  }

  public String getSslTrustStore() {
    return get("artemis.ssl.trustStore", null);
  }

  public String getSslTrustStorePassword() {
    return get("artemis.ssl.trustStorePassword", null);
  }

  // ═════════════════════════════════════════════════════════════════════════
  // ПРИВАТНЫЕ МЕТОДЫ
  // ═════════════════════════════════════════════════════════════════════════

  /**
   * Возвращает значение с приоритетом: 1. System property (-Dartemis.host=...) 2.
   * artemis.properties файл 3. defaultValue
   */
  private String get(String key, String defaultValue) {
    String systemValue = System.getProperty(key);
    if (systemValue != null && !systemValue.isBlank()) {
      return systemValue;
    }
    String fileValue = props.getProperty(key);
    if (fileValue != null && !fileValue.isBlank()) {
      return fileValue;
    }
    return defaultValue;
  }
}
