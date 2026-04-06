package ru.gazprombank.automation.akitagpb.modules.kafkamq.steps.artemis;

import static ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables.resolveVars;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.И;
import java.util.List;
import java.util.Optional;
import javax.jms.DeliveryMode;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import lombok.SneakyThrows;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.config.ArtemisProperties;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.mq.artemis.ArtemisRequestParam;

/**
 * Шаги для работы с топиками Artemis (Multicast / Publish-Subscribe).
 *
 * <p>Модель доставки: одно сообщение -> все подписчики получают свою копию. Используется durable
 * подписка — Artemis хранит сообщения для подписчика даже если он был офлайн в момент отправки.
 *
 * <p>ВАЖНО: подписку нужно создавать ДО того как сервис отправит сообщение, иначе сообщение будет
 * потеряно для этого подписчика.
 *
 * <p>Селектор указывается при создании подписки — брокер фильтрует сообщения при поступлении и
 * кладёт в персональную очередь только совпавшие. Это правильный подход — не нужно читать лишние
 * сообщения на клиенте.
 */
public class ArtemisTopicSteps extends ArtemisBaseSteps {

  // =========================================================================
  // ОТПРАВКА
  // =========================================================================

  /**
   * Отправляет сообщение в топик.
   *
   * <p>Формат таблицы: | type | name | value | | BODY | body | payment_event.json | | PROPERTY |
   * CorrelationID | ${correlationId} | <- опционально
   */
  @И("^отправлено сообщение в топик \"(.*)\" с параметрами из таблицы$")
  @SneakyThrows
  public void sendToTopic(String topicName, List<ArtemisRequestParam> params) {
    var connection = getConnection();
    var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    var topic = session.createTopic("multicast://" + topicName);
    var producer = session.createProducer(topic);
    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    var message = buildMessage(session, params);
    producer.send(message);
    akitaScenario.log("Отправлено в топик: " + topicName);
    producer.close();
    session.close();
  }

  // =========================================================================
  // ПОДПИСКА
  // =========================================================================

  /**
   * Создаёт durable подписку на топик без фильтра. Подписчик получает все сообщения из топика.
   *
   * <p>ВАЖНО: вызывать ДО триггера действия которое публикует событие.
   */
  @И("^создана подписка на топик \"([^\"]+)\" с именем \"([^\"]+)\"$")
  @SneakyThrows
  public void createTopicSubscription(String topicName, String subscriptionName) {
    createSubscription(topicName, subscriptionName, null);
  }

  /**
   * Создаёт durable подписку на топик с одним PROPERTY фильтром. Брокер кладёт в персональную
   * очередь только совпавшие сообщения.
   *
   * <p>Пример: И создана подписка на топик "PAYMENT.EVENTS" с именем "audit-consumer" по property
   * "CorrelationID" равному "pas2"
   */
  @И("^создана подписка на топик \"(.*)\" с именем \"(.*)\" по property \"(.*)\" равному \"(.*)\"$")
  @SneakyThrows
  public void createTopicSubscriptionWithProperty(
      String topicName, String subscriptionName, String propertyName, String propertyValue) {
    String resolvedValue =
        BaseMethods.getInstance().getPropertyOrStringVariableOrValue(propertyValue);
    resolvedValue = resolveVars(resolvedValue);
    String selector = propertyName + " = '" + resolvedValue + "'";
    createSubscription(topicName, subscriptionName, selector);
  }

  /**
   * Создаёт durable подписку на топик с несколькими property фильтрами. Все условия объединяются
   * через AND. Брокер кладёт в персональную очередь только совпавшие сообщения.
   *
   * <p>Таблица: | имя property | значение |
   *
   * <p>Пример: И создана подписка на топик "PAYMENT.EVENTS" с именем "audit-consumer" с property
   * таблицей | CorrelationID | pas2 | | eventType | PAYMENT_CREATED |
   *
   * <p>Результирующий селектор: CorrelationID = 'pas2' AND eventType = 'PAYMENT_CREATED'
   */
  @И("^создана подписка на топик \"(.*)\" с именем \"(.*)\" с property таблицей$")
  @SneakyThrows
  public void createTopicSubscriptionWithPropertyTable(
      String topicName, String subscriptionName, DataTable dataTable) {
    StringBuilder selector = new StringBuilder();
    for (List<String> row : dataTable.cells()) {
      String name = row.get(0);
      String value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(row.get(1));
      value = resolveVars(value);
      if (selector.length() > 0) {
        selector.append(" AND ");
      }
      selector.append(name).append(" = '").append(value).append("'");
    }
    createSubscription(topicName, subscriptionName, selector.toString());
  }

