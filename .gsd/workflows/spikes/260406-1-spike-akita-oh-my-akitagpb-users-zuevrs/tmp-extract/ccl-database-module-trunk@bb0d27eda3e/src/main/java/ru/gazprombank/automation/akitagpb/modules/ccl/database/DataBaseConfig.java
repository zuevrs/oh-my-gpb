package ru.gazprombank.automation.akitagpb.modules.ccl.database;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.FileHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;

/**
 * Класс содержащий в себе параметры подключения к БД. Параметры должны браться из .conf файлов
 * проекта.
 */
@Setter
@Getter
public class DataBaseConfig {

  private static final Logger logger = LoggerFactory.getLogger(DataBaseConfig.class);

  private String driver;
  private String url;
  private String username;
  private String password;
  private String queryFilePath;
  private HashMap<String, String> queries = new HashMap<>();

  public DataBaseConfig(Map<String, Object> config) {
    driver = (String) config.get("driver");
    url = (String) config.get("url");
    username = processValue((String) config.get("username"));
    password = processValue((String) config.get("password"));
    queryFilePath = (String) config.get("queryFilePath");
    if (queryFilePath != null && !queryFilePath.isEmpty()) {
      parseSqlFile();
    }
  }

  private void parseSqlFile() {
    String queriesFile = resolveVariables(queryFilePath);
    var resources = StringHelper.class.getResourceAsStream("/queries/" + queriesFile);

    if (resources == null)
      throw new DataBaseExceptions(
          "Не нашли файл с запросами в БД в ресурсах по пути: /queries/%s", queriesFile);

    String queriesText = FileHelper.getFileContent(resources);
    String regex = "-- ([\\S\\s]+?)\\n([\\S\\s]+?);";
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(queriesText);
    while (m.find()) {
      String key = m.group(1).replaceAll("\\r", "");
      String body = m.group(2).strip();
      if (queries.containsKey(key)) {
        logger.warn(
            "В списке запросов в БД уже содержится запрос с именем {}. Требуется изменить имя запроса.",
            key);
      } else {
        queries.put(key, body);
      }
    }
    if (queries.isEmpty()) {
      throw new DataBaseExceptions(
          "В файле с запросами \"/queries/%s\" не найден ни один запрос! Проверьте правльность заполнения файла!",
          queriesFile);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "Driver: %s\nURL: %s\nUsername: %s\nPassword: %s",
        this.driver, this.url, this.username, this.password);
  }
}
