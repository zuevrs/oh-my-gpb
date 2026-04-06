package ru.gazprombank.automation.akitagpb.modules.ccl.database.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.prepareStringFromList;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValueWithoutProperty;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

import io.cucumber.core.exception.CucumberException;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.И;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import ru.gazprombank.automation.akitagpb.modules.ccl.database.DataBaseMethods;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class CCLDataBaseSteps extends BaseMethods {

  /**
   * Выполняет запрос в БД с параметрами.<br>
   * Параметры запросы должны бфть заранее подготовлены в виде переменных сценария.<br>
   *
   * @param dbName название базы данных.
   * @param query название подготовленного запроса или сам запрос. Список запросов можно посмотреть
   *     в файлах конфигурации.
   */
  @И("^в БД \"(.+)\" выполняем запрос \"(.+)\" с параметрами$")
  public void executeQueryWithParameters(String dbName, String query) {
    // выполняем запрос в БД
    DataBaseMethods.queryInDatabase(dbName, query);
  }

  /**
   * Выполняет запрос в БД с параметрами.<br>
   * Параметры запросы должны бфть заранее подготовлены в виде переменных сценария.<br>
   * Так же недостающие переменные можно задать под шагмо в виде таблицы:<br>
   * | имя переменной | занчение |
   *
   * @param dbName название базы данных.
   * @param query название подготовленного запроса или сам запрос. Список запросов можно посмотреть
   *     в файлах конфигурации.
   * @param parameters опциональный паараметр, содержащий таблицу с именем переменной, которую
   *     требуется создать и значением, которое туда нужно поместить.
   */
  @И(
      "^в БД \"(.+)\" выполняем запрос \"(.+)\" с параметрами из таблицы( без перезаписи переменных)?$")
  public void executeQueryWithParameters(
      String dbName, String query, String rewriteVars, Map<String, String> parameters) {
    Map<String, Object> parametersBefore = new HashMap<>(akitaScenario.getVars().getVariables());
    // объявляем все переменные, которые переданы вместе с запросом
    parameters.forEach((key, value) -> akitaScenario.setVar(key, resolveVariables(value)));
    // выполняем запрос в БД
    DataBaseMethods.queryInDatabase(dbName, query);
    if (rewriteVars != null) {
      parametersBefore.forEach((key, value) -> akitaScenario.setSystemVar(key, value));
    }
  }

  /**
   * Выполняет запрос в БД с параметрами. Сохраняет результат в переменные сценария.<br>
   * Параметры запросы должны бфть заранее подготовлены в виде переменных сценария.<br>
   * Так же недостающие переменные можно задать под шагмо в виде таблицы:<br>
   * | имя переменной | занчение |
   *
   * @param dbName название базы данных.
   * @param query название подготовленного запроса или сам запрос. Список запросов можно посмотреть
   *     в файлах конфигурации.
   * @param varName название переменной сценария, в котрую будет сохранён результат.
   */
  @И("^в БД \"(.+)\" выполняем запрос \"(.+)\" с параметрами и сохраняем результат в \"(.+)\"$")
  public void executeQueryWithParametersAndSave(String dbName, String query, String varName) {
    akitaScenario.setVar(varName, DataBaseMethods.queryInDatabase(dbName, query));
  }

  /**
   * Выполняет запрос в БД с параметрами. Сохраняет результат в переменные сценария.<br>
   * Параметры запросы должны бфть заранее подготовлены в виде переменных сценария.<br>
   * Так же недостающие переменные можно задать под шагмо в виде таблицы:<br>
   * | имя переменной | занчение |
   *
   * @param dbName название базы данных.
   * @param query название подготовленного запроса или сам запрос. Список запросов можно посмотреть
   *     в файлах конфигурации.
   * @param varName название переменной сценария, в котрую будет сохранён результат.
   * @param parameters опциональный паараметр, содержащий таблицу с именем переменной, которую
   *     требуется создать и значением, которое туда нужно поместить.
   */
  @И(
      "^в БД \"(.+)\" выполняем запрос \"(.+)\" с параметрами из таблицы( без перезаписи переменных)? и сохраняем результат в \"(.+)\"$")
  public void executeQueryWithParametersAndSave(
      String dbName,
      String query,
      String rewriteVars,
      String varName,
      Map<String, String> parameters) {
    Map<String, Object> parametersBefore = new HashMap<>(akitaScenario.getVars().getVariables());
    // объявляем все переменные, которые переданы вместе с запросом
    parameters.forEach((key, value) -> akitaScenario.setVar(key, resolveVariables(value)));
    // выполняем запрос и сохраняем результат в переменную
    akitaScenario.setVar(varName, DataBaseMethods.queryInDatabase(dbName, query));
    if (rewriteVars != null) {
      parametersBefore.forEach((key, value) -> akitaScenario.setSystemVar(key, value));
    }
  }

  /**
   * Выполняет запрос в БД и ожидает пустой ответ.
   *
   * @param dbName название базы данных.
   * @param query название подготовленного запроса или сам запрос. Список запросов можно посмотреть
   *     в файлах конфигурации.
   */
  @И("^в БД \"(.+)\" выполняем запрос \"(.+)\" и получаем пустой ответ$")
  public void executePreparedNullQuery(String dbName, String query) {
    if (!DataBaseMethods.queryInDatabase(dbName, query).isEmpty()) {
      throw new CucumberException(
          "Были найдены записи по запросу: " + DataBaseMethods.getQuery(dbName, query));
    }
  }

  /**
   * Выполняет запрос в БД и ждём пока не придёт непустой ответ.
   *
   * @param dbName название базы данных.
   * @param query название подготовленного запроса или сам запрос. Список запросов можно посмотреть
   *     в файлах конфигурации.
   */
  @И(
      "^в БД \"(.+)\" выполняем запрос \"(.+)\" с параметрами из таблицы и ждём пока придёт непустой ответ$")
  public void executePreparedNullQuery2(
      String dbName, String query, Map<String, String> parameters) {
    try {
      parameters.forEach((key, value) -> akitaScenario.setVar(key, resolveVariables(value)));
      Awaitility.await()
          .pollInSameThread()
          .timeout(60, TimeUnit.SECONDS)
          .pollInterval(1, TimeUnit.SECONDS)
          .until(() -> !DataBaseMethods.queryInDatabase(dbName, query).isEmpty());
    } catch (ConditionTimeoutException e) {
      throw new ConditionTimeoutException("Время ожидания истекло");
    }
  }

  /**
   * Проверяет значения полей в ответе от БД. Имена столбцов БД таблицы и ожидаемые значения
   * указываются в таблице:<br>
   * &nbsp;&nbsp;| имя столбца | ожидаемое значение |<br>
   * Пример:<br>
   * &nbsp;&nbsp;| customer_id | 123 |
   *
   * @param valueName имя переменной, в которой хранится ответ от БД.
   * @param dataTable таблица с названиями полей и ожидаемыми значениями.
   */
  @SuppressWarnings("unchecked")
  @И("^в ответе от БД \"(.+)\", проверяем значения полей$")
  public void checkDataFromDb(String valueName, Map<String, String> dataTable) {
    var resultList = (List<Map<String, Object>>) this.akitaScenario.getVar(valueName);
    var resultMap = resultList.get(0);
    SoftAssertions softAssertions = new SoftAssertions();

    for (Map.Entry<String, String> entry : dataTable.entrySet()) {
      String expectedValue = processValue(entry.getValue());

      if (!resultMap.containsKey(entry.getKey())) {
        softAssertions
            .assertThat(false)
            .withFailMessage(String.format("Не удалось найти элемент по пути '%s'", entry.getKey()))
            .isTrue();
        continue;
      }

      Object actualValue = resultMap.get(entry.getKey());

      if (entry.getValue().equalsIgnoreCase("null")) {
        softAssertions
            .assertThat(actualValue)
            .withFailMessage(
                String.format(
                    "DataRowPath: '%s', ожидаемое значение должно быть пустое, фактическое значение: '%s'",
                    entry.getKey(), actualValue))
            .isNull();
        continue;
      }

      if (entry.getValue().equalsIgnoreCase("not_null")) {
        softAssertions
            .assertThat(actualValue)
            .withFailMessage(
                String.format(
                    "DataRowPath: '%s', ожидаемое значение должно быть не пустое, фактическое значение: '%s",
                    entry.getKey(), actualValue))
            .isNotNull();
        continue;
      }

      softAssertions
          .assertThat(actualValue.toString())
          .withFailMessage(
              String.format(
                  "DataRowPath: '%s', ожидаемое значение: '%s', фактическое значение: '%s'",
                  entry.getKey(), expectedValue, actualValue))
          .isEqualTo(expectedValue);
    }

    softAssertions.assertAll();
  }

  /**
   * Шаг для проверки полей ответа от БД, когда в ответе может быть несколько строк. Шаг применим,
   * когда важен порядок (сортировка) строк. Данный шаг проходит в цикле по списку строк,
   * полученному из БД: берёт первую строку из БД и первую строку из ожидаемой таблицы, и вызывает
   * для них шаг {@link #checkDataFromDb(String, Map)}, и т.д. для всех пар строк. В первой строке
   * таблицы параметров - проверяемые поля каждой строки из БД, в следующих строках - ожидаемые
   * значения для каждой строки. Пример: И в ответе от БД "dbResponse" проверяем значения полей с
   * учётом порядка строк | code | name | visible | | 24 | Действующее | true | | 36 | Ликвидировано
   * | true |
   *
   * @param valueName имя переменной, в которой хранится ответ от БД
   * @param dataTable таблица с названиями полей и ожидаемыми значениями
   */
  @SuppressWarnings("unchecked")
  @И("^в ответе от БД \"(.+)\" проверяем значения полей с учётом порядка строк:?$")
  public void checkOrderedRowsFromDb(String valueName, DataTable dataTable) {
    var actualMaps = (List<Map<String, Object>>) this.akitaScenario.getVar(valueName);
    var expectedMaps = dataTable.asMaps();

    var tempValueName = "actualDbRowFromTestMethod";
    SoftAssertions softAssertions = new SoftAssertions();
    for (int i = 0; i < actualMaps.size(); i++) {
      List<Map<String, Object>> actualRow = new ArrayList<>();
      actualRow.add(actualMaps.get(i));
      var expectedRow = expectedMaps.get(i);
      akitaScenario.setVar(tempValueName, actualRow);
      akitaScenario.log(String.format("Строка %s из БД: %n%s", i, actualRow.get(0)));
      akitaScenario.log(String.format("Ожидаемая строка %s: %n%s", i, expectedRow));
      try {
        checkDataFromDb(tempValueName, expectedRow);
      } catch (AssertionError e) {
        softAssertions.fail(String.format("Ошибка при сравнении строк %s: %s", i, e.getMessage()));
      }
    }
    softAssertions
        .assertThat(actualMaps)
        .as("Количество строк в БД не равно ожидаемому количеству строк")
        .hasSameSizeAs(expectedMaps);
    softAssertions.assertAll();
  }

  /**
   * Шаг для проверки полей ответа от БД, когда в ответе может быть несколько строк. Шаг применим,
   * когда порядок (сортировка) строк НЕ важен. В первой строке таблицы параметров - проверяемые
   * поля каждой строки из БД, в следующих строках - ожидаемые значения для каждой строки. Пример: И
   * в ответе от БД "dbResponse" проверяем значения полей без учёта порядка строк | code | name |
   * visible | | 36 | Ликвидировано | true | | 24 | Действующее | true |
   *
   * @param valueName имя переменной, в которой хранится ответ от БД
   * @param dataTable таблица с названиями полей и ожидаемыми значениями
   */
  @SuppressWarnings("unchecked")
  @И("^в ответе от БД \"(.+)\" проверяем значения полей без учёта порядка строк:?$")
  public void checkUnorderedRowsFromDb(String valueName, DataTable dataTable) {
    var actualMaps = (List<Map<String, Object>>) this.akitaScenario.getVar(valueName);
    var actualMapsCopy = new ArrayList<>(actualMaps);
    var expectedMaps = dataTable.asMaps();

    SoftAssertions softAssertions = new SoftAssertions();
    expectedMaps.forEach(
        e -> {
          akitaScenario.log(String.format("Поиск ожидаемой строки %s в БД", e));
          var found = findExpectedRowInListAndRemove(actualMapsCopy, e);
          akitaScenario.log(
              found != null ? "Строка найдена в БД: \n" + found : "Строка НЕ найдена в БД");
          softAssertions
              .assertThat(found)
              .as(String.format("Ожидаемая строка %s в БД не найдена", e))
              .isNotNull();
        });
    softAssertions
        .assertThat(actualMaps)
        .as("Количество строк в БД не равно ожидаемому количеству строк")
        .hasSameSizeAs(expectedMaps);
    softAssertions.assertAll();
  }

  /**
   * Сохраняет значение столбца из ответа от БД в виде текста (String) в переменную.
   *
   * @param dataFromRow имя переменной, сожержащей ответ от БД.
   * @param key имя столбца таблицы БД.
   * @param variableName имя переменной, куда положить значение поля.
   */
  @SuppressWarnings("unchecked")
  @И(
      "^из ответа от БД \"(.*)\" значение столбца \"(.*)\" сохранено как ?(текст|объект) в переменную \"(.*)\"$")
  public void saveValueFromDbAnswerToVariable(
      String dataFromRow, String key, String typeObject, String variableName) {
    var data = (List<Map<String, Object>>) this.akitaScenario.getVar(dataFromRow);
    Object value = data.stream().map(map -> map.get(key)).findFirst().orElse(null);
    assertThat(value).as("По пути " + key + " элемент не найден").isNotNull();
    if ("объект".equals(typeObject)) {
      this.akitaScenario.setVar(variableName, value);
    } else {
      this.akitaScenario.setVar(variableName, value.toString());
    }
    this.akitaScenario.log(String.format("Значение %s: %s", key, value));
  }

  /**
   * Сохраняет значение столбца из ответа от БД в виде текста (String) в переменную.
   *
   * @param dataFromRow имя переменной, сожержащей ответ от БД.
   * @param key имя столбца таблицы БД.
   * @param variableName имя переменной, куда положить значение поля.
   */
  @SuppressWarnings("unchecked")
  @И(
      "^из ответа от БД \"(.*)\" значение столбца \"(.*)\" сохранено как массив строк формата \"(.*)\" в переменную \"(.*)\"$")
  public void saveValueListFromDbAnswerToVariable(
      String dataFromRow, String key, String format, String variableName) {
    var data = (List<Map<String, Object>>) this.akitaScenario.getVar(dataFromRow);
    List<String> values = data.stream().map(map -> map.get(key).toString()).toList();
    assertThat(values).as("По пути " + key + " значения не найдены").isNotEmpty();
    String result = prepareStringFromList(values, format);
    this.akitaScenario.setVar(variableName, result);
    this.akitaScenario.log(String.format("Значение %s: %s", key, result));
  }

  /**
   * Метод выполняет запрос в БД и сохраняет результат в виде json строки.
   *
   * @param dbName название базы данных.
   * @param query название подготовленного запроса или сам запрос. Список запросов можно посмотреть
   *     в файлах конфигурации.
   * @param varName название переменной сценария, в котрую будет сохранён результат.
   */
  @SneakyThrows
  @И(
      "^в БД \"(.+)\" выполняем запрос \"(.+)\" с параметрами и сохраняем результат в переменную \"(.+)\" в виде json строки$")
  public void executeQueryAndSaveJsonString(String dbName, String query, String varName) {
    String varJsonFormatValue =
        StringHelper.objectToJsonString(DataBaseMethods.queryInDatabase(dbName, query));
    akitaScenario.setVar(varName, varJsonFormatValue);
  }

  /**
   * Выполняет запрос обновления или удаления в БД
   *
   * @param dbName название базы данных.
   * @param query название подготовленного запроса или сам запрос. Список запросов можно посмотреть
   *     в файлах конфигурации.
   * @return количество изменённых строк
   */
  @И("^в БД \"(.+)\" выполняем запрос (?:обновления|удаления|добавления) \"(.+)\"$")
  public int executeUpdateQuery(String dbName, String query) {
    int rowResult = DataBaseMethods.updateQueryInDatabase(dbName, query);
    akitaScenario.log(String.format("Количество изменяемых строк = %s", rowResult));
    return rowResult;
  }

  /**
   * Выполняет запрос обновления или удаления в БД с параметрами из таблицы
   *
   * @param dbName название базы данных.
   * @param query название подготовленного запроса или сам запрос. Список запросов можно посмотреть
   *     в файлах конфигурации.
   * @param parameters таблица с параметрами, которые будут вставляться в запрос
   * @return количество изменённых строк
   */
  @И(
      "^в БД \"(.+)\" выполняем запрос (?:обновления|удаления|добавления) \"(.+)\" с параметрами из таблицы$")
  public int executeUpdateQueryWithParameters(
      String dbName, String query, Map<String, String> parameters) {
    // объявляем все переменные, которые переданы вместе с запросом
    parameters.forEach((key, value) -> akitaScenario.setVar(key, resolveVariables(value)));
    int rowResult = DataBaseMethods.updateQueryInDatabase(dbName, query);
    akitaScenario.log(String.format("Количество изменяемых строк = %s", rowResult));
    return rowResult;
  }

  /**
   * Выполняет запрос обновления или удаления в БД. После выполнения проверяет что количество
   * изменяемых строк больше 0.
   *
   * @param dbName название базы данных.
   * @param query название подготовленного запроса или сам запрос. Список запросов можно посмотреть
   *     в файлах конфигурации.
   */
  @И(
      "^в БД \"(.+)\" выполняем запрос (?:обновления|удаления|добавления) \"(.+)\" и проверяем полученный ответ$")
  public void executeUpdateQueryAndCheckResponse(String dbName, String query) {
    int rowResult = executeUpdateQuery(dbName, query);
    assertThat(rowResult)
        .withFailMessage("Количество изменяемых строк должно быть больше 0")
        .isGreaterThan(0);
  }

  /**
   * Выполняет запрос обновления или удаления в БД с параметрами из таблицы. После выполнения
   * проверяет что количество изменённых строк больше 0.
   *
   * @param dbName название базы данных.
   * @param query название подготовленного запроса или сам запрос. Список запросов можно посмотреть
   *     в файлах конфигурации.
   * @param parameters таблица с параметрами, которые будут вставляться в запрос
   */
  @И(
      "^в БД \"(.+)\" выполняем запрос (?:обновления|удаления|добавления) \"(.+)\" с параметрами из таблицы и проверяем полученный ответ$")
  public void executeUpdateQueryWithParametersAndCheckResponse(
      String dbName, String query, Map<String, String> parameters) {
    int rowResult = executeUpdateQueryWithParameters(dbName, query, parameters);
    assertThat(rowResult)
        .withFailMessage("Количество изменяемых строк должно быть больше 0")
        .isGreaterThan(0);
  }

  private Map<String, Object> findExpectedRowInListAndRemove(
      List<Map<String, Object>> dbRows, Map<String, String> expectedRow) {
    var element =
        dbRows.stream()
            .filter(
                actualMap -> {
                  boolean isFound = true;
                  for (Map.Entry<String, String> entry : expectedRow.entrySet()) {
                    String expectedValue = processValueWithoutProperty(entry.getValue());

                    if (!actualMap.containsKey(entry.getKey())) {
                      isFound = false;
                      break;
                    }
                    Object actualValue = actualMap.get(entry.getKey());
                    if (entry.getValue().equalsIgnoreCase("null") && actualValue == null) {
                      continue;
                    }
                    if (entry.getValue().equalsIgnoreCase("not_null") && actualValue != null) {
                      continue;
                    }
                    if (actualValue != null && actualValue.toString().equals(expectedValue)) {
                      continue;
                    }
                    isFound = false;
                    break;
                  }
                  return isFound;
                })
            .findFirst()
            .orElse(null);
    if (element != null) {
      dbRows.remove(element);
    }
    return element;
  }
}
