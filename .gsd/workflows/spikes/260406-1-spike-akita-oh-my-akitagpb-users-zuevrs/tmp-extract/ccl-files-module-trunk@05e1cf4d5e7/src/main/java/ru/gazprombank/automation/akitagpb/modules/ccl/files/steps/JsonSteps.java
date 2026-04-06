package ru.gazprombank.automation.akitagpb.modules.ccl.files.steps;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.getFileContentOrValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.И;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.apache.commons.text.StringEscapeUtils;
import org.assertj.core.api.SoftAssertions;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.FileHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.assertions.MainAssert;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels.JsonAssertableConditionParam;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

/** Шаги для работы с json'ом */
public class JsonSteps extends BaseMethods {

  public static final Configuration jsonPathConfig =
      new Configuration.ConfigurationBuilder()
          .jsonProvider(new JacksonJsonProvider(StringHelper.OBJECT_MAPPER))
          .mappingProvider(new JacksonMappingProvider(StringHelper.OBJECT_MAPPER))
          .options(Option.ALWAYS_RETURN_LIST)
          .build();

  /**
   * В json строке, сохраннённой в переменной, происходит поиск значений по jsonpath из первого
   * столбца таблицы. Полученные значения сравниваются с ожидаемым значением во втором столбце
   * таблицы. Шаг работает со всеми типами json элементов: объекты, массивы, строки, числа, литералы
   * true, false и null.
   */
  @И("^в json (строке|файле) \"(.*)\" значения, найденные по JsonPath, равны значениям из таблицы$")
  public void checkValuesInJsonAsString(String strOrFile, String jsonVar, DataTable dataTable) {
    String strJson =
        "файле".equals(strOrFile)
            ? getFileContentOrValue(processValue(jsonVar))
            : processValue(jsonVar);
    ReadContext ctx = JsonPath.parse(strJson, jsonPathConfig);

    var softAssertions = new SoftAssertions();

    for (List<String> row : dataTable.cells()) {
      String jsonPath = resolveVariables(row.get(0));
      String expectedValue = row.get(1);
      String actualValue = "null";

      List<?> rawValueList = ctx.read(jsonPath, List.class);
      if (rawValueList.isEmpty()) {
        softAssertions.fail("Не удалось найти элемент по пути '%s'", jsonPath);
        continue;
      }
      Object rawValue = rawValueList.get(0);
      try {
        if (rawValue != null) {
          actualValue = StringHelper.getStringValue(rawValue);
        }
      } catch (JsonProcessingException ex) {
        throw new RuntimeException(
            String.format("Не удалось преобразовать значение %s в текст!", rawValue));
      }

      if (expectedValue != null) {
        expectedValue = processValue(expectedValue);
      } else {
        expectedValue = "null";
      }

      if (expectedValue.equalsIgnoreCase("not_null")) {
        softAssertions
            .assertThat(actualValue)
            .as(
                "JsonPath: '%s', ожидаемое значение: не равно null, фактическое значение: '%s'",
                jsonPath, actualValue)
            .isNotNull();
      } else {
        softAssertions
            .assertThat(actualValue)
            .as(
                "JsonPath: '%s', ожидаемое значение: '%s', фактическое значение: '%s'",
                jsonPath, expectedValue, actualValue)
            .isEqualTo(expectedValue);
      }
    }
    softAssertions.assertAll();
  }

  /**
   * В json строке, сохраннённой в переменной, происходит поиск значений по jsonpath из первого
   * столбца таблицы. Полученные значения сохраняются в переменных. Название переменной указывается
   * во втором столбце таблицы. Шаг работает со всеми типами json элементов: объекты, массивы,
   * строки, числа, литералы true, false и null.
   */
  @И(
      "^значения из json (строки|файла) \"(.*)\", найденные по JsonPath из таблицы, сохранены в переменные$")
  public void getValuesFromJsonAsString(String strOrFile, String jsonVar, DataTable dataTable) {
    String strJson =
        "файле".equals(strOrFile)
            ? getFileContentOrValue(processValue(jsonVar))
            : processValue(jsonVar);
    ReadContext ctx = JsonPath.parse(strJson, jsonPathConfig);

    var softAssertions = new SoftAssertions();

    for (List<String> row : dataTable.cells()) {
      String jsonPath = resolveVariables(row.get(0));
      String varName = row.get(1);
      String jsonElement;

      List<?> rawValueList = ctx.read(jsonPath, List.class);
      if (rawValueList.isEmpty()) {
        softAssertions.fail("Не удалось найти элемент по пути '%s'", jsonPath);
        continue;
      }
      Object rawValue = rawValueList.get(0);
      try {
        jsonElement = StringHelper.getStringValue(rawValue);
      } catch (JsonProcessingException ex) {
        throw new RuntimeException(
            String.format("Не удалось преобразовать значение %s в текст!", rawValue));
      }
      akitaScenario.log(String.format("Переменная %s = %s", varName, jsonElement));
      akitaScenario.setVar(varName, jsonElement);
    }
    softAssertions.assertAll();
  }

