package ru.gazprombank.automation.akitagpb.modules.database.steps;

import static ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables.resolveVars;
import static ru.gazprombank.automation.akitagpb.modules.core.helpers.PropertyLoader.loadValueFromFileOrPropertyOrVariableOrDefault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.И;
import io.qameta.allure.Allure;
import java.util.*;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

/** Шаги для тестирования БД, доступные по умолчанию в каждом новом проекте */
@Slf4j
public class DataBaseSteps extends BaseMethods {

  private static final DataSourceProperties dataSourceProperties = new DataSourceProperties();

  @И(
      "^создано подключение к БД с драйвером: \"(.*)\", url: \"(.*)\", логином: \"(.*)\" и паролем: \"(.*)\", сохранено в переменную \"(.*)\"$")
  @SneakyThrows
  public void createDbConnection2(
      String driverName, String url, String login, String password, String variableName) {

    dataSourceProperties.setDriverClassName(
        BaseMethods.getInstance().getPropertyOrStringVariableOrValue(resolveVars(driverName)));
    dataSourceProperties.setUrl(
        BaseMethods.getInstance().getPropertyOrStringVariableOrValue(resolveVars(url)));
    dataSourceProperties.setUsername(
        BaseMethods.getInstance().getPropertyOrStringVariableOrValue(resolveVars(login)));
    dataSourceProperties.setPassword(
        BaseMethods.getInstance().getPropertyOrStringVariableOrValue(resolveVars(password)));

    var dataSource =
        dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();

    dataSource.setMaximumPoolSize(3);
    dataSource.setMinimumIdle(1);
    akitaScenario.setVar(variableName, dataSource);
  }

  @И(
      "^выполнен запрос \"(.*)\" к БД: \"(.*)\". Результат\\(название поля, значение\\) сохранен в переменную \"(.*)\"")
  @SneakyThrows
  public void selectInDatabase(String query, String dataSourceVar, String variableName) {
    List<Map<String, Object>> dataFromRow = new ArrayList<>();
    var dataSource = (DataSource) akitaScenario.getVar(dataSourceVar);
    var select = loadValueFromFileOrPropertyOrVariableOrDefault(resolveVars(query));
    String selectValue = resolveVars(select);
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    if (selectValue.toLowerCase().startsWith("select")) {
      try {
        dataFromRow = jdbcTemplate.queryForList(selectValue);
      } catch (EmptyResultDataAccessException emptyResultDataAccessException) {
        System.out.println("Запрос " + selectValue + " ничего не нашел!");
      }
      this.akitaScenario.log("Данные из Таблицы: " + dataFromRow);
      Allure.addAttachment("Данные из Таблицы", dataFromRow.toString());
      akitaScenario.setVar(variableName, dataFromRow);
    } else if (selectValue.toLowerCase().contains("delete")
        || selectValue.toLowerCase().startsWith("update")
        || selectValue.toLowerCase().startsWith("truncate")
        || selectValue.toLowerCase().startsWith("insert")) {
      var updated = jdbcTemplate.update(selectValue);
      if (updated > 0) {
        this.akitaScenario.log("Количество измененных записей: " + updated);
        Allure.addAttachment("Количество измененных записей", Integer.toString(updated));
      } else {
        System.out.println("Запрос " + selectValue + "ничего не обновил!");
      }
    }
  }

  @И(
      "^полученный ответ \"(.*)\" от БД преобразован в список значений и сохранен в переменную \"(.*)\"$")
  public void saveValueListFromBd(String valueName, String variableName) {
    var data = (List<Map>) this.akitaScenario.getVar(valueName);
    var values =
        Arrays.toString(data.stream().map(Map::values).toArray()).replaceAll("[\\[\\]]", "");
    this.akitaScenario.log("Список значений из Таблицы: " + values);
    akitaScenario.setVar(variableName, values);
  }

  @И("^получено количество полей в ответе от БД \"(.*)\" и сохранено в переменную \"(.*)\"$")
  public void saveListSizeFromBd(String valueName, String variableName) {
    var data = (List<Map>) this.akitaScenario.getVar(valueName);
    var size = data.stream().map(Map::size).findFirst().get();
    this.akitaScenario.log("Количество полей из Таблицы: " + size);
    akitaScenario.setVar(variableName, size);
  }

  @И("^в ответе от БД \"(.*)\", найденные значения равны значениям из таблицы$")
  public void checkDataFromDb(String valueName, DataTable dataTable) {
    var strDataRow = (List<Map>) akitaScenario.getVar(valueName);
    var softAssertions = new SoftAssertions();
    var var6 = dataTable.cells().iterator();

    while (var6.hasNext()) {
      List<String> row = (List) var6.next();
      String dataRowPath = row.get(0);
      String expectedValue = row.get(1);
      String actualValue;
      try {
        actualValue =
            strDataRow.stream()
                .map(
                    value -> {
                      if (value.containsKey(dataRowPath)) {
                        if (value.get(dataRowPath) == null) {
                          return "null";
                        } else {
                          return value.get(dataRowPath);
                        }
                      }
                      return null;
                    })
                .findAny()
                .get()
                .toString()
                .replaceAll("^\"|^'|\"$|'$", "");
      } catch (NoSuchElementException | NullPointerException var12) {
        softAssertions
            .assertThat(false)
            .withFailMessage(
                "Не удалось найти элемент по пути '"
                    + dataRowPath
                    + "';\n Exception: "
                    + var12.getMessage(),
                new Object[0])
            .isTrue();
        continue;
      }

      if (expectedValue != null) {
        expectedValue =
            BaseMethods.getInstance().getPropertyOrStringVariableOrValueAndReplace(expectedValue);
        expectedValue = resolveVars(expectedValue);
      } else {
        expectedValue = "";
      }
      softAssertions
          .assertThat(actualValue)
          .withFailMessage(
              "DataRowPath: '"
                  + dataRowPath
                  + "', ожидаемое значение: '"
                  + expectedValue
                  + "', фактическое значение: '"
                  + actualValue
                  + "'",
              new Object[0])
          .contains(expectedValue);
    }
    softAssertions.assertAll();
  }

