package ru.gazprombank.automation.akitagpb.modules.kafkamq.steps.artemis;

import static ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables.resolveVars;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.И;
import java.util.List;
import javax.jms.DeliveryMode;
import javax.jms.Session;
import javax.jms.TextMessage;
import lombok.SneakyThrows;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.mq.artemis.ArtemisRequestParam;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.mq.artemis.ArtemisRequestParamType;

/**
 * Шаги для работы с очередями Artemis (Anycast / Point-to-Point).
 *
 * <p>Модель доставки: одно сообщение → один получатель. Сообщение удаляется из очереди после
 * чтения.
 *
 * <p>Рекомендации: - Всегда добавляйте PROPERTY с correlationId для надёжного чтения - Используйте
 * чтение по property при параллельном запуске тестов - Browse используйте когда нужно проверить
 * сообщение не удаляя его
 *
 * <p>Варианты фильтрации при чтении: 1. по property "key" равному "value" — одно условие 2. по
 * property таблице | key | value | — несколько условий AND 3. по селектору "..." — сложные условия:
 * OR, LIKE, IN, числа
 */
public class ArtemisQueueSteps extends ArtemisBaseSteps {

  // =========================================================================
  // ОТПРАВКА
  // =========================================================================

  /**
   * Отправляет сообщение в очередь.
   *
   * <p>Формат таблицы: | type | name | value | | PROPERTY | correlationId | ${correlationId} | ←
   * для фильтрации при чтении | BODY | body | request.json | ← тело сообщения
   *
   * <p>⚠️ Без PROPERTY надёжное чтение возможно только если очередь пустая.
   */
  @И("^отправлено сообщение в очередь \"(.*)\" с параметрами из таблицы$")
  @SneakyThrows
  public void sendToQueue(String queue, List<ArtemisRequestParam> params) {
    var connection = getConnection();
    var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    var dest = session.createQueue(queue);
    var producer = session.createProducer(dest);
    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    var message = buildMessage(session, params);

    warnIfNoProperty(params, queue);

    producer.send(message);
    akitaScenario.log("Отправлено в очередь: " + queue);
    producer.close();
    session.close();
  }

  /**
   * Отправляет сообщение в очередь и сохраняет MessageID.
   *
   * <p>Формат таблицы: | type | name | value | | PROPERTY | correlationId | ${correlationId} | |
   * BODY | body | request.json |
   */
  @И(
      "^отправлено сообщение в очередь \"(.*)\" с параметрами из таблицы. MessageID сохранен в переменную \"(.*)\"$")
  @SneakyThrows
  public void sendToQueueSaveId(String queue, String varName, List<ArtemisRequestParam> params) {
    var connection = getConnection();
    var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    var dest = session.createQueue(queue);
    var producer = session.createProducer(dest);
    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    var message = buildMessage(session, params);

    warnIfNoProperty(params, queue);

    producer.send(message);
    String id = message.getJMSMessageID();
    akitaScenario.setVar(varName, id);
    akitaScenario.log("Отправлено в очередь: " + queue + ", MessageID: " + id);
    producer.close();
    session.close();
  }

  // =========================================================================
  // ЧТЕНИЕ
  // =========================================================================

  /**
   * Читает первое сообщение из очереди. Бросает исключение если очередь пуста в течение
   * artemis.receive.timeout.
   *
   * <p>⚠️ Используйте только если очередь гарантированно пустая перед тестом. При параллельном
   * запуске тестов используйте чтение по property.
   */
  @И(
      "^прочитано первое сообщение из очереди \"([^\"]+)\". Результат сохранен в переменную \"([^\"]+)\"$")
  @SneakyThrows
  public void readFirstFromQueue(String queueName, String varName) {
    receiveFromQueue(queueName, null, varName);
  }

  /**
   * Читает сообщение из очереди по одному PROPERTY. Фреймворк строит селектор: propertyName =
   * 'propertyValue'. Брокер фильтрует на своей стороне — остальные сообщения не затрагиваются.
   *
   * <p>✅ Основной способ чтения — надёжен при параллельном запуске тестов.
   */
  @И(
      "^прочитано сообщение из очереди \"(.*)\" по property \"(.*)\" равному \"(.*)\". Результат сохранен в переменную \"(.*)\"$")
  @SneakyThrows
  public void readFromQueueByProperty(
      String queueName, String propertyName, String propertyValue, String varName) {
    String selector = buildSingleSelector(propertyName, propertyValue);
    receiveFromQueue(queueName, selector, varName);
  }

  /**
   * Читает сообщение из очереди по нескольким PROPERTY. Все условия объединяются через AND. Брокер
   * фильтрует на своей стороне — остальные сообщения не затрагиваются.
   *
   * <p>Таблица: | имя property | значение |
   *
   * <p>Пример: И прочитано сообщение из очереди "ORDERS.RESPONSE" по property таблице |
   * correlationId | ${correlationId} | | orderType | EXPRESS | Результат сохранен в переменную
   * "response"
   *
   * <p>Результирующий селектор: correlationId = '...' AND orderType = 'EXPRESS'
   */
  @И("^прочитано сообщение из очереди \"(.*)\" по property таблице$")
  @SneakyThrows
  public void readFromQueueByPropertyTable(String queueName, DataTable dataTable, String varName) {
    String selector = buildTableSelector(dataTable);
    receiveFromQueue(queueName, selector, varName);
  }

