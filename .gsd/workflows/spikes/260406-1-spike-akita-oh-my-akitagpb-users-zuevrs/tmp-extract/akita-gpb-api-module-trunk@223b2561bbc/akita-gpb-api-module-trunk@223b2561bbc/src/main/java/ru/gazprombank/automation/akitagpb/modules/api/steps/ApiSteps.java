package ru.gazprombank.automation.akitagpb.modules.api.steps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables.resolveVars;
import static ru.gazprombank.automation.akitagpb.modules.core.helpers.PropertyLoader.loadValueFromFileOrPropertyOrVariableOrDefault;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.И;
import io.cucumber.java.ru.Тогда;
import io.restassured.internal.support.Prettifier;
import io.restassured.path.json.exception.JsonPathException;
import io.restassured.response.Response;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.assertj.core.api.BooleanAssert;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.StringAssert;
import org.everit.json.schema.loader.SchemaLoader;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ru.gazprombank.automation.akitagpb.modules.api.rest.RequestParam;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

/** Шаги для тестирования API, доступные по умолчанию в каждом новом проекте */
@Slf4j
public class ApiSteps extends ApiBaseMethods {

  /**
   * Посылается http запрос по заданному урлу без параметров и BODY. Результат сохраняется в
   * заданную переменную URL можно задать как напрямую в шаге, так и указав в application.properties
   */
  @И(
      "^выполнен (GET|POST|PUT|DELETE|PATCH) запрос на URL \"(.*)\". Полученный ответ сохранен в переменную \"(.*)\"$")
  public void sendHttpRequestWithoutParams(String method, String address, String variableName) {
    Response response = sendRequest(method, address, new ArrayList<>());
    getResponseAndSaveToVariable(variableName, response);
  }

  /**
   * Посылается http GET запрос по заданному урлу. Результат сохраняется в заданную переменную в
   * формате String, URL можно задать как напрямую в шаге, так и указав в application.properties
   */
  @И(
      "^выполнен GET\\(SSE\\) запрос на URL \"(.*)\". Полученный ответ сохранен в переменную \"(.*)\"$")
  public void sendHttpGetRequestWithoutParams(String address, String variableName) {
    String responseBody = sendGetSseRequest(address);
    var jsonBody = new StringBuilder(responseBody);
    jsonBody.insert(0, "{").insert(responseBody.length(), "}");
    log.info("Тело ответа: " + jsonBody.toString());
    akitaScenario.setVar(variableName, jsonBody.toString());
  }

  /**
   * Посылается http запрос по заданному урлу с заданными параметрами. И в URL, и в значениях в
   * таблице можно использовать переменные и из application.properties, и из хранилища переменных из
   * AlfaScenario. Для этого достаточно заключить переменные в фигурные скобки, например:
   * http://{hostname}?user={username}. Content-Type при необходимости должен быть указан в качестве
   * header. Результат сохраняется в заданную переменную
   */
  @И(
      "^выполнен (GET|POST|PUT|DELETE|PATCH) запрос на URL \"(.*)\" с headers и parameters из таблицы. Полученный ответ сохранен в переменную \"(.*)\"$")
  public void sendHttpRequestSaveResponse(
      String method, String address, String variableName, List<RequestParam> paramsTable) {
    Response response = sendRequest(method, address, paramsTable);
    getResponseAndSaveToVariable(variableName, response);
  }

  /**
   * Посылается http запрос по заданному урлу без параметров и BODY. Проверяется, что код ответа
   * соответствует ожиданиям. URL можно задать как напрямую в шаге, так и указав в
   * application.properties
   */
  @И("^выполнен (GET|POST|PUT|DELETE|PATCH) запрос на URL \"(.*)\". Ожидается код ответа: (\\d+)$")
  public void checkResponseCodeWithoutParams(
      String method, String address, int expectedStatusCode) {
    Response response = sendRequest(method, address, new ArrayList<>());
    assertTrue(checkStatusCode(response, expectedStatusCode));
  }

