package ru.gazprombank.automation.akitagpb.modules.kafkamq.steps.artemis;

import static ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables.resolveVars;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.И;
import io.cucumber.messages.internal.com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.PropertyLoader;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

/**
 * Шаги для проверки содержимого сообщений Artemis. Используются после чтения сообщения из очереди
 * или топика.
 *
 * <p>Поддерживает: - Проверку текста (любой формат) - Проверку JMS properties (заголовков) -
 * Проверку JSON тела по JSONPath - Проверку XML тела по XPath - Сохранение значений полей в
 * переменные сценария
 *
 * <p>Все JSON и XML методы перед работой валидируют формат сообщения и выдают информативную ошибку
 * если формат не соответствует ожидаемому.
 */
public class ArtemisMessageSteps extends ArtemisBaseSteps {

  // ═════════════════════════════════════════════════════════════════════════
  // ОБЩИЕ ПРОВЕРКИ
  // ═════════════════════════════════════════════════════════════════════════

  /**
   * Проверяет что тело сообщения содержит указанный текст. Работает для любого формата: JSON, XML,
   * plain text.
   */
  @И("^сообщение \"(.*)\" содержит текст \"(.*)\"$")
  public void checkMessageContainsText(String varName, String text) {
    String body = getMessageBody(varName);
    String value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(text);
    value = resolveVars(value);
    var soft = new SoftAssertions();
    soft.assertThat(body)
        .withFailMessage("Сообщение '%s' не содержит текст: '%s'", varName, value)
        .contains(value);
    soft.assertAll();
  }

  /**
   * Проверяет JMS properties (заголовки) сообщения. Properties сохраняются автоматически при каждом
   * чтении сообщения.
   *
   * <p>Таблица: | имя property | ожидаемое значение |
   *
   * <p>Пример: | correlationId | ${correlationId} | | eventType | PAYMENT_CREATED |
   */
  @И("^заголовки сообщения \"(.*)\" соответствуют таблице$")
  @SneakyThrows
  public void checkMessageHeaders(String varName, DataTable dataTable) {
    @SuppressWarnings("unchecked")
    var headers = (HashMap<String, String>) akitaScenario.getVar("__headers__" + varName);
    var soft = new SoftAssertions();

    for (List<String> row : dataTable.cells()) {
      String headerName = row.get(0);
      String expectedValue =
          BaseMethods.getInstance().getPropertyOrStringVariableOrValue(row.get(1));
      expectedValue = resolveVars(expectedValue);
      String actualValue = headers != null ? headers.get(headerName) : null;
      soft.assertThat(actualValue)
          .withFailMessage(
              "Property '%s': ожидалось '%s', получено '%s'",
              headerName, expectedValue, actualValue)
          .isEqualTo(expectedValue);
    }
    soft.assertAll();
  }

  // ═════════════════════════════════════════════════════════════════════════
  // ПРОВЕРКИ JSON
  // ═════════════════════════════════════════════════════════════════════════

  /**
   * Проверяет поля JSON сообщения по JSONPath. Перед проверкой валидирует что сообщение является
   * валидным JSON.
   *
   * <p>Таблица: | $.path.to.field | ожидаемое значение |
   *
   * <p>Пример: | $.status | SUCCESS | | $.payment.id | ${paymentId} | | $.payment.amount | 1000 |
   */
  @И("^json сообщение \"(.*)\" соответствует таблице$")
  public void checkJsonMessageByTable(String varName, DataTable dataTable) {
    String message = requireJson(varName);
    System.out.println(message);
    var soft = new SoftAssertions();

    for (List<String> row : dataTable.cells()) {
      String path = row.get(0);
      String expectedValue =
          BaseMethods.getInstance().getPropertyOrStringVariableOrValue(row.get(1));
      expectedValue = resolveVars(expectedValue);
      try {
        String actualValue = JsonPath.read(message, path).toString();
        soft.assertThat(actualValue)
            .withFailMessage(
                "JSON поле '%s': ожидалось '%s', получено '%s'", path, expectedValue, actualValue)
            .isEqualTo(expectedValue);
      } catch (JsonPathException e) {
        soft.fail("JSON поле '%s' не найдено в сообщении '%s'", path, varName);
      }
    }
    soft.assertAll();
  }