  /**
   * Читает сообщение из очереди по произвольному JMS селектору. Используется для сложных условий
   * которые нельзя выразить таблицей: OR, LIKE, IN, числовые сравнения (amount > 1000).
   *
   * <p>Пример: И прочитано сообщение из очереди "ORDERS.RESPONSE" по селектору "correlationId =
   * '${id}' AND orderType = 'EXPRESS'" Результат сохранен в переменную "response"
   */
  @И(
      "^прочитано сообщение из очереди \"(.*)\" по селектору \"(.*)\". Результат сохранен в переменную \"(.*)\"$")
  @SneakyThrows
  public void readFromQueueBySelector(String queueName, String selector, String varName) {
    String resolvedSelector =
        BaseMethods.getInstance().getPropertyOrStringVariableOrValue(selector);
    resolvedSelector = resolveVars(resolvedSelector);
    receiveFromQueue(queueName, resolvedSelector, varName);
  }

  // =========================================================================
  // BROWSE — чтение без удаления
  // =========================================================================

  /**
   * Просматривает первое сообщение из очереди БЕЗ удаления. Сообщение остаётся в очереди для
   * настоящего потребителя.
   */
  @И(
      "^просмотрено первое сообщение из очереди \"([^\"]+)\". Результат сохранен в переменную \"([^\"]+)\"$")
  @SneakyThrows
  public void browseFirstFromQueue(String queueName, String varName) {
    browseFromQueue(queueName, null, varName);
  }

  /**
   * Просматривает сообщение из очереди по одному PROPERTY БЕЗ удаления. Сообщение остаётся в
   * очереди для настоящего потребителя.
   */
  @И(
      "^просмотрено сообщение из очереди \"(.*)\" по property \"(.*)\" равному \"(.*)\". Результат сохранен в переменную \"(.*)\"$")
  @SneakyThrows
  public void browseFromQueueByProperty(
      String queueName, String propertyName, String propertyValue, String varName) {
    String selector = buildSingleSelector(propertyName, propertyValue);
    browseFromQueue(queueName, selector, varName);
  }

  /**
   * Просматривает сообщение из очереди по нескольким PROPERTY БЕЗ удаления. Все условия
   * объединяются через AND. Сообщение остаётся в очереди для настоящего потребителя.
   *
   * <p>Таблица: | имя property | значение |
   *
   * <p>Пример: И просмотрено сообщение из очереди "AUDIT.QUEUE" по property таблице | correlationId
   * | ${correlationId} | | action | PAYMENT_CREATED | Результат сохранен в переменную "auditMsg"
   */
  @И("^просмотрено сообщение из очереди \"(.*)\" по property таблице$")
  @SneakyThrows
  public void browseFromQueueByPropertyTable(
      String queueName, DataTable dataTable, String varName) {
    String selector = buildTableSelector(dataTable);
    browseFromQueue(queueName, selector, varName);
  }

  /** Просматривает сообщение из очереди по произвольному JMS селектору БЕЗ удаления. */
  @И(
      "^просмотрено сообщение из очереди \"(.*)\" по селектору \"(.*)\". Результат сохранен в переменную \"(.*)\"$")
  @SneakyThrows
  public void browseFromQueueBySelector(String queueName, String selector, String varName) {
    String resolvedSelector =
        BaseMethods.getInstance().getPropertyOrStringVariableOrValue(selector);
    resolvedSelector = resolveVars(resolvedSelector);
    browseFromQueue(queueName, resolvedSelector, varName);
  }

  // =========================================================================
  // ПРИВАТНЫЕ МЕТОДЫ
  // =========================================================================

  /** Строит JMS селектор из одного property: key = 'value'. */
  private String buildSingleSelector(String propertyName, String propertyValue) {
    String resolved = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(propertyValue);
    resolved = resolveVars(resolved);
    return propertyName + " = '" + resolved + "'";
  }

  /**
   * Строит JMS селектор из таблицы property — объединяет через AND. Таблица: | имя property |
   * значение | Результат: key1 = 'val1' AND key2 = 'val2'
   */
  private String buildTableSelector(DataTable dataTable) {
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
    return selector.toString();
  }

  /**
   * Общий метод browse — читает без удаления. selector == null → первое сообщение без фильтра.
   * selector != null → фильтрует на брокере.
   */
  @SneakyThrows
  private void browseFromQueue(String queueName, String selector, String varName) {
    var session = getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
    var queue = session.createQueue(queueName);
    var browser =
        selector != null ? session.createBrowser(queue, selector) : session.createBrowser(queue);
    var messages = browser.getEnumeration();

    if (!messages.hasMoreElements()) {
      throw new IllegalStateException(
          "Очередь '"
              + queueName
              + "' не содержит сообщений"
              + (selector != null ? " по селектору: " + selector : ""));
    }

    var message = (TextMessage) messages.nextElement();
    String text = message.getText();
    akitaScenario.log(
        "Browse из очереди ["
            + queueName
            + "]"
            + (selector != null ? " селектор [" + selector + "]" : "")
            + ": "
            + text);
    akitaScenario.setVar(varName, text);
    browser.close();
    session.close();
  }

  /** Предупреждает если сообщение отправляется без PROPERTY. */
  private void warnIfNoProperty(List<ArtemisRequestParam> params, String queue) {
    boolean hasProperty =
        params.stream().anyMatch(p -> p.getType() == ArtemisRequestParamType.PROPERTY);
    if (!hasProperty) {
      akitaScenario.log(
          "⚠️ Сообщение в очередь '"
              + queue
              + "' отправлено без PROPERTY. "
              + "Для надёжного чтения добавьте в таблицу: "
              + "| PROPERTY | correlationId | ${correlationId} |");
    }
  }
}