  /**
   * Посылается http запрос по заданному урлу с заданными параметрами. Проверяется, что код ответа
   * соответствует ожиданиям. URL можно задать как напрямую в шаге, так и указав в
   * application.properties Content-Type при необходимости должен быть указан в качестве header.
   */
  @И(
      "^выполнен (GET|POST|PUT|DELETE|PATCH) запрос на URL \"(.*)\" с headers и parameters из таблицы. Ожидается код ответа: (\\d+)$")
  public void checkResponseCode(
      String method, String address, int expectedStatusCode, List<RequestParam> paramsTable) {
    Response response = sendRequest(method, address, paramsTable);
    assertTrue(checkStatusCode(response, expectedStatusCode));
  }

  @И("^в ответе \"(.*)\" код ответа равен (\\d+)$")
  public void checkResponseCode(String responseName, int expectedStatusCode) {
    Response response = (Response) akitaScenario.getVar(responseName);
    assertTrue(checkStatusCode(response, expectedStatusCode));
  }

  @И("^из ответа \"(.*)\" cookie с именем \"(.*)\" сохранен в переменную \"(.*)\"$")
  public void saveCookieResponseToVariable(
      String responseName, String cookieName, String variableName) {
    Response response = (Response) akitaScenario.getVar(responseName);
    var cookieValue = response.getCookie(cookieName);
    assertThat("Cookie " + cookieName + " не найден", cookieValue, Matchers.notNullValue());
    akitaScenario.setVar(variableName, cookieValue);
    akitaScenario.log(String.format("Значение %s: %s", cookieName, cookieValue));
  }

  @И("^из ответа \"(.*)\" header с именем \"(.*)\" сохранен в переменную \"(.*)\"$")
  public void saveHeaderResponseToVariable(
      String responseName, String headerName, String variableName) {
    Response response = (Response) akitaScenario.getVar(responseName);
    var headerValue = response.getHeader(headerName);
    assertThat("Header " + headerName + " не найден", headerValue, Matchers.notNullValue());
    akitaScenario.setVar(variableName, headerValue);
    akitaScenario.log(String.format("Значение %s: %s", headerName, headerValue));
  }

  @И("^из ответа \"(.*)\" значение тела по пути \"(.*)\" сохранено в переменную \"(.*)\"$")
  public void saveValueFromJsonPathResponseToVariable(
      String responseName, String jsonPath, String variableName) {
    var strJson = "";
    var value = "";
    Response response = (Response) akitaScenario.getVar(responseName);
    strJson = response.body().asString();
    ReadContext ctx = JsonPath.parse(strJson, createJsonPathConfiguration());
    if (jsonPath.startsWith("$")) {
      value = ctx.read(jsonPath).toString().replaceAll("^\"|^\'|\"$|\'$", "");
    } else {
      value = io.restassured.path.json.JsonPath.with(strJson).get(jsonPath).toString();
    }
    assertThat("По пути " + jsonPath + " элемент не найден", value, Matchers.notNullValue());
    akitaScenario.setVar(variableName, value);
    akitaScenario.log(String.format("Значение %s: %s", jsonPath, value));
  }

  /**
   * Получает body из ответа и сохраняет в переменную
   *
   * @param responseName имя переменной, в которую был сохранен ответ
   * @param variableName имя переменной, в которую будет сохранен body
   */
  @И("^из ответа \"(.*)\" значение тела сохранено в переменную \"(.*)\"$")
  public void getBodyAndSaveToVariable(String responseName, String variableName) {
    Response response = (Response) akitaScenario.getVar(responseName);
    akitaScenario.setVar(variableName, response.getBody().asString());
    akitaScenario.log(
        "Тело ответа : \n" + new Prettifier().getPrettifiedBodyIfPossible(response, response));
  }

  /**
   * В json строке, сохраннённой в переменной, происходит поиск значений по jsonpath из первого
   * столбца таблицы. Шаг работает со всеми типами json элементов: объекты, массивы, строки, числа,
   * литералы true, false и null.
   */
  @Тогда(
      "^в json (?:строке|файле) \"(.*)\" элементы, найденные по jsonpath из таблицы, существуют$")
  public void checkJsonPathExists(String jsonVar, DataTable dataTable) {
    var softAssertions = new SoftAssertions();
    String strJson = loadValueFromFileOrPropertyOrVariableOrDefault(jsonVar);
    ReadContext ctx =
        JsonPath.parse(strJson, createJsonPathConfiguration().addOptions(Option.AS_PATH_LIST));

    for (List<String> row : dataTable.cells()) {

      String jsonPath = ScopedVariables.resolveVars(row.get(0));

      try {
        ctx.read(jsonPath);
      } catch (JsonPathException | PathNotFoundException e) {
        softAssertions
            .assertThat(false)
            .withFailMessage(
                "Не удалось найти элемент по пути '"
                    + jsonPath
                    + "';\n Exception: "
                    + e.getMessage())
            .isTrue();
      }
    }
    softAssertions.assertAll();
  }