  /**
   * Сравнивает JSON сообщение с эталонным файлом из resources. Перед сравнением валидирует что
   * сообщение является валидным JSON. Динамичные поля (timestamp, id и т.д.) можно исключить из
   * сравнения.
   *
   * <p>Таблица игнорируемых полей: | $.timestamp | | $.request_id | | $.created_at |
   */
  @И("^json сообщение \"(.*)\" соответствует файлу \"(.*)\" игнорируя поля$")
  @SneakyThrows
  public void checkJsonMessageByFile(String varName, String fileName, DataTable ignoreTable) {
    String message = requireJson(varName);
    var expected = PropertyLoader.loadValueFromFileOrPropertyOrVariableOrDefault(fileName);

    var ignoredPaths =
        ignoreTable.asList().stream()
            .map(path -> new Customization(path, (o1, o2) -> true))
            .toArray(Customization[]::new);

    JSONAssert.assertEquals(
        expected, message, new CustomComparator(JSONCompareMode.LENIENT, ignoredPaths));
  }

  /**
   * Сохраняет значение поля JSON сообщения в переменную сценария. Перед извлечением валидирует что
   * сообщение является валидным JSON.
   *
   * <p>Пример: И из json сообщения "response" сохранено значение "$.payment.id" в переменную
   * "paymentId" И отправлен GET запрос на "/api/payment/${paymentId}"
   */
  @И("^из json сообщения \"(.*)\" сохранено значение \"(.*)\" в переменную \"(.*)\"$")
  public void saveValueFromJson(String varName, String jsonPath, String targetVar) {
    String message = requireJson(varName);
    try {
      String value = JsonPath.read(message, jsonPath).toString();
      akitaScenario.setVar(targetVar, value);
      akitaScenario.log("Сохранено из JSON [" + jsonPath + "]: " + value);
    } catch (JsonPathException e) {
      throw new IllegalStateException(
          "JSON поле '" + jsonPath + "' не найдено в сообщении '" + varName + "'");
    }
  }

  // ═════════════════════════════════════════════════════════════════════════
  // ПРОВЕРКИ XML
  // ═════════════════════════════════════════════════════════════════════════

  /**
   * Проверяет поля XML сообщения по XPath. Перед проверкой валидирует что сообщение является
   * валидным XML.
   *
   * <p>Таблица: | //xpath/expression | ожидаемое значение |
   *
   * <p>Пример: | //payment/status | SUCCESS | | //payment/id | ${paymentId} | | //payment/amount |
   * 1000 |
   */
  @И("^xml сообщение \"(.*)\" соответствует таблице$")
  @SneakyThrows
  public void checkXmlMessageByTable(String varName, DataTable dataTable) {
    String message = requireXml(varName);
    var document = parseXml(message);
    var xpath = XPathFactory.newInstance().newXPath();
    var soft = new SoftAssertions();

    for (List<String> row : dataTable.cells()) {
      String path = row.get(0);
      String expectedValue =
          BaseMethods.getInstance().getPropertyOrStringVariableOrValue(row.get(1));
      expectedValue = resolveVars(expectedValue);
      String actualValue = (String) xpath.evaluate(path, document, XPathConstants.STRING);
      soft.assertThat(actualValue)
          .withFailMessage(
              "XML поле '%s': ожидалось '%s', получено '%s'", path, expectedValue, actualValue)
          .isEqualTo(expectedValue);
    }
    soft.assertAll();
  }

