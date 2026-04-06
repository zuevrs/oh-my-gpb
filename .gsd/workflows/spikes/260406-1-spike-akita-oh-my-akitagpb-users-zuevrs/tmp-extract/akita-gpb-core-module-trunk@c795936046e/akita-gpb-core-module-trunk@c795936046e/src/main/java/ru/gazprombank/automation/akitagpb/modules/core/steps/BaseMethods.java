package ru.gazprombank.automation.akitagpb.modules.core.steps;

import static java.util.Objects.isNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.api.AkitaScenario;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.PropertyLoader;

/** Общие методы, используемые в различных шагах */
@Slf4j
public class BaseMethods {

  public static final int DEFAULT_TIMEOUT =
      PropertyLoader.loadPropertyInt("waitingCustomElementsTimeout", 10000);
  private static BaseMethods instance = new BaseMethods();
  protected AkitaScenario akitaScenario = AkitaScenario.getInstance();

  //    protected static final String SPECS_DIR_PATH = PropertyLoader.loadSystemPropertyOrDefault(
  //        "specsDir", System.getProperty("user.dir") + "/src/test/resources/specs/");
  //    protected static final String IMG_DIFF_PATH = PropertyLoader.loadSystemPropertyOrDefault(
  //        "imgDiff", System.getProperty("user.dir") + "/build/results-img/");

  public static BaseMethods getInstance() {
    BaseMethods localInstance = instance;
    if (localInstance == null) {
      synchronized (BaseMethods.class) {
        localInstance = instance;
        if (localInstance == null) {
          instance = localInstance = new BaseMethods();
        }
      }
    }
    return localInstance;
  }

  public static String replaceVarsInStringByParams(String string, Map<String, String> params) {
    final String[] result = {string};
    params.forEach(
        (key, value) -> {
          String pattern = "\\$\\{" + key + "}";
          String replacedValue = getPropertyOrStringVariableOrValueAndReplace(value);
          result[0] = result[0].replaceAll(pattern, replacedValue);
        });
    return result[0];
  }

  public static String getPropertyOrStringVariableOrValueAndReplace(String string) {
    if (string != null && string.matches("^\\$\\{.+}$"))
      string =
          BaseMethods.getInstance()
              .getPropertyOrStringVariableOrValue(string.replaceAll("\\$\\{(.+)}", "$1"));
    if (string != null && string.matches("^.*\\$\\{.+}.*$")) {
      Pattern pattern = Pattern.compile("\\$\\{[\\w.]+}");
      Matcher mt = pattern.matcher(string);
      int breaker = 0;
      String resultString = string;
      while (mt.find() && breaker != 100) {
        breaker++;
        String group = mt.group();
        String v =
            BaseMethods.getInstance()
                .getPropertyOrStringVariableOrValue(group.replaceAll("\\$\\{(.+)}", "$1"));
        resultString = resultString.replace(group, v);
      }
      string = resultString;
    }
    return string;
  }

  public static String replaceVarsInString(String string, Map<String, String> params) {
    final String[] result = {string};
    params.forEach(
        (key, value) -> {
          String pattern = "\\$\\{" + key + "}";
          String replacedValue = getPropertyOrStringVariableOrValueAndReplace(value);
          result[0] = result[0].replaceAll(pattern, replacedValue);
        });
    return result[0];
  }

  /**
   * Прикрепляет файл к текущему сценарию в cucumber отчете
   *
   * @param fileName - название файла
   * @param mimeType - тип файла
   */
  @SneakyThrows
  public static void embedFileToReport(File fileName, String mimeType) {
    AkitaScenario.getInstance()
        .getScenario()
        .attach(FileUtils.readFileToByteArray(fileName), mimeType, String.valueOf(fileName));
  }

  /** Записывает файл в директорию */
  @SneakyThrows
  public static void writeFileToDirectory(byte[] bytes, String directory) {
    try (var stream = new FileOutputStream(directory)) {
      stream.write(bytes);
    }
  }