  /**
   * В json строке, сохраннённой в переменной, происходит поиск значений по jsonpath из первого
   * столбца таблицы. Происходит проверка, что в json строке нет указанного значения.
   */
  @И("^в json (?:строке|файле) \"(.*)\" элементы, найденные по jsonpath из таблицы, не существуют$")
  public void checkJsonPathDoesNotExists(String jsonVar, DataTable dataTable) {
    var softAssertions = new SoftAssertions();
    var strJson = "";
    if (akitaScenario.getVar(jsonVar) instanceof Response) {
      Response response = (Response) akitaScenario.getVar(jsonVar);
      strJson = response.body().asString();
    } else {
      strJson = loadValueFromFileOrPropertyOrVariableOrDefault(jsonVar);
    }

    ReadContext ctx =
        JsonPath.parse(strJson, createJsonPathConfiguration().addOptions(Option.AS_PATH_LIST));
    for (List<String> row : dataTable.cells()) {
      String jsonPath = ScopedVariables.resolveVars(row.get(0));
      if (jsonPath.startsWith("$")) {
        softAssertions
            .assertThatCode(() -> ctx.read(jsonPath))
            .withFailMessage("Элемент найден по пути " + jsonPath + ";")
            .isInstanceOf(PathNotFoundException.class);
      } else {
        var finalStrJson = strJson;
        softAssertions
            .assertThatCode(
                () -> io.restassured.path.json.JsonPath.with(finalStrJson).get(jsonPath).toString())
            .withFailMessage("Элемент найден по пути " + jsonPath + ";")
            .isInstanceOf(NullPointerException.class);
      }
    }
    softAssertions.assertAll();
  }

  /**
   * Принимает на вход JSON строку или ответ (в ответе достается JSON строка из тела) и происходит
   * поиск значений по jsonpath из первого столбца таблицы (если jsonpath начинается с символа "$",
   * то поиск осуществляется при помощи библиотеки jayway, если знака "$" нет, то при помощи
   * restassured). Полученные значения сравниваются с ожидаемым значением во втором столбце таблицы.
   * Шаг работает со всеми типами json элементов: объекты, массивы, строки, числа, литералы true,
   * false и null.
   *
   * <p>При написании названия шага вот так: Тогда("^в (?:ответе|json строке) \"(.*)\" значения,
   * найденные по jsonpath, равны значениям из таблицы$") в фича файле неудобно отображается
   * подсказка, корректно отображается только если в выборе параметра отсутствует пробел:
   * (?:ответе|строке) по этому было принято решение разделить метод на два шага с разным названием.
   */
  @Тогда("^в json строке \"(.*)\" значения, найденные по jsonpath, равны значениям из таблицы$")
  public void checkValuesInStrJsonAsString(String jsonVar, DataTable dataTable) {
    checkValuesInJsonAsString(jsonVar, dataTable);
  }

  @Тогда("^в ответе \"(.*)\" значения, найденные по jsonpath, равны значениям из таблицы$")
  public void checkValuesInResponseAsString(String jsonVar, DataTable dataTable) {
    checkValuesInJsonAsString(jsonVar, dataTable);
  }

