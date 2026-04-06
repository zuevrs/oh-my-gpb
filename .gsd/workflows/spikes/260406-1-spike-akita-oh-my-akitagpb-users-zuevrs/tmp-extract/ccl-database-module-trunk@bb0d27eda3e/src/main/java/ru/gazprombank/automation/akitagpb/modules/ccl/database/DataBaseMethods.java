package ru.gazprombank.automation.akitagpb.modules.ccl.database;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValueWithoutProperty;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariablesWithoutProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;
import ru.gazprombank.automation.akitagpb.modules.core.steps.isteps.IBaseMethods;

public class DataBaseMethods implements IBaseMethods {

  /**
   * Создаёт конфигурацию для подключения к БД из .conf файла. Сохраняет конфигурация в переменные
   * сценария, для дальгейшего использования. Так же сохраняет все подготовленные запросы для данной
   * конфигурации БД в переменную сценария с именем БД плюс "-queries".
   *
   * @param dbName имя конйигурации БД из .conf файла.
   * @return подготовленную конфигурацию для подключения к БД типа {@link DriverManagerDataSource}.
   */
  private static DriverManagerDataSource createDbConnection(String dbName) {
    // Получаем настройки из файла конфигурации
    Map<String, Object> dbMapConfig = ConfigLoader.getConfigValue(dbName);
    assertThat(dbMapConfig).as("Конфигурация базы данных %s не найдена", dbName).isNotNull();

    dbMapConfig =
        dbMapConfig.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, e -> processValue((String) e.getValue())));

    DataBaseConfig dbConfig = new DataBaseConfig(dbMapConfig);

    // Создаём DataSource с настройками из файла конфигурации
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName(dbConfig.getDriver());
    dataSource.setUrl(dbConfig.getUrl());
    dataSource.setUsername(dbConfig.getUsername());
    dataSource.setPassword(dbConfig.getPassword());

    // Сохраняем DataSource объект для последующего использования
    akitaScenario.setVar(dbName, dataSource);
    // Сохраняем подготовление запросы из файла конфигурации
    akitaScenario.setVar(dbName + "-queries", dbConfig.getQueries());

    return dataSource;
  }

  /**
   * Выполняет запрос с параметрами к БД, конфигурация которой указана в dbName. Если сценарий не
   * содержит переменной с конфигурацией для подключения к БД - пробует создать ей из файла .conf.
   *
   * @param dbName имя переменой сценария, содержащей конфигурацию для БД.
   * @param query запрос в БД. запросе, значение - значением, которое нужно подставить.
   * @return возвращает ответ от БД в виде {@link Map}.
   */
  public static List<Map<String, Object>> queryInDatabase(String dbName, String query) {
    // Получаем сохраненный DataSource объект. Если его нет - создаём новый
    DataSource dataSource =
        Objects.requireNonNullElseGet(
            (DataSource) akitaScenario.tryGetVar(dbName), () -> createDbConnection(dbName));

    String queryValue = getQuery(dbName, query);

    List<Map<String, Object>> dataFromRow = query(dataSource, queryValue);

    akitaScenario.log("Данные из Таблицы: " + dataFromRow);
    return dataFromRow;
  }

  /**
   * Пытается получить подготовленный запрос по тексту query. Если у конфигурации БД такой запрос не
   * надйен, пытается загрузить из свойств, файлов или переменных. Если ничего не найдено -
   * возвращает исходный текст запроса.
   *
   * @param dbName имя переменной сценария, содержащей конфигурации БД.
   * @param query имя запроса или файла, или свойства, или переменой или сам запрос.
   * @return возвращает найденный запрос или исходный текст.
   */
  @SuppressWarnings("unchecked")
  public static String getQuery(String dbName, String query) {
    // Получаем запрос из сохраненных запросов. Если такого запроса нет - используем текст из
    // запроса
    var queries = ((Map<String, String>) akitaScenario.getVar(dbName + "-queries"));
    if (!queries.containsKey(query)) {
      akitaScenario.log(
          String.format(
              """
                                В конфигурации БД "%s" нет запроса "%s".
                                Доступные запросы: %s
                                Пытаемся найти в другом месте
                            """,
              dbName, query, queries.keySet()));
      return processValueWithoutProperty(query);
    } else {
      String queryScript = queries.get(query);
      try {
        return resolveVariablesWithoutProperty(queryScript);
      } catch (IllegalArgumentException ex) {
        throw new DataBaseExceptions(
            "Нет переменных, необходимых для БД запроса: %s!\n" + "Требуемы переменные: %s",
            queryScript, getQueryParameters(queryScript));
      }
    }
  }

  /**
   * Получает список всех требуемых запросу параметров.
   *
   * @param query текст запроса.
   * @return возвращает строку, в которой перечислены все необходимые запросу параметры.
   */
  private static String getQueryParameters(String query) {
    StringBuilder result = new StringBuilder();
    Pattern p = Pattern.compile("\\$\\{(.+?)}");
    Matcher matcher = p.matcher(query);
    while (matcher.find()) {
      String varName = matcher.group(1);
      result.append(varName).append(", ");
      String newString = matcher.replaceFirst(varName);
      matcher = p.matcher(newString);
    }
    result.replace(result.length() - 2, result.length(), "");
    return result.toString();
  }

  /**
   * Выполняет запрос с параметрами к БД, конфигурация которой указана в dbName. Если сценарий не
   * содержит переменной с конфигурацией для подключения к БД, то пробует создать её из файла .conf.
   *
   * @param dbName имя переменой сценария, содержащей конфигурацию для БД.
   * @param query запрос в БД. запросе
   * @return возвращает количество обновлённых(удалённых) записей.
   */
  public static int updateQueryInDatabase(String dbName, String query) {
    // Получаем сохраненный DataSource объект. Если его нет - создаём новый
    DataSource dataSource =
        Objects.requireNonNullElseGet(
            (DataSource) akitaScenario.tryGetVar(dbName), () -> createDbConnection(dbName));

    String queryValue = getQuery(dbName, query);
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    return jdbcTemplate.update(queryValue);
  }

  public static List<Map<String, Object>> query(DataSource dataSource, String query) {
    try {
      return new JdbcTemplate(dataSource).queryForList(query);
    } catch (EmptyResultDataAccessException | DataIntegrityViolationException ignore) {
      akitaScenario.log(String.format("Запрос %s ничего не вернул.", query));
    }
    return null;
  }

  public static List<Map<String, Object>> query(String dbName, String query) {
    // Получаем сохраненный DataSource объект. Если его нет - создаём новый
    DataSource dataSource =
        Objects.requireNonNullElseGet(
            (DataSource) akitaScenario.tryGetVar(dbName), () -> createDbConnection(dbName));
    try {
      return new JdbcTemplate(dataSource).queryForList(query);
    } catch (EmptyResultDataAccessException | DataIntegrityViolationException ignore) {
      akitaScenario.log(String.format("Запрос %s ничего не вернул.", query));
    }
    return Collections.emptyList();
  }
}