  /**
   * Возвращает значение из property файла, если отсутствует, то из пользовательских переменных,
   * если и оно отсутствует, то из conf файла, если и там ничего нету, то возвращает значение
   * переданной на вход переменной
   *
   * @return
   */
  public String getPropertyOrStringVariableOrValue(
      String propertyNameOrVariableNameOrConfigNameValue) {

    boolean checkSecureFlag = true;
    if (propertyNameOrVariableNameOrConfigNameValue.toLowerCase().contains("login")
        || propertyNameOrVariableNameOrConfigNameValue.toLowerCase().contains("password")) {
      checkSecureFlag = false;
    }

    String variableValue =
        (String) akitaScenario.tryGetVar(propertyNameOrVariableNameOrConfigNameValue);
    if (checkResult(
        variableValue,
        "Переменная сценария " + propertyNameOrVariableNameOrConfigNameValue,
        checkSecureFlag)) {
      return variableValue;
    }

    String propertyValue =
        PropertyLoader.tryLoadProperty(propertyNameOrVariableNameOrConfigNameValue);
    if (checkResult(
        propertyValue,
        "Переменная " + propertyNameOrVariableNameOrConfigNameValue + " из property файла",
        checkSecureFlag)) {
      return propertyValue;
    }

    String configValue = ConfigLoader.getConfigValue(propertyNameOrVariableNameOrConfigNameValue);
    if (checkResult(
        configValue,
        "Переменная конфигурации " + propertyNameOrVariableNameOrConfigNameValue,
        checkSecureFlag)) {
      return configValue;
    }

    return propertyNameOrVariableNameOrConfigNameValue;
  }

  private boolean checkResult(String result, String message, boolean checkSecure) {
    if (isNull(result)) {
      log.debug(message + " не найдена");
      return false;
    }
    if (checkSecure) {
      akitaScenario.log(message + " = " + result);
    } else {
      akitaScenario.log(message + " = " + "SENSITIVE DATA");
    }
    return true;
  }

  /**
   * Возвращает каталог "Downloads" в домашней директории
   *
   * @return
   */
  public File getDownloadsDir() {
    String homeDir = System.getProperty("user.home");
    return new File(homeDir + "/Downloads");
  }

  /**
   * Удаляет файлы, переданные в метод
   *
   * @param filesToDelete массив файлов
   */
  public void deleteFiles(File[] filesToDelete) {
    for (File file : filesToDelete) {
      file.delete();
    }
  }

  /**
   * Возвращает случайное число от нуля до maxValueInRange
   *
   * @param maxValueInRange максимальная граница диапазона генерации случайных чисел
   */
  public int getRandom(int maxValueInRange) {
    return (int) (Math.random() * maxValueInRange);
  }

  /**
   * Возвращает последовательность случайных символов переданных алфавита и длины Принимает на вход
   * варианты языков 'ru' и 'en' Для других входных параметров возвращает латинские символы (en)
   */
  public String getRandCharSequence(int length, String lang) {

    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < length; i++) {
      char symbol = charGenerator(lang);
      builder.append(symbol);
    }
    return builder.toString();
  }

  /** Возвращает случайный символ переданного алфавита */
  public char charGenerator(String lang) {
    Random random = new Random();
    if (lang.equals("ru")) {
      return (char) (1072 + random.nextInt(32));
    } else {
      return (char) (97 + random.nextInt(26));
    }
  }

  /** Проверка на соответствие строки паттерну */
  public boolean isTextMatches(String str, String pattern) {
    Pattern r = Pattern.compile(pattern);
    Matcher m = r.matcher(str);
    return m.matches();
  }

  /** Выдергиваем число из строки */
  public int getCounterFromString(String variableName) {
    return Integer.parseInt(variableName.replaceAll("[^0-9]", ""));
  }
}