  public void checkValuesInJsonAsString(String jsonVar, DataTable dataTable) {
    var softAssertions = new SoftAssertions();
    var expectedValue = "";
    var actualValue = "";
    var strJson = "";
    var jsonPath = "";

    if (akitaScenario.getVar(jsonVar) instanceof Response) {
      Response response = (Response) akitaScenario.getVar(jsonVar);
      strJson = response.body().asString();
    } else {
      strJson = loadValueFromFileOrPropertyOrVariableOrDefault(jsonVar);
    }
    ReadContext ctx = JsonPath.parse(strJson, createJsonPathConfiguration());
    for (List<String> row : dataTable.cells()) {
      jsonPath = ScopedVariables.resolveVars(row.get(0));
      expectedValue = row.get(1);

      if (jsonPath.startsWith("$")) {
        try {
          actualValue = ctx.read(jsonPath).toString().replaceAll("^\"|^\'|\"$|\'$", "");
        } catch (PathNotFoundException e) {
          softAssertions
              .assertThat(false)
              .withFailMessage(
                  "Не удалось найти элемент по пути '"
                      + jsonPath
                      + "';\n Exception: "
                      + e.getMessage())
              .isTrue();
          continue;
        }
      } else {
        try {
          actualValue = io.restassured.path.json.JsonPath.with(strJson).get(jsonPath).toString();
        } catch (JsonPathException | NullPointerException e) {
          softAssertions
              .assertThat(false)
              .withFailMessage(
                  "Не удалось найти элемент по пути '"
                      + jsonPath
                      + "';\n Exception: "
                      + e.getMessage())
              .isTrue();
          continue;
        }
      }

      if (expectedValue != null) {
        expectedValue = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(expectedValue);
        expectedValue = resolveVars(expectedValue);
      } else {
        expectedValue = "";
      }

      softAssertions
          .assertThat(actualValue)
          .withFailMessage(
              "JsonPath: '"
                  + jsonPath
                  + "', ожидаемое значение: '"
                  + expectedValue
                  + "', фактическое значение: '"
                  + actualValue
                  + "'")
          .isEqualTo(expectedValue);
    }
    softAssertions.assertAll();
  }

  /**
   * Принимает на вход JSON строку или ответ (в ответе достается JSON строка из тела) и происходит
   * поиск значений по jsonpath из первого столбца таблицы (если jsonpath начинается с символа "$",
   * то поиск осуществляется при помощи библиотеки jayway, если знака "$" нет, то при помощи
   * restassured). Полученные значения сохраняются в переменных. Название переменной указывается во
   * втором столбце таблицы. Шаг работает со всеми типами json элементов: объекты, массивы, строки,
   * числа, литералы true, false и null.
   */
  @Тогда(
      "^значения из json строки \"(.*)\", найденные по jsonpath из таблицы, сохранены в переменные$")
  public void getValuesFromStrJsonAsString(String jsonVar, DataTable dataTable) {
    getValuesFromJsonAsString(jsonVar, dataTable);
  }

  @Тогда("^значения из ответа \"(.*)\", найденные по jsonpath из таблицы, сохранены в переменные$")
  public void getValuesFromResponseAsString(String jsonVar, DataTable dataTable) {
    getValuesFromJsonAsString(jsonVar, dataTable);
  }

  public void getValuesFromJsonAsString(String jsonVar, DataTable dataTable) {
    var softAssertions = new SoftAssertions();
    var strJson = "";
    var jsonPath = "";
    var varName = "";
    var jsonElement = "";
    if (akitaScenario.getVar(jsonVar) instanceof Response) {
      Response response = (Response) akitaScenario.getVar(jsonVar);
      strJson = response.body().asString();
    } else {
      strJson = loadValueFromFileOrPropertyOrVariableOrDefault(jsonVar);
    }
    ReadContext ctx = JsonPath.parse(strJson, createJsonPathConfiguration());
    for (List<String> row : dataTable.cells()) {
      jsonPath = ScopedVariables.resolveVars(row.get(0));
      varName = row.get(1);

      if (jsonPath.startsWith("$")) {
        try {
          jsonElement = ctx.read(jsonPath).toString().replaceAll("^\"|^\'|\"$|\'$", "");
        } catch (PathNotFoundException e) {
          softAssertions
              .assertThat(false)
              .withFailMessage(
                  "Не удалось найти элемент по пути '"
                      + jsonPath
                      + "';\n Exception: "
                      + e.getMessage())
              .isTrue();
          continue;
        }
      } else {
        try {
          jsonElement = io.restassured.path.json.JsonPath.with(strJson).get(jsonPath).toString();
        } catch (JsonPathException | NullPointerException e) {
          softAssertions
              .assertThat(false)
              .withFailMessage(
                  "Не удалось найти элемент по пути '"
                      + jsonPath
                      + "';\n Exception: "
                      + e.getMessage())
              .isTrue();
          continue;
        }
      }

      akitaScenario.setVar(varName, jsonElement);
      akitaScenario.log(
          "JsonPath: "
              + jsonPath
              + ", значение: "
              + jsonElement
              + ", записано в переменную: "
              + varName);
    }
    softAssertions.assertAll();
  }