  @И("^из ответа от БД \"(.*)\" значение столбца \"(.*)\" сохранено в переменную \"(.*)\"$")
  public void saveValueFromDbAnswerToVariable(String dataFromRow, String key, String variableName) {
    var softAssertions = new SoftAssertions();
    var data = (List<Map>) this.akitaScenario.getVar(dataFromRow);
    String value = null;
    try {
      value = data.stream().map(val -> val.get(key)).findAny().get().toString();
    } catch (NullPointerException var12) {
      softAssertions
          .assertThat(false)
          .withFailMessage(
              "Не удалось найти элемент '" + key + "';\n Exception: " + var12.getMessage(),
              new Object[0])
          .isTrue();
    }
    MatcherAssert.assertThat(
        "По пути " + key + " элемент не найден", value, Matchers.notNullValue());
    this.akitaScenario.setVar(variableName, value);
    this.akitaScenario.log(String.format("Значение %s: %s", key, value));
  }

  /*
  * Usages:

   Значения из ответа "nameDBAnswer" попадают под следующие значения
            | locator	    |restrictionMode  |	restrictionValue  |

  * Example:

   Значения из ответа "nameDBAnswer" попадают под следующие значения
           | status	        | IS_EQUAL_TO     |	  success         |
           | actualTime     | IS_NOT_EMPTY    |   true            |
           | actionCode	    | IS_EMPTY        |	  false           |
  * */
  @И("^значения из ответа \"(.*)\" попадают под следующие ограничения$")
  public void checkAnswerDBNoun(String nameVar, DataTable dataTable) {
    var softAssertions = new SoftAssertions();
    var strDataRow = (List<Map>) akitaScenario.getVar(nameVar);
    softAssertions
        .assertThat(strDataRow.isEmpty())
        .withFailMessage("Ответ от БД прилетел пустой")
        .isEqualTo(false);
    String actualValue = null;
    var var6 = dataTable.cells().iterator();
    String errLog = "";
    while (var6.hasNext()) {
      List<String> row = (List) var6.next();
      String locator = row.get(0);
      String operator = row.get(1).toUpperCase();
      String expetcedValue = row.get(2);
      errLog = locator;

      try {
        actualValue =
            strDataRow.stream()
                .map(
                    value -> {
                      return (checkCondition(
                          value.get(locator).toString(), operator.toUpperCase(), expetcedValue));
                    })
                .findAny()
                .get()
                .toString()
                .replaceAll("^\"|^'|\"$|'$", "");
      } catch (NoSuchElementException | NullPointerException e) {
        if (operator.equals("IS_EMPTY") && expetcedValue.equals("false"))
          softAssertions
              .assertThat(actualValue)
              .withFailMessage("Значение " + errLog + " из таблицы не пустое")
              .isEqualTo("false");

        if (operator.equals("IS_EQUAL_TO"))
          softAssertions
              .assertThat(actualValue)
              .withFailMessage("пустое Значение " + errLog + " из таблицы не соответствует условию")
              .isEqualTo("false");
        continue;
      }
      softAssertions
          .assertThat(actualValue)
          .withFailMessage("Значение " + errLog + " из таблицы не соответствует условию")
          .isEqualTo("true");
    }
    softAssertions.assertAll();
  }

  private static boolean checkCondition(String actualValue, String operator, String expectedValue) {
    switch (operator) {
      case "IS_EQUAL_TO":
        expectedValue = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(expectedValue);
        expectedValue = resolveVars(expectedValue);
        if (actualValue.equals("")) expectedValue = "";
        return actualValue.equals(expectedValue);
      case "IS_NOT_EMPTY":
        if (expectedValue.equals("true") && !actualValue.isEmpty()) return true;
        else if (expectedValue.equals("false") && (actualValue.isEmpty())) return true;
        else return false;
      case "IS_EMPTY":
        if ((actualValue.equals("") && expectedValue.equals("true"))) return true;
        else if ((!actualValue.isEmpty()) && expectedValue.equals("false")) return true;
        return false;
      default:
        throw new IllegalArgumentException("Неподдерживаемый оператор: " + operator);
    }
  }

  @И(
      "^полученный ответ \"(.*)\" от БД преобразован в json-строку и сохранен в переменную \"(.*)\"$")
  public void saveJsonFromBd(String valueName, String variableName) {
    List<Map> data = (List) this.akitaScenario.getVar(valueName);
    String dataAsJsonStr = "";
    try {
      dataAsJsonStr = new ObjectMapper().writeValueAsString(data.getFirst());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(
          "При попытке сохранить ответ от БД в виде json произошла ошибка:\n" + e);
    }
    akitaScenario.setVar(variableName, dataAsJsonStr);
    this.akitaScenario.log("Список значений из Json: " + dataAsJsonStr);
  }
}