  /**
   * Сохранить json в виде строки с экранированными спец-символами (кавычки и т.д.)
   *
   * @param jsonVarOrPath json в текстовом виде, или переменная, в которой сохранён json, или путь
   *     (достаточно относительного пути, можно даже просто имя файла ресурсов) к файлу с json'ом
   *     внутри
   * @param varName имя переменной сценария для сохранения результата
   */
  @И("^сохраняем json \"(.*)\" в виде строки в переменную \"(.*)\"$")
  public void saveJsonAsString(String jsonVarOrPath, String varName) {
    var jsonString = processValue(jsonVarOrPath);
    if (FileHelper.isFilePathValid(jsonString)) {
      jsonString =
          jsonString.contains("src")
              ? resolveVariables(
                  FileHelper.getFileContent(FileHelper.getFileFromProject(jsonString)))
              : resolveVariables(FileHelper.getResourcesFileContent(jsonString));
    }
    var result = StringEscapeUtils.escapeJson(jsonString);
    akitaScenario.setVar(varName, result);
    akitaScenario.log(String.format("Значение %s: %s", varName, result));
  }

  /**
   * Сохраняем несколько значения по (JsonPath|GPath) из REST ответа. Путь (JsonPath|GPath) и имя
   * переменной указывается в таблице вида: | path | var name | | json.path.id | current_id |
   *
   * @param jsonVar переменная, содержащая REST response.
   * @param pathType с помощью чего будем осуществлять поиск: JsonPath или GPath
   * @param dataTable таблица с (JsonPath|GPath) и именами переменных для сохранения.
   */
  @И("^из ответа \"(.*)\" значения тела, найденные по (jsonpath|GPath)?, сохранены в переменные$")
  public void getValuesFromJsonAsString(
      String jsonVar, String pathType, Map<String, String> dataTable) {
    Response response = (Response) akitaScenario.getVar(jsonVar);
    SoftAssertions softAssertions = new SoftAssertions();
    dataTable.forEach(
        (key, value) -> {
          String jsonPath = resolveVariables(key);
          Object object;
          try {
            if (pathType.equals("GPath")) {
              object = response.body().path(jsonPath);
            } else {
              object = JsonPath.read(response.body().asPrettyString(), jsonPath);
            }
            String jsonElement = StringHelper.objectToJsonString(object);
            akitaScenario.setVar(value, jsonElement);
            akitaScenario.log(
                "JsonPath: "
                    + jsonPath
                    + ", значение: "
                    + jsonElement
                    + ", записано в переменную: "
                    + value);
          } catch (PathNotFoundException | JsonProcessingException e) {
            akitaScenario.log(e);
            softAssertions.fail("Не удалось найти элемент по пути " + jsonPath);
          }
        });
    softAssertions.assertAll();
  }

  /**
   * Проверяем несколько значения по (JsonPath|GPath) из REST ответа. Путь (JsonPath|GPath) и
   * ожидаемое значение указывается в таблице вида: | path | var name | | json.path.id | current_id
   * |
   *
   * @param jsonVar переменная, содержащая REST response.
   * @param pathType с помощью чего будем осуществлять поиск: JsonPath или GPath
   * @param dataTable таблица с (JsonPath|GPath) и именами переменных для сохранения.
   */
  @И(
      "^из ответа \"(.*)\" значения тела, найденные по (jsonpath|GPath)?, равны значениям из таблицы$")
  public void checkValuesFromJsonAsString(
      String jsonVar, String pathType, Map<String, String> dataTable) {
    Response response = (Response) akitaScenario.getVar(jsonVar);

    SoftAssertions softAssertions = new SoftAssertions();
    String failMessage =
        "%nЭлемент по пути %s не равен ожидаемому значению:%n   Ожидаемое значение: %s%n   Текущее значение: %s";

    dataTable.forEach(
        (key, value) -> {
          String jsonPath = resolveVariables(key);
          value = resolveVariables(value);
          Object object;
          try {
            if (pathType.equals("GPath")) {
              object = response.body().path(jsonPath);
            } else {
              object = JsonPath.read(response.body().prettyPrint(), jsonPath);
            }
            String jsonElement = StringHelper.objectToJsonString(object);
            if (object == null) {
              softAssertions
                  .assertThat("null")
                  .withFailMessage(String.format(failMessage, jsonPath, value, "null"))
                  .isEqualTo(value);
            } else if (!value.equalsIgnoreCase("not_null")) {
              softAssertions
                  .assertThat(jsonElement)
                  .withFailMessage(String.format(failMessage, jsonPath, value, jsonElement))
                  .isEqualTo(value);
            }
          } catch (PathNotFoundException | JsonProcessingException e) {
            akitaScenario.log(e);
            softAssertions.fail("Не удалось найти элемент по пути " + jsonPath);
          }
        });
    softAssertions.assertAll();
  }