  /** Полученный response соответствует схеме. */
  @И("^значения json \"(.*)\", соответствует схеме \"(.*)\"$")
  public void checkJsonSchema(String responseName, String schemaName) {
    var softAssertions = new SoftAssertions();
    var response = (Response) akitaScenario.getVar(responseName);
    var jsonSubject = new JSONObject(response.body().asString());
    var jsonSchema =
        new JSONObject(
            loadValueFromFileOrPropertyOrVariableOrDefault(
                ScopedVariables.resolveVars(schemaName)));

    var schema = SchemaLoader.load(jsonSchema);
    softAssertions.assertThatCode(() -> schema.validate(jsonSubject)).doesNotThrowAnyException();
    softAssertions.assertAll();
  }

  @SneakyThrows
  @И("^в xml (?:строке|файле) \"(.*)\" значения, найденные по xmlpath, равны значениям из таблицы$")
  public void checkValuesInXmlAsString(String jsonVar, DataTable dataTable) {
    var strXml = loadValueFromFileOrPropertyOrVariableOrDefault(jsonVar);
    var dbf = DocumentBuilderFactory.newInstance();
    dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

    var db = dbf.newDocumentBuilder();
    var doc = db.parse(new InputSource(new StringReader(strXml)));

    var xPathFactory = XPathFactory.newInstance();
    var xPath = xPathFactory.newXPath();

    var softAssertions = new SoftAssertions();
    var var6 = dataTable.cells().iterator();

    while (var6.hasNext()) {
      var row = (List) var6.next();
      var xmlPath = ScopedVariables.resolveVars((String) row.get(0));
      var expectedValue = (String) row.get(1);

      var expression = xPath.compile(xmlPath);

      String actualValue;
      try {
        actualValue = expression.evaluate(doc, XPathConstants.STRING).toString();
        if (actualValue.isEmpty()) {
          softAssertions
              .assertThat(false)
              .withFailMessage("Не удалось найти элемент по пути '" + xmlPath)
              .isTrue();
        }
      } catch (XPathExpressionException var12) {
        ((BooleanAssert)
                softAssertions
                    .assertThat(false)
                    .withFailMessage(
                        "Не удалось найти элемент по пути '"
                            + xmlPath
                            + "';\n Exception: "
                            + var12.getMessage(),
                        new Object[0]))
            .isTrue();
        continue;
      }

      if (expectedValue != null) {
        expectedValue = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(expectedValue);
        expectedValue = ScopedVariables.resolveVars(expectedValue);
      } else {
        expectedValue = "";
      }

      ((StringAssert)
              softAssertions
                  .assertThat(actualValue)
                  .withFailMessage(
                      "XmlPath: '"
                          + xmlPath
                          + "', ожидаемое значение: '"
                          + expectedValue
                          + "', фактическое значение: '"
                          + actualValue
                          + "'",
                      new Object[0]))
          .isEqualTo(expectedValue);
    }
    softAssertions.assertAll();
  }

  @И("^в xml (?:строке|файле) \"(.*)\" элементы, найденные по xmlpath из таблицы, существуют$")
  public void checkXmlPathExists(String xmlVar, DataTable dataTable)
      throws ParserConfigurationException, IOException, SAXException {
    var strXml = loadValueFromFileOrPropertyOrVariableOrDefault(xmlVar);
    var dbf = DocumentBuilderFactory.newInstance();
    dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

    var db = dbf.newDocumentBuilder();
    var doc = db.parse(new InputSource(new StringReader(strXml)));

    var xPathFactory = XPathFactory.newInstance();
    var xPath = xPathFactory.newXPath();

    var softAssertions = new SoftAssertions();
    for (List<String> row : dataTable.cells()) {

      var xmlPath = ScopedVariables.resolveVars(row.get(0));

      try {
        var expression = xPath.compile(xmlPath);
        if (expression.evaluate(doc).isEmpty()) {
          softAssertions
              .assertThat(false)
              .withFailMessage("Не удалось найти элемент по пути '" + xmlPath)
              .isTrue();
        }
      } catch (XPathExpressionException e) {
        softAssertions
            .assertThat(false)
            .withFailMessage(
                "Не удалось найти элемент по пути '"
                    + xmlPath
                    + "';\n Exception: "
                    + e.getMessage())
            .isTrue();
      }
    }
    softAssertions.assertAll();
  }

