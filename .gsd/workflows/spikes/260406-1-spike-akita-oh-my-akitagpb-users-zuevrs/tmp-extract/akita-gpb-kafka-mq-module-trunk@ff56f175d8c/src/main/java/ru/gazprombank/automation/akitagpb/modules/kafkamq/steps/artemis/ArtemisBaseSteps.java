package ru.gazprombank.automation.akitagpb.modules.kafkamq.steps.artemis;

import static ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables.resolveVars;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.jms.Connection;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import lombok.SneakyThrows;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.PropertyLoader;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.config.ArtemisProperties;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.mq.artemis.ArtemisRequestParam;

/**
 * Базовый класс для всех Artemis шагов. Содержит общие методы используемые в Queue и Topic шагах.
 */
public abstract class ArtemisBaseSteps extends BaseMethods {

  // ─── Константа для хранения соединения ────────────────────────────────────
  static final String CONNECTION_VAR = "__artemisConnection__";

  // ═════════════════════════════════════════════════════════════════════════
  // СОЕДИНЕНИЕ
  // ═════════════════════════════════════════════════════════════════════════

  /**
   * Возвращает текущее соединение. Используется и для Queue и для Topic шагов. Падает с понятной
   * ошибкой если соединение не создано.
   */
  protected Connection getConnection() {
    try {
      var connection = (Connection) akitaScenario.getVar(CONNECTION_VAR);
      if (connection == null) {
        throw new IllegalStateException(
            "Artemis соединение не создано. "
                + "Добавьте шаг 'И создано подключение к artemis' перед использованием");
      }
      return connection;
    } catch (IllegalArgumentException e) {
      // AkitaScenario.getVar бросает IllegalArgumentException если переменная не найдена
      throw new IllegalStateException(
          "Artemis соединение не создано. "
              + "Добавьте шаг 'И создано подключение к artemis' перед использованием");
    }
  }

  // ═════════════════════════════════════════════════════════════════════════
  // ПОСТРОЕНИЕ СООБЩЕНИЯ
  // ═════════════════════════════════════════════════════════════════════════

  /**
   * Строит JMS TextMessage из списка параметров.
   *
   * <p>Формат таблицы: | type | name | value | | PROPERTY | correlationId | ${correlationId} | |
   * BODY | body | request.json | | VAR | myVar | someValue |
   */
  @SneakyThrows
  protected TextMessage buildMessage(Session session, List<ArtemisRequestParam> params) {
    var message = session.createTextMessage();
    String body = null;

    for (ArtemisRequestParam param : params) {
      String name = param.getName();
      String value = param.getValue();

      switch (param.getType()) {
        case PROPERTY:
          if (value == null || value.isEmpty()) {
            message.setStringProperty(name, "");
          } else {
            value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
            value = resolveVars(value);
            message.setStringProperty(name, value);
          }
          break;
        case VAR:
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          this.akitaScenario.setVar(name, value);
          break;
        case BODY:
          value = PropertyLoader.loadValueFromFileOrPropertyOrVariableOrDefault(resolveVars(value));
          body = resolveVars(value);
          break;
      }
    }

    message.setText(body);
    return message;
  }

  // ═════════════════════════════════════════════════════════════════════════
  // ЧТЕНИЕ ИЗ ОЧЕРЕДИ
  // ═════════════════════════════════════════════════════════════════════════

  /**
   * Общий метод чтения из очереди. selector == null → читает первое сообщение без фильтра. Таймаут
   * берётся из artemis.properties (artemis.receive.timeout). Бросает исключение если сообщение не
   * получено за отведённое время.
   */
  @SneakyThrows
  protected void receiveFromQueue(String queueName, String selector, String varName) {
    long timeout = new ArtemisProperties().getReceiveTimeout();
    var connection = getConnection();
    var session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
    var queue = session.createQueue(queueName);
    var receiver =
        selector != null ? session.createConsumer(queue, selector) : session.createConsumer(queue);

    connection.start();
    var message =
        Optional.ofNullable(receiver.receive(timeout))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Сообщение не получено за "
                            + timeout
                            + "мс из очереди: "
                            + queueName
                            + (selector != null ? ", селектор: " + selector : "")));

    var textMessage = (TextMessage) message;
    String text = textMessage.getText();
    akitaScenario.log("Получено из очереди [" + queueName + "]: " + text);

    saveHeaders(textMessage, varName);
    message.acknowledge();
    receiver.close();
    session.close();
    akitaScenario.setVar(varName, text);
  }

  // ═════════════════════════════════════════════════════════════════════════
  // ЗАГОЛОВКИ
  // ═════════════════════════════════════════════════════════════════════════

  /**
   * Сохраняет JMS properties сообщения для последующей проверки шагом 'заголовки сообщения
   * соответствуют таблице'.
   */
  @SneakyThrows
  protected void saveHeaders(TextMessage message, String varName) {
    var headers = new HashMap<String, String>();
    var names = message.getPropertyNames();
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      headers.put(name, message.getStringProperty(name));
    }
    if (message.getJMSCorrelationID() != null) {
      headers.put("JMSCorrelationID", message.getJMSCorrelationID());
    }
    headers.put("JMSMessageID", message.getJMSMessageID());
    akitaScenario.setVar("__headers__" + varName, headers);
  }

  // ═════════════════════════════════════════════════════════════════════════
  // XML УТИЛИТЫ
  // ═════════════════════════════════════════════════════════════════════════

  @SneakyThrows
  protected Document parseXml(String xml) {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
  }

  @SneakyThrows
  protected void clearXmlNode(Document doc, String xpathExpr, javax.xml.xpath.XPath xpath) {
    var node = (org.w3c.dom.Node) xpath.evaluate(xpathExpr, doc, XPathConstants.NODE);
    if (node != null) {
      node.setTextContent("");
    }
  }

  @SneakyThrows
  protected String xmlToString(Document doc) {
    var transformer = TransformerFactory.newInstance().newTransformer();
    var result = new StreamResult(new StringWriter());
    transformer.transform(new DOMSource(doc), result);
    return result.getWriter().toString();
  }
}