  /**
   * Сравнивает XML сообщение с эталонным файлом из resources. Перед сравнением валидирует что
   * сообщение является валидным XML. Динамичные узлы можно исключить из сравнения.
   *
   * <p>Таблица игнорируемых узлов: | //timestamp | | //requestId |
   */
  @И("^xml сообщение \"(.*)\" соответствует файлу \"(.*)\" игнорируя поля$")
  @SneakyThrows
  public void checkXmlMessageByFile(String varName, String fileName, DataTable ignoreTable) {
    String message = requireXml(varName);
    var expected = PropertyLoader.loadValueFromFileOrPropertyOrVariableOrDefault(fileName);
    var docActual = parseXml(message);
    var docExpected = parseXml(expected);
    var xpath = XPathFactory.newInstance().newXPath();
    var ignoredPaths = ignoreTable.asList();
    var soft = new SoftAssertions();

    for (String path : ignoredPaths) {
      clearXmlNode(docActual, path, xpath);
      clearXmlNode(docExpected, path, xpath);
    }

    soft.assertThat(xmlToString(docActual))
        .withFailMessage("XML сообщение не соответствует файлу: " + fileName)
        .isEqualToIgnoringWhitespace(xmlToString(docExpected));
    soft.assertAll();
  }

  /**
   * Сохраняет значение узла XML сообщения в переменную сценария. Перед извлечением валидирует что
   * сообщение является валидным XML.
   *
   * <p>Пример: И из xml сообщения "response" сохранено значение "//payment/id" в переменную
   * "paymentId" И отправлен GET запрос на "/api/payment/${paymentId}"
   */
  @И("^из xml сообщения \"(.*)\" сохранено значение \"(.*)\" в переменную \"(.*)\"$")
  @SneakyThrows
  public void saveValueFromXml(String varName, String xpathExpr, String targetVar) {
    String message = requireXml(varName);
    var document = parseXml(message);
    var xpath = XPathFactory.newInstance().newXPath();
    var value = (String) xpath.evaluate(xpathExpr, document, XPathConstants.STRING);
    akitaScenario.setVar(targetVar, value);
    akitaScenario.log("Сохранено из XML [" + xpathExpr + "]: " + value);
  }

  // ═════════════════════════════════════════════════════════════════════════
  // ПРИВАТНЫЕ МЕТОДЫ — ВАЛИДАЦИЯ
  // ═════════════════════════════════════════════════════════════════════════

  /**
   * Возвращает тело сообщения из переменной сценария. Падает с понятной ошибкой если переменная
   * пустая или не найдена.
   */
  private String getMessageBody(String varName) {
    String body;
    try {
      body = (String) akitaScenario.getVar(varName);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Сообщение '"
              + varName
              + "' не найдено. "
              + "Убедитесь что шаг чтения выполнен до проверки.");
    }
    if (body == null || body.isBlank()) {
      throw new IllegalStateException("Сообщение '" + varName + "' пустое.");
    }
    return body;
  }

  /**
   * Проверяет что сообщение является валидным JSON. Выдаёт информативную ошибку с первыми 300
   * символами тела если нет.
   *
   * <p>Частые причины ошибки: - Сервис вернул XML вместо JSON - Сервис вернул plain text с ошибкой
   * - Пришло пустое сообщение
   */
  private String requireJson(String varName) {
    String body = getMessageBody(varName);
    try {
      new ObjectMapper().readTree(body);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Сообщение '"
              + varName
              + "' не является валидным JSON.\n"
              + "Первые 300 символов тела:\n"
              + body.substring(0, Math.min(body.length(), 300))
              + (body.length() > 300 ? "\n... (truncated)" : ""));
    }
    return body;
  }

  /**
   * Проверяет что сообщение является валидным XML. Выдаёт информативную ошибку с первыми 300
   * символами тела если нет.
   *
   * <p>Частые причины ошибки: - Сервис вернул JSON вместо XML - Сервис вернул plain text с ошибкой
   * - Некорректный XML (незакрытые теги и т.д.)
   */
  private String requireXml(String varName) {
    String body = getMessageBody(varName);
    try {
      parseXml(body);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Сообщение '"
              + varName
              + "' не является валидным XML.\n"
              + "Причина: "
              + e.getMessage()
              + "\n"
              + "Первые 300 символов тела:\n"
              + body.substring(0, Math.min(body.length(), 300))
              + (body.length() > 300 ? "\n... (truncated)" : ""));
    }
    return body;
  }
}