  /**
   * Метод сравнивает json пременные из сценария с учётом сортировки в json
   *
   * @param jsonVar1 1ая сравниваемая переменная
   * @param jsonVar2 2ая сравниваемая переменная
   */
  @SneakyThrows
  @И("^json переменные \"(.*)\" и \"(.*)\" совпадают с учётом сортировки$")
  public void compareJsonWithSort(String jsonVar1, String jsonVar2) {
    String jsonValue1 = StringHelper.objectToJsonString(akitaScenario.getVar(jsonVar1));
    String jsonValue2 = StringHelper.objectToJsonString(akitaScenario.getVar(jsonVar2));
    JsonAssertions.assertThatJson(jsonValue1).isEqualTo(jsonValue2);
  }

  /**
   * Метод сравнивает json пременные из сценария без учёта сортировки в json
   *
   * @param jsonVar1 1ая сравниваемая переменная
   * @param jsonVar2 2ая сравниваемая переменная
   */
  @SneakyThrows
  @И("^json переменные \"(.*)\" и \"(.*)\" совпадают без учёта сортировки$")
  public void compareJsonWithoutSort(String jsonVar1, String jsonVar2) {
    String jsonValue1 = StringHelper.objectToJsonString(akitaScenario.getVar(jsonVar1));
    String jsonValue2 = StringHelper.objectToJsonString(akitaScenario.getVar(jsonVar2));
    JsonAssertions.assertThatJson(jsonValue1)
        .when(net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER)
        .isEqualTo(jsonValue2);
  }

  /**
   * Метод сравнивает json пременные из сценария с учётом сортировки в json и игнорируемыми
   * элементами
   *
   * @param jsonVar1 1ая сравниваемая переменная
   * @param jsonVar2 2ая сравниваемая переменная
   * @param ignoreElements список jsonPath игнорируемых элементов
   */
  @SneakyThrows
  @И("^json переменные \"(.*)\" и \"(.*)\" совпадают с учётом сортировки, игнорируемые элементы:$")
  public void compareJsonWithSortAndIgnoreElements(
      String jsonVar1, String jsonVar2, DataTable ignoreElements) {
    String jsonValue1 = StringHelper.objectToJsonString(akitaScenario.getVar(jsonVar1));
    String jsonValue2 = StringHelper.objectToJsonString(akitaScenario.getVar(jsonVar2));
    String[] ignoreElementsArray = ignoreElements.asList().toArray(new String[0]);
    JsonAssertions.assertThatJson(jsonValue1)
        .whenIgnoringPaths(ignoreElementsArray)
        .isEqualTo(jsonValue2);
  }

  /**
   * Метод сравнивает json пременные из сценария без учёта сортировки в json и игнорируемыми
   * элементами
   *
   * @param jsonVar1 1ая сравниваемая переменная
   * @param jsonVar2 2ая сравниваемая переменная
   * @param ignoreElements список jsonPath игнорируемых элементов
   */
  @SneakyThrows
  @И("^json переменные \"(.*)\" и \"(.*)\" совпадают без учёта сортировки, игнорируемые элементы:$")
  public void compareJsonWithoutSortAndIgnoreElements(
      String jsonVar1, String jsonVar2, DataTable ignoreElements) {
    String jsonValue1 = StringHelper.objectToJsonString(akitaScenario.getVar(jsonVar1));
    String jsonValue2 = StringHelper.objectToJsonString(akitaScenario.getVar(jsonVar2));
    String[] ignoreElementsArray = ignoreElements.asList().toArray(new String[0]);
    JsonAssertions.assertThatJson(jsonValue1)
        .when(net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER)
        .whenIgnoringPaths(ignoreElementsArray)
        .isEqualTo(jsonValue2);
  }

