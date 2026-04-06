package ru.gazprombank.automation.akitagpb.modules.kafkamq.steps.artemis;

import io.cucumber.java.ru.И;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.config.ArtemisProperties;

/**
 * Шаги для управления подключением к Apache ActiveMQ Artemis.
 *
 * <p>Параметры подключения берутся из файла artemis.properties в src/test/resources. Если файл не
 * найден или обязательные поля не заполнены — тест падает сразу.
 *
 * <p>Соединение хранится в контексте сценария и доступно всем шагам. ClientID устанавливается один
 * раз при создании соединения — это позволяет создавать несколько durable подписок на одном
 * соединении.
 *
 * <p>Рекомендуется закрывать соединение в @After хуке через ArtemisHooks.
 */
public class ArtemisConnectionSteps extends ArtemisBaseSteps {

  /**
   * Создаёт подключение к Artemis из artemis.properties. ClientID генерируется автоматически как
   * уникальный UUID — это позволяет создавать несколько durable подписок на топики в рамках одного
   * сценария.
   *
   * <p>Пример artemis.properties: artemis.host=localhost artemis.port=61616 artemis.user=admin
   * artemis.password=admin artemis.protocol=tcp artemis.ssl.enabled=false
   * artemis.receive.timeout=5000
   */
  @И("^создано подключение к artemis$")
  @SneakyThrows
  public void createArtemisConnection() {
    var p = new ArtemisProperties();

    String protocol = p.isSslEnabled() ? "ssl" : p.getProtocol();
    String brokerUrl = String.format("%s://%s:%d", protocol, p.getHost(), p.getPort());

    if (p.isSslEnabled()) {
      System.setProperty("javax.net.ssl.keyStore", p.getSslKeyStore());
      System.setProperty("javax.net.ssl.keyStorePassword", p.getSslKeyStorePassword());
      System.setProperty("javax.net.ssl.trustStore", p.getSslTrustStore());
      System.setProperty("javax.net.ssl.trustStorePassword", p.getSslTrustStorePassword());
    }

    var cf = new ActiveMQConnectionFactory(brokerUrl);
    cf.setUser(p.getUser());
    cf.setPassword(p.getPassword());

    var connection = cf.createConnection();
    // ClientID устанавливается один раз здесь — до создания любых подписок.
    // Уникальный UUID позволяет параллельным тестам не конфликтовать на брокере.
    String clientId = "test-" + UUID.randomUUID();
    connection.setClientID(clientId);

    akitaScenario.setVar(CONNECTION_VAR, connection);
    akitaScenario.log("Artemis подключение создано: " + brokerUrl + ", clientId: " + clientId);
  }

  /**
   * Закрывает соединение с Artemis. Можно не вызывать явно если используется тег @artemis —
   * ArtemisHooks закроет соединение автоматически в @After.
   */
  @И("^закрыть artemis соединение$")
  @SneakyThrows
  public void closeArtemisConnection() {
    getConnection().close();
    akitaScenario.setVar(CONNECTION_VAR, null);
    akitaScenario.log("Artemis соединение закрыто");
  }
}