  // =========================================================================
  // ЧТЕНИЕ
  // =========================================================================

  /**
   * Читает первое сообщение из персональной очереди подписчика. Таймаут берётся из
   * artemis.properties (artemis.receive.timeout).
   */
  @И(
      "^прочитано сообщение из топика по подписке \"([^\"]+)\". Результат сохранен в переменную \"([^\"]+)\"$")
  @SneakyThrows
  public void readFromTopic(String subscriptionName, String varName) {
    receiveFromTopic(subscriptionName, varName);
  }

  // =========================================================================
  // ОЧИСТКА
  // =========================================================================

  /**
   * Удаляет durable подписку с брокера. Вызывать после теста — ArtemisHooks делает это
   * автоматически в @After. Накопленные непрочитанные сообщения для этой подписки также удалятся.
   */
  @И("^удалена подписка на топик с именем \"(.*)\"$")
  @SneakyThrows
  public void removeTopicSubscription(String subscriptionName) {
    var existing = (MessageConsumer) akitaScenario.getVar("__subscriber__" + subscriptionName);
    if (existing != null) {
      existing.close();
    }
    var session = getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
    session.unsubscribe(subscriptionName);
    session.close();
    akitaScenario.setVar("__subscriber__" + subscriptionName, null);
    akitaScenario.setVar("__topic__" + subscriptionName, null);
    akitaScenario.log("Удалена подписка: " + subscriptionName);
  }

  // =========================================================================
  // ПРИВАТНЫЕ МЕТОДЫ
  // =========================================================================

  /**
   * Общий метод создания durable подписки. selector == null -> подписчик получает все сообщения.
   * selector != null -> брокер фильтрует при поступлении сообщений.
   */
  @SneakyThrows
  private void createSubscription(String topicName, String subscriptionName, String selector) {
    var connection = getConnection();
    var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    var topic = session.createTopic("multicast://" + topicName);
    var subscriber =
        selector != null
            ? session.createDurableSubscriber(topic, subscriptionName, selector, false)
            : session.createDurableSubscriber(topic, subscriptionName);
    akitaScenario.setVar("__subscriber__" + subscriptionName, subscriber);
    akitaScenario.setVar("__topic__" + subscriptionName, topicName);
    akitaScenario.log(
        "Создана подписка '"
            + subscriptionName
            + "' на топик: "
            + topicName
            + (selector != null ? ", селектор: " + selector : ""));
  }

  /**
   * Читает сообщение из персональной очереди подписчика. Таймаут берётся из artemis.properties
   * (artemis.receive.timeout).
   */
  @SneakyThrows
  private void receiveFromTopic(String subscriptionName, String varName) {
    long timeout = new ArtemisProperties().getReceiveTimeout();
    var connection = getConnection();
    connection.start();

    var subscriber = (MessageConsumer) akitaScenario.getVar("__subscriber__" + subscriptionName);
    if (subscriber == null) {
      throw new IllegalStateException(
          "Подписка '"
              + subscriptionName
              + "' не найдена. "
              + "Добавьте шаг 'создана подписка на топик' перед чтением");
    }

    var message =
        Optional.ofNullable(subscriber.receive(timeout))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Сообщение не получено за "
                            + timeout
                            + "мс по подписке: "
                            + subscriptionName));

    var textMessage = (TextMessage) message;
    String text = textMessage.getText();
    akitaScenario.log("Сообщение из топика [" + subscriptionName + "]: " + text);
    message.acknowledge();

    saveHeaders(textMessage, varName);
    akitaScenario.setVar(varName, text);
  }
}