  /**
   * Шаг для проверки переданного json'а на соответствие условиям из таблицы параметров.
   *
   * <p>Колонки таблицы параметров: - Path - jsonpath (если начинается с '$.') или GPath (в ином
   * случае) к проверяемому полю json'а - Тип - тип значения поля по указанному пути (строка, число,
   * булевый, json-объект, json-массив, дата) - Условие - условие, которому должно отвечать значение
   * поля (равно, не равно, а также отдельные условия для каждого Типа - Значение - ожидаемое
   * значение - Допуск - допустимое отклонение от ожидаемого значения - для Типов 'число' (тогда
   * допуск - это число: 3 или 5.131) и 'дата' (тогда допуск - одно из значений типа 3s, 7m, 1h, 2d
   * - секунды, минуты, часы или дни) - Игнорируемые поля - jsonpath'ы полей, не учитываемых при
   * сравнении json-объектов или json-массивов (если полей несколько - перечисляются через запятую,
   * см. примеры)
   *
   * <p>Примеры: 1. То для json переменной "json1" выполняются условия из таблицы: | Path | Значение
   * | | data[0].anyNullField | null | 2. То для json переменной "json1" выполняются условия из
   * таблицы: | Path | Тип | Условие | | data[0].productCampaign | строка | пустая | |
   * data[0].productName | строка | не пустая | 3. То для json переменной "json1" выполняются
   * условия из таблицы: | Path | Тип | Условие | Значение | Допуск | | data[0].productName | строка
   * | равно | Гарантия возврата аванса | | | data[0].productCampaign | строка | пустая | | | |
   * data[0].productName | строка | содержит | возврата | | | data[0].productName | строка |
   * начинается с | Гарантия | | | data[0].productName | | не | null | | | total | число | равно |
   * ${value106} | | | total | число | равно | 104 | 2 | 4. То для json переменной "json1"
   * выполняются условия из таблицы: | Path | Тип | Условие | Значение | Игнорируемые поля | |
   * data[1] | json-объект | равно | ${json3} | | | data | json-массив | равно | ${json5} | | | data
   * | json-массив | равно без учёта сортировки | ${json6} | [*].productCampaign, [*].tags[*].id | |
   * tasks[0].intArray | json-массив | равно | [1, 2, ${value106}] | | | tasks[0].intArray |
   * json-массив | равно | ${intArray} | | | tasks[0].stringArray1 | json-массив | равно без учёта
   * сортировки | ["3", "2", "1"] | | | tasks[0].stringArray3 | json-массив | равно |
   * ["${value106}", "2", "c"] | |
   *
   * @param jsonVar проверяемый json
   * @param conditions список параметров проверок
   */
  @SneakyThrows
  @И("^для json переменной \"(.*)\" выполняются условия из таблицы:?$")
  public void assertJsonVariable(String jsonVar, List<JsonAssertableConditionParam> conditions) {
    var json = processValue(jsonVar);
    var jsonPath = io.restassured.path.json.JsonPath.from(json);

    MainAssert mainAssert = new MainAssert();
    conditions.forEach(
        condition -> {
          Object actualValue =
              condition.getPath().startsWith("$")
                  ? JsonPath.read(json, condition.getPath())
                  : jsonPath.get(condition.getPath());
          mainAssert.getAssertion(condition.getType()).accept(condition, actualValue);
        });
    mainAssert.assertAll();
  }

  /**
   * Шаг сохраняет значение массива в хранилище переменных
   *
   * @param jsonVar строка или файл с json-ом
   * @param dataTable таблица с jsonPath и именами переменных
   */
  @И(
      "^значения из json (строки|файла) \"(.*)\", найденные по JsonPath из таблицы, сохранены в виде массива в переменные$")
  public void getValuesFromJsonAsString2(String strOrFile, String jsonVar, DataTable dataTable) {
    String strJson =
        "файле".equals(strOrFile)
            ? getFileContentOrValue(processValue(jsonVar))
            : processValue(jsonVar);
    ReadContext ctx = JsonPath.parse(strJson, jsonPathConfig);
    var softAssertions = new SoftAssertions();

    for (List<String> row : dataTable.cells()) {
      String jsonPath = resolveVariables(row.get(0));
      String varName = row.get(1);

      List<?> rawValueList = ctx.read(jsonPath, List.class);
      akitaScenario.log(String.format("Переменная %s = %s", varName, rawValueList));
      akitaScenario.setVar(varName, rawValueList);
    }
    softAssertions.assertAll();
  }