  @И("^в xml (?:строке|файле) \"(.*)\" элементы, найденные по xmlpath из таблицы, не существуют$")
  public void checkXmlPathNotExists(String xmlVar, DataTable dataTable)
      throws ParserConfigurationException, IOException, SAXException {
    var strXml = loadValueFromFileOrPropertyOrVariableOrDefault(xmlVar);
    var dbf = DocumentBuilderFactory.newInstance();
    dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

    var db = dbf.newDocumentBuilder();
    var doc = db.parse(new InputSource(new StringReader(strXml)));

    var xPathFactory = XPathFactory.newInstance();
    var xPath = xPathFactory.newXPath();

    var softAssertions = new SoftAssertions();
    for (List<String> row : dataTable.cells()) {

      var xmlPath = ScopedVariables.resolveVars(row.get(0));

      try {
        var expression = xPath.compile(xmlPath);
        if (((NodeList) expression.evaluate(doc, XPathConstants.NODESET)).getLength() > 0) {
          softAssertions
              .assertThat(false)
              .withFailMessage("Найден элемент по пути '" + xmlPath + "'")
              .isTrue();
        }
      } catch (XPathExpressionException e) {
        softAssertions
            .assertThat(false)
            .withFailMessage("Некорректный xmlPath '" + xmlPath + "'\n" + e.getMessage())
            .isTrue();
      }
    }
    softAssertions.assertAll();
  }

  @SneakyThrows
  @И(
      "^значения из xml (?:строки|файла) \"(.*)\", найденные по xmlpath из таблицы, сохранены в переменные$")
  public void getValuesFromXmlAsString(String xmlVar, DataTable dataTable) {
    var strXml = loadValueFromFileOrPropertyOrVariableOrDefault(xmlVar);

    var dbf = DocumentBuilderFactory.newInstance();
    dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

    var db = dbf.newDocumentBuilder();
    var doc = db.parse(new InputSource(new StringReader(strXml)));

    var xPathFactory = XPathFactory.newInstance();
    var xPath = xPathFactory.newXPath();
    var softAssertions = new SoftAssertions();
    for (var row : dataTable.cells()) {
      var xmlPath = ScopedVariables.resolveVars(row.get(0));
      var varName = row.get(1);
      String xmlElement;
      var expression = xPath.compile(xmlPath);
      try {
        xmlElement = expression.evaluate(doc, XPathConstants.STRING).toString();
        if (xmlElement.isEmpty()) {
          softAssertions
              .assertThat(false)
              .withFailMessage("Не удалось найти элемент по пути " + xmlPath)
              .isTrue();
        }
      } catch (XPathExpressionException e) {
        softAssertions
            .assertThat(false)
            .withFailMessage(
                "Не удалось найти элемент по пути " + xmlPath + ";\n Exception: " + e.getMessage())
            .isTrue();
        continue;
      }
      akitaScenario.setVar(varName, xmlElement);
      akitaScenario.log(
          "XmlPath: "
              + xmlPath
              + ", значение: "
              + xmlElement
              + ", записано в переменную: "
              + varName);
      softAssertions.assertAll();
    }
  }

  /** Полученный xml response соответствует схеме. */
  @SneakyThrows
  @И("^значения xml \"(.*)\", соответствует схеме \"(.*)\"$")
  public void checkXsdSchema(String responseName, String schemaName) {
    var softAssertions = new SoftAssertions();
    var response =
        akitaScenario
            .getVar(responseName)
            .toString()
            .replaceAll("</soapenv:Body></soapenv:Envelope>", "")
            .replaceAll(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">",
                "")
            .replaceAll("<soapenv:Body>", "");
    var xmlFile = File.createTempFile("xml_answer", ".xml");

    try (var writer = new FileWriter(xmlFile)) {
      writer.write(response);
      xmlFile.deleteOnExit();
    }

    var source = new StreamSource(xmlFile);
    var schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    var schema =
        schemaFactory.newSchema(
            new File(FilenameUtils.normalize(System.getProperty("user.dir") + schemaName)));
    var validator = schema.newValidator();

    softAssertions.assertThatCode(() -> validator.validate(source)).doesNotThrowAnyException();
    softAssertions.assertAll();
  }
}
