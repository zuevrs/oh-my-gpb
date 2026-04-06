package ru.gazprombank.automation.akitagpb.modules.kafkamq.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;

/**
 * Загружает параметры подключения к Kafka из kafka.properties в resources.
 *
 * <p>Приоритет значений: 1. System property (-D параметр Maven/CI) 2. kafka.properties файл 3.
 * Default значение
 *
 * <p>Путь к файлу конфига переопределяется через:
 * -Dglobal.kafka.properties.path=/my-kafka.properties
 *
 * <p>Пример kafka.properties: bootstrap.servers=localhost:9092 security.protocol=PLAINTEXT
 * ssl.enabled=false ssl.keystore.location=certs/keystore.jks ssl.keystore.password=changeit
 * ssl.truststore.location=certs/truststore.jks ssl.truststore.password=changeit
 */
public class KafkaProperties {

  private static final String DEFAULT_PROPERTIES_FILE = "kafka.properties";
  private final Properties props;
  private final String propertiesFile;

  public KafkaProperties() {
    // Путь к файлу: System property → default
    this.propertiesFile =
        System.getProperty("global.kafka.properties.path", DEFAULT_PROPERTIES_FILE);

    props = new Properties();

    try (InputStream is =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(propertiesFile)) {

      if (is == null) {
        throw new IllegalStateException(
            String.format(
                "Файл '%s' не найден в resources. " + "Создайте файл src/test/resources/%s",
                propertiesFile, propertiesFile));
      }

      props.load(is);

    } catch (IOException e) {
      throw new IllegalStateException("Ошибка при чтении файла " + propertiesFile, e);
    }

    validateRequiredProperties();
    resolveAbsolutePaths();
    applySystemPasswordOverrides();
  }

  // =========================================================================
  // ВАЛИДАЦИЯ
  // =========================================================================

  private void validateRequiredProperties() {
    String[] required = {"bootstrap.servers", "group.id"};

    StringBuilder missing = new StringBuilder();
    for (String key : required) {
      String value = get(key, null);
      if (value == null || value.isBlank()) {
        missing.append("\n  - ").append(key);
      }
    }

    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "В файле " + propertiesFile + " отсутствуют обязательные свойства:" + missing);
    }

    if (isSslEnabled()) {
      validateSslProperties();
    }
  }

  private void validateSslProperties() {
    String[] sslRequired = {
      "ssl.keystore.location",
      "ssl.keystore.password",
      "ssl.truststore.location",
      "ssl.truststore.password"
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
          "SSL включён (ssl.enabled=true или security.protocol=SSL/SASL_SSL), "
              + "но не заполнены обязательные свойства:"
              + missing);
    }
  }

  // =========================================================================
  // ПУТИ К KEYSTORE / TRUSTSTORE
  // =========================================================================

  /**
   * Преобразует относительные пути ssl.keystore.location и ssl.truststore.location в абсолютные
   * относительно рабочей директории проекта. Если пути не заданы — пропускает (SSL может быть
   * отключён).
   */
  private void resolveAbsolutePaths() {
    String userDir = System.getProperty("user.dir") + "/";

    String keystorePath = props.getProperty("ssl.keystore.location");
    if (keystorePath != null && !keystorePath.isBlank()) {
      String absolute =
          new File(FilenameUtils.normalize(userDir + keystorePath)).getAbsoluteFile().toString();
      props.setProperty("ssl.keystore.location", absolute);
    }

    String truststorePath = props.getProperty("ssl.truststore.location");
    if (truststorePath != null && !truststorePath.isBlank()) {
      String absolute =
          new File(FilenameUtils.normalize(userDir + truststorePath)).getAbsoluteFile().toString();
      props.setProperty("ssl.truststore.location", absolute);
    }
  }

  /**
   * Применяет пароли из System properties если переданы. Позволяет не хранить пароли в файле
   * конфига. Переопределить: -Dkeystore.password=secret -Dtruststore.password=secret
   */
  private void applySystemPasswordOverrides() {
    String keystorePassword = System.getProperty("keystore.password");
    String truststorePassword = System.getProperty("truststore.password");
    if (keystorePassword != null) {
      props.setProperty("ssl.keystore.password", keystorePassword);
    }
    if (truststorePassword != null) {
      props.setProperty("ssl.truststore.password", truststorePassword);
    }
  }

  // =========================================================================
  // ГЕТТЕРЫ
  // =========================================================================

  /**
   * Таймаут ожидания сообщения в миллисекундах. По умолчанию 5000 мс (5 секунд). Переопределить:
   * -Dkafka.receive.timeout=30000
   */
  public long getReceiveTimeout() {
    return Long.parseLong(get("kafka.receive.timeout", "5000"));
  }

  public String getGroupId() {
    return get("group.id", null);
  }

  /**
   * Поведение при отсутствии сохранённого offset. По умолчанию latest — читать только новые
   * сообщения. Переопределить: -Dauto.offset.reset=earliest
   */
  public String getAutoOffsetReset() {
    return get("auto.offset.reset", "latest");
  }

  /** Автоматический коммит offset. По умолчанию true. Переопределить: -Denable.auto.commit=false */
  public boolean isAutoCommitEnabled() {
    return Boolean.parseBoolean(get("enable.auto.commit", "true"));
  }

  public String getBootstrapServers() {
    return get("bootstrap.servers", null);
  }

  /** Возвращает security.protocol. По умолчанию PLAINTEXT если не указан. */
  public String getSecurityProtocol() {
    return get("security.protocol", "PLAINTEXT");
  }

  /** SSL считается включённым если: ssl.enabled=true или security.protocol содержит SSL */
  public boolean isSslEnabled() {
    if (Boolean.parseBoolean(get("ssl.enabled", "false"))) {
      return true;
    }
    String protocol = getSecurityProtocol();
    return protocol.contains("SSL");
  }

  public String getSslKeystoreLocation() {
    return props.getProperty("ssl.keystore.location");
  }

  public String getSslKeystorePassword() {
    return get("ssl.keystore.password", null);
  }

  public String getSslTruststoreLocation() {
    return props.getProperty("ssl.truststore.location");
  }

  public String getSslTruststorePassword() {
    return get("ssl.truststore.password", null);
  }

  /**
   * Возвращает полный объект Properties для передачи в KafkaProducer / KafkaConsumer. Содержит все
   * свойства из файла с применёнными переопределениями.
   */
  public Properties toProperties() {
    Properties result = new Properties();
    result.putAll(props);
    // bootstrap.servers может быть переопределён через System property
    String bootstrapOverride = System.getProperty("bootstrap.servers");
    if (bootstrapOverride != null && !bootstrapOverride.isBlank()) {
      result.setProperty("bootstrap.servers", bootstrapOverride);
    }
    return result;
  }

  // =========================================================================
  // ПРИВАТНЫЕ МЕТОДЫ
  // =========================================================================

  /**
   * Возвращает значение с приоритетом: 1. System property (-Dkey=value) 2. kafka.properties файл 3.
   * defaultValue
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