  /**
   * Шаг для изменения json элементов по jsonPath пути Пример: И в json строке "json" выполнено
   * преобразование и результат сохранён в переменную "jsonNew" | Path | Действие | Тип | Значение |
   * Ключ | | $.d | вставить | String | 1234 | | | $.d | вставить | Integer | 1 | | | $.a.b |
   * вставить | Boolean | true | | | $.a.b | удалить | | | | | $.a.e[1] | удалить | | | | | $.a.e[1]
   * | вставить | Integer | 99 | | | $.a | новое | Integer | 100 | c |
   *
   * @param jsonVar json строка из переменной или файла
   * @param varName имя переменной в которую сохраняется итоговый результат
   * @param dataTable таблица с параметрами
   */
  @И(
      "^в json (строке|файле) \"(.*)\" выполнено преобразование и результат сохранён в переменную \"(.*)\"$")
  public void setJsonPath(String strOrFile, String jsonVar, String varName, DataTable dataTable) {
    Configuration configuration =
        Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build();

    var dataMap = dataTable.asMaps();
    jsonVar =
        "файле".equals(strOrFile)
            ? getFileContentOrValue(processValue(jsonVar))
            : processValue(jsonVar);
    String result = jsonVar;

    for (Map<String, String> line : dataMap) {
      String jsonPath = line.get("Path");
      String action = line.get("Действие");
      String type = line.get("Тип");
      String value = line.get("Значение");
      String key = line.get("Ключ");

      try {
        JsonPath.read(result, jsonPath);
      } catch (PathNotFoundException ex) {
        throw new RuntimeException("Путь " + jsonPath + " не найден в Json: " + result);
      }

      switch (action.toLowerCase()) {
        case "удалить" -> result =
            JsonPath.using(configuration).parse(result).delete(jsonPath).jsonString();
        case "вставить" -> result =
            JsonPath.using(configuration)
                .parse(result)
                .set(jsonPath, prepareJsonValue(type, value))
                .jsonString();
        case "новое" -> {
          if (configuration
              .jsonProvider()
              .isArray(JsonPath.using(configuration).parse(result).read(jsonPath))) {
            result =
                JsonPath.using(configuration)
                    .parse(result)
                    .add(jsonPath, prepareJsonValue(type, value))
                    .jsonString();
          } else if (key == null) {
            throw new RuntimeException(
                "Ключ - обязательное значение для действия \"новое\", оно не может быть null.");
          } else {
            result =
                JsonPath.using(configuration)
                    .parse(result)
                    .put(jsonPath, key, prepareJsonValue(type, value))
                    .jsonString();
          }
        }
        default -> throw new RuntimeException("Нет такого действия: " + action);
      }
    }
    akitaScenario.setVar(varName, result);
    akitaScenario.log(result);
  }

  /**
   * Подготавливает значение для Json - преоброазовывает в правильный формат, разименовывает
   * переменные.
   *
   * @param type тип значения.
   * @param value необработанное значение.
   * @return обработанное значение в нужном формате.
   */
  private Object prepareJsonValue(String type, String value) {
    if (value != null) {
      value = resolveVariables(value);
      return switch (type) {
        case "Integer" -> Integer.parseInt(value);
        case "Long" -> Long.parseLong(value);
        case "Double" -> Double.parseDouble(value);
        case "Boolean" -> Boolean.parseBoolean(value);
        case "Map" -> new HashMap<>();
        case "Array" -> new ArrayList<>();
        case "Null" -> null;
        default -> value;
      };
    } else {
      return switch (type) {
        case "Map" -> new HashMap<>();
        case "Array" -> new ArrayList<>();
        case "Null" -> null;
        default -> throw new RuntimeException(
            "Значение null может быть только у типов Null, Map и Array. Текущий тип: " + type);
      };
    }
  }

  /**
   * Шаг для проверки того что по jsonPath путь не существует
   *
   * @param responseVar имя переменной с сохранёнными ответом от RestAssured
   * @param dataTable список ключей для проверки
   */
  @И("^из ответа \"(.*)\" ключ по jsonpath не найден$")
  public void checkNotExistJsonpath(String responseVar, List<String> dataTable) {
    Response response = (Response) akitaScenario.getVar(responseVar);
    SoftAssertions softAssertions = new SoftAssertions();

    dataTable.forEach(
        (path) -> {
          String jsonPath = resolveVariables(path);
          Object obj;
          try {
            obj = JsonPath.read(response.body().prettyPrint(), jsonPath);
          } catch (PathNotFoundException e) {
            akitaScenario.log(e);
            return;
          }
          softAssertions.fail("По пути %s найдено значение = %s", jsonPath, obj);
        });
    softAssertions.assertAll();
  }
}
