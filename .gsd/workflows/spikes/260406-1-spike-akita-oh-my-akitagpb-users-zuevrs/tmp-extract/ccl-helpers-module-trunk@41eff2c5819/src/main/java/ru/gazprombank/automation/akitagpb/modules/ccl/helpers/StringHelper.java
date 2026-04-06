package ru.gazprombank.automation.akitagpb.modules.ccl.helpers;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.FileHelper.getResourcesFileContent;
import static ru.gazprombank.automation.akitagpb.modules.core.steps.isteps.IBaseMethods.akitaScenario;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.ibm.icu.text.RuleBasedNumberFormat;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.generator.DataGenerationHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.loggers.XmlErrorHandler;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.PropertyLoader;

/** Утилитный класс для работы со строками */
public class StringHelper {

  public static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
          .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true))
          .enable(Feature.WRITE_BIGDECIMAL_AS_PLAIN);
  private static final Logger LOGGER = LoggerFactory.getLogger(StringHelper.class);
  private static final String ALPHABET_CYRILLIC =
      "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";
  private static final String ALPHABET_LATIN =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  private static final Map<String, String> systemWords =
      System.getProperty("os.name").toLowerCase().contains("windows")
          ? ConfigLoader.getConfigValue("systemWords.windows")
          : ConfigLoader.getConfigValue("systemWords.linux");

  /**
   * Метод определяет, является ли переданная строка json'ом.
   *
   * @param stringValue строка для проверки
   * @return true - если переданная строка является json'ом, иначе - false
   */
  public static boolean isJsonStringValid(String stringValue) {
    try {
      new JSONObject(stringValue);
    } catch (JSONException e) {
      try {
        new JSONArray(stringValue);
      } catch (JSONException ex) {
        return false;
      }
    }
    return true;
  }

  /**
   * Метод определяет, является ли переданная строка xml'ом.
   *
   * @param stringValue строка для проверки
   * @return true - если переданная строка является xml'ом, иначе - false
   */
  public static boolean isXmlStringValid(String stringValue) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setErrorHandler(new XmlErrorHandler());
      builder.parse(new ByteArrayInputStream(stringValue.getBytes(StandardCharsets.UTF_8)));
      return true;
    } catch (SAXException | ParserConfigurationException | IOException e) {
      return false;
    }
  }

  /**
   * Метод определяет, является ли переданная строка UUID.
   *
   * @param stringValue строка для проверки
   * @return true - если переданная строка является UUID, иначе - false
   */
  public static boolean isUUIDStringValid(String stringValue) {
    try {
      UUID.fromString(stringValue);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Метод для получения из класссов, не наследующихся от BaseMethods, переменной сценария/значения
   * property/значения конфига/строки, парметризованной переменными сценария в конструкциях
   * ${varKey}/или самому значению строки, если предыдущие опции не найдены.
   *
   * @param variableKey название переменной сценария/property key/config key/параметризованная
   *     строка/или просто строка
   * @return значение переменной сценария/значение property/значение конфига/параметризованная
   *     строка/само значение переданной строки или null, если переданный параметр равен null
   */
  public static String getVariableOrValue(String variableKey) {
    return variableKey == null ? null : processValue(variableKey);
  }

  /**
   * Метод добавляет после значения @value ".0", для работы с переменными Camunda
   *
   * @param value значение даты.
   * @return возвращает полученное значение в виде строки.
   */
  public static String appendDotZero(String value) {
    return value + ".0";
  }

  /**
   * Метод для получения значения из строки с помощью регулярного выражения по номеру группы в этом
   * выражении.
   *
   * @param source исходная строка
   * @param regexpPattern регулярное выражение для поиска в строке
   * @param groupIndex номер группы в регулярном выражении
   * @return возвращает найденное значение или null
   */
  public static String getRegexpGroupValue(String source, String regexpPattern, int groupIndex) {
    Pattern pattern = Pattern.compile(regexpPattern);
    Matcher matcher = pattern.matcher(source);
    if (matcher.find()) {
      return matcher.group(groupIndex);
    }
    return null;
  }

  /**
   * Получить рандомную строку заданной длины
   *
   * @param length длина строки (если null, то будет равна 10)
   * @param isLatin true (или null), если нужна строка из латинских символов, false - из
   *     кириллических
   * @param isNumeric true, если нужна строка из цифр, false (или null) - из букв
   * @return рандомная строка по заданным параметрам
   */
  public static String getRandomString(String length, Boolean isLatin, Boolean isNumeric) {
    int lengthInt = length == null ? 10 : Integer.parseInt(length);
    return isNumeric != null && isNumeric
        ? getRandomNumericString(lengthInt)
        : isLatin == null || isLatin
            ? getRandomLatinString(lengthInt)
            : getRandomCyrillicString(lengthInt);
  }

  /**
   * Получить рандомную строку заданной длины из латинских символов
   *
   * @param length длина строки
   * @return рандомная строка заданной длины из латинских символов
   */
  public static String getRandomLatinString(int length) {
    return RandomStringUtils.random(length, ALPHABET_LATIN);
  }

  /**
   * Получить рандомную строку заданной длины из кириллических символов
   *
   * @param length длина строки
   * @return рандомная строка заданной длины из кириллических символов
   */
  public static String getRandomCyrillicString(int length) {
    return RandomStringUtils.random(length, ALPHABET_CYRILLIC);
  }

  /**
   * Получить рандомную строку заданной длины из цифр
   *
   * @param length длина строки
   * @return рандомная строка из цифр заданной длины
   */
  public static String getRandomNumericString(int length) {
    var number = RandomStringUtils.randomNumeric(length);
    return number.startsWith("0")
        ? number.replaceFirst("0", DataGenerationHelper.getRandomNumber("1", "10").toString())
        : number;
  }

  /**
   * Метод для приведения относительного пути файла к "общему" виду. Если разделитель пути -
   * обратный слеш "\" - метод заменит его на "/". Если путь начинается со слеша "/" - метод уберёт
   * этот первый слеш из пути.
   *
   * @param path относительный путь файла
   * @return "нормализованнй" путь
   */
  public static String normalizeRelativeFilePath(String path) {
    path = path.replace("\\\\", "/");
    return path.startsWith("/") ? path.replaceFirst("/", "") : path;
  }

  /**
   * Метод для приведения объекта к виду json строки. Если объект изначально является строкой, то
   * возвращаем её
   *
   * @param object объект для преобразования к json строке
   * @return преобразованный в строку json
   * @throws JsonProcessingException исключение при преобразовании
   */
  public static String objectToJsonString(Object object) throws JsonProcessingException {
    if (object instanceof String) {
      return (String) object;
    }
    ObjectWriter objectWriter = new ObjectMapper().writer();
    return objectWriter.writeValueAsString(object);
  }

  /**
   * Метод обрабатывает значение, меняя имена переменных на значения и системные слова из конфига на
   * значения. Ищем значения в переменных, конфигах и свойствах проекта.
   *
   * @param value исходная значение
   * @return преобразованная строка
   */
  public static String processValue(String value) {
    // Преобразуем переменную в её значение
    String result = getPropertyOrVariableOrConfigValueOrValue(resolveVariables(value));
    // Заменяем системные слова на значения
    for (Map.Entry<String, String> entry : systemWords.entrySet()) {
      result = result.replaceAll(String.format("\\[%s\\]", entry.getKey()), entry.getValue());
    }
    // Выводим итоговый результат в лог, если это не просто обычная непараметризованная строка
    if (!result.equals(value)) {
      LOGGER.debug(String.format("%nЗначение %s: %s", value, result));
    }
    return result;
  }

  /**
   * Метод пробует получить значение файла из ресурсов или по пути. Если получается возврщает
   * содержимое файла. Если возникает ошибка, то возвращается исходное значение.
   *
   * @param value предполагаемый путь для поиска файла.
   * @return String содержимого файла или исходное значение.
   */
  public static String getFileContentOrValue(String value) {
    try {
      return getResourcesFileContent(value);
    } catch (RuntimeException ignore) {
      return value;
    }
  }

  /**
   * Метод обрабатывает значение, меняя имена переменных на значения и системные слова из конфига на
   * значения. Ищем значения в переменных, конфигах.
   *
   * @param value исходная значение
   * @return преобразованная строка
   */
  public static String processValueWithoutProperty(String value) {
    // Преобразуем переменную в её значение
    String result = getVariableOrConfigValueOrValue(resolveVariablesWithoutProperty(value));
    // Заменяем системные слова на значения
    for (Map.Entry<String, String> entry : systemWords.entrySet()) {
      result = result.replaceAll(String.format("\\[%s\\]", entry.getKey()), entry.getValue());
    }
    // Выводим итоговый результат в лог, если это не просто обычная непараметризованная строка
    if (!result.equals(value)) {
      LOGGER.debug(String.format("%nЗначение %s: %s", value, result));
    }
    return result;
  }

  /**
   * Ищем значение по имени в переменных, конфигах и свойствах проекта. Если нигде не нашли -
   * возвращаем имя переменной.
   *
   * @param valueName имя переменной или свойства или конфига.
   * @return значение переменной или конфига или свойства или имя переменной в формате String.
   */
  private static String getPropertyOrVariableOrConfigValueOrValue(String valueName) {
    try {
      return getPropertyOrVariableOrConfigValue(valueName);
    } catch (IllegalArgumentException ex) {
      LOGGER.trace("\n" + ex.getMessage());
      return valueName;
    }
  }

  /**
   * Ищем значение по имени в переменных или конфигах. Если нигде не нашли - возвращаем имя
   * переменной.
   *
   * @param valueName имя переменной или свойства или конфига.
   * @return значение переменной или конфига или свойства или имя переменной в формате String.
   */
  private static String getVariableOrConfigValueOrValue(String valueName) {
    try {
      return getVariableOrConfigValue(valueName);
    } catch (IllegalArgumentException ex) {
      LOGGER.trace("\n" + ex.getMessage());
      return valueName;
    }
  }

  /**
   * Ищем значение по имени в переменных, конфигах и свойствах проекта. Если нигде не нашли -
   * возвращаем ошибку.
   *
   * @param valueName имя переменной или свойства или конфига.
   * @return значение переменной или конфига или свойства в формате String.
   */
  private static String getPropertyOrVariableOrConfigValue(String valueName) {
    Object result = akitaScenario.tryGetVar(valueName);
    if (result != null) {
      return result.toString();
    }

    result = PropertyLoader.tryLoadProperty(valueName);
    if (result != null) {
      return result.toString();
    }

    result = ConfigLoader.getConfigValue(valueName);
    if (result != null) {
      return result.toString();
    }

    throw new IllegalArgumentException(
        "Значения для имени \""
            + valueName
            + "\" не были найдены ни в Properties, ни в Config, ни в environment переменных");
  }

  /**
   * Ищем значение по имени в переменных или конфигах проекта. Если нигде не нашли - возвращаем
   * ошибку.
   *
   * @param valueName имя переменной или свойства или конфига.
   * @return значение переменной или конфига или свойства в формате String.
   */
  private static String getVariableOrConfigValue(String valueName) {
    Object result = akitaScenario.tryGetVar(valueName);
    if (result != null) {
      return result.toString();
    }

    result = ConfigLoader.getConfigValue(valueName);
    if (result != null) {
      return result.toString();
    }

    throw new IllegalArgumentException(
        "Значения для имени \""
            + valueName
            + "\" не были найдены ни в Properties, ни в Config, ни в environment переменных");
  }

  /**
   * Заменить в переданной строке все переменные формата "${var}" на значения данных переменных из
   * сценария, конфига или параметров. В отличие от ScopedVariables.resolveVars, в данном методе
   * переменные "${var}" не должны быть строками.
   *
   * @param parametrized обрабатываемая строка
   * @return строка с заменёнными переменными
   */
  public static String resolveVariables(String parametrized) {
    while (parametrized.replaceAll("\\n", "").replaceAll("\\r", "").matches(".*\\$\\{.+}.*")) {
      var varName = getRegexpGroupValue(parametrized, "\\$\\{(.+?)\\}", 1);
      parametrized =
          parametrized.replace("${" + varName + "}", getPropertyOrVariableOrConfigValue(varName));
    }
    return parametrized;
  }

  /**
   * Заменить в переданной строке все переменные формата "${var}" на значения данных переменных из
   * сценария, конфига или параметров. В отличие от ScopedVariables.resolveVars, в данном методе
   * переменные "${var}" не должны быть строками.
   *
   * @param parametrized обрабатываемая строка
   * @return строка с заменёнными переменными
   */
  public static String resolveVariablesWithoutProperty(String parametrized) {
    while (parametrized.replaceAll("\\n", "").replaceAll("\\r", "").matches(".*\\$\\{.+}.*")) {
      var varName = getRegexpGroupValue(parametrized, "\\$\\{(.+?)\\}", 1);
      parametrized = parametrized.replace("${" + varName + "}", getVariableOrConfigValue(varName));
    }
    return parametrized;
  }

  /**
   * Метод возвращает значение Json элемента в виде строки - то есть так, как указано в самом Json,
   * без преобразования на стороне Java.
   */
  public static String getStringValue(Object rawValue) throws JsonProcessingException {
    return rawValue.getClass() == String.class
        ? rawValue.toString()
        : OBJECT_MAPPER.writeValueAsString(rawValue);
  }

  /** Метод возвращает имя переменной без специальных символов. */
  public static String getVarName(String var) {
    String varName =
        getRegexpGroupValue(var, "\\$[{]([\\wа-яА-Я]+[\\wа-яА-Я.-]*[\\wа-яА-Я]+)[}]", 1);
    return varName == null ? var : varName;
  }

  /**
   * Метод для написания числа прописью в указанном формате (формат из библиотеки icu4j). Пример:
   * 11.6 в формате "%spellout-cardinal-masculine" - одиннадцать целых шесть десятых 1 в формате
   * "%spellout-ordinal-neuter-genitive" - Первого
   *
   * @param number число, которое нужно записать прописью
   * @param format формат из библиотеки icu4j. Чтобы узнать список доступных форматов - можно
   *     вызвать метод {@link #spellOutNumberWithAllFormats(BigDecimal)}
   * @param isCapitalizedOrCaps отформатировать полученную запись: "с большой буквы", "в верхнем
   *     регистре" или null, если такое форматирование не нужно
   * @return число прописью
   */
  public static String spellOutNumber(
      BigDecimal number, String format, String isCapitalizedOrCaps) {
    RuleBasedNumberFormat nf =
        new RuleBasedNumberFormat(Locale.forLanguageTag("ru"), RuleBasedNumberFormat.SPELLOUT);
    nf.setDefaultRuleSet(format);
    var result = nf.format(number, new StringBuffer(), null).toString();
    if (isCapitalizedOrCaps != null) {
      result =
          isCapitalizedOrCaps.contains("с большой буквы")
              ? StringUtils.capitalize(result)
              : result.toUpperCase();
    }
    return result;
  }

  /**
   * Метод вернёт название формата (из библиотеки icu4j) и соответствующую ему запись числа
   * прописью. Нужен в качестве подсказки для определения необходимого формата.
   *
   * @param number число, которое нужно записать прописью
   * @return число прописью во всех доступных форматах
   */
  public static String spellOutNumberWithAllFormats(BigDecimal number) {
    RuleBasedNumberFormat nf =
        new RuleBasedNumberFormat(Locale.forLanguageTag("ru"), RuleBasedNumberFormat.SPELLOUT);
    var stringBuffer = new StringBuffer("SPELLOUT");
    for (String ruleSetName : nf.getRuleSetNames()) {
      nf.setDefaultRuleSet(ruleSetName);
      stringBuffer.append(System.lineSeparator()).append(ruleSetName).append(" --> ");
      nf.format(number, stringBuffer, null);
    }
    return stringBuffer.toString();
  }

  /**
   * Метод для написания суммы в рублях прописью. Пример: 12345.67 - Двенадцать тысяч триста сорок
   * пять рублей 67 копеек 500000 - Пятьсот тысяч рублей 00 копеек
   *
   * @param amount сумма
   * @param isCapitalizedOrCaps отформатировать полученную запись: "с большой буквы", "в верхнем
   *     регистре" или null, если такое форматирование не нужно
   * @return сумма в рублях прописью
   */
  public static String spellOutCurrencyAmountRub(BigDecimal amount, String isCapitalizedOrCaps) {
    var ruleSet =
        FileHelper.getFileContent(
            StringHelper.class.getResourceAsStream("/numberformatrules/rubles-and-cents.txt"));
    RuleBasedNumberFormat nf = new RuleBasedNumberFormat(ruleSet, Locale.forLanguageTag("ru"));
    nf.setDefaultRuleSet("%spellout-rubles-and-cents");
    var result = nf.format(amount, new StringBuffer(), null).toString();
    if (isCapitalizedOrCaps != null) {
      result =
          isCapitalizedOrCaps.contains("с большой буквы")
              ? StringUtils.capitalize(result)
              : result.toUpperCase();
    }
    return result;
  }

  /**
   * Метод для написания процентов прописью. Пример: 2.00 - две целых ноль десятых процента 1.10 -
   * одна целая одна десятая процента 0.25 - ноль целых двадцать пять сотых процента
   *
   * @param percentNumber число процентов
   * @param isCapitalizedOrCaps отформатировать полученную запись: "с большой буквы", "в верхнем
   *     регистре" или null, если такое форматирование не нужно
   * @return проценты прописью
   */
  public static String spellOutPercents(BigDecimal percentNumber, String isCapitalizedOrCaps) {
    var ruleSet =
        FileHelper.getFileContent(
            StringHelper.class.getResourceAsStream("/numberformatrules/percents.txt"));
    RuleBasedNumberFormat nf = new RuleBasedNumberFormat(ruleSet, Locale.forLanguageTag("ru"));
    nf.setDefaultRuleSet("%spellout-percents");
    var result = nf.format(percentNumber, new StringBuffer(), null).toString();
    if (isCapitalizedOrCaps != null) {
      result =
          isCapitalizedOrCaps.contains("с большой буквы")
              ? StringUtils.capitalize(result)
              : result.toUpperCase();
    }
    return result;
  }

  /**
   * Получить последние N частей строки при разделении её заданным разделителем (например, последние
   * 5 строк многострочного текста)
   *
   * @param source исходная строка
   * @param partsNumber число последних частей
   * @param delimiter разделитель
   * @return последние partsNumber частей строки
   */
  public static String getLastParts(String source, int partsNumber, String delimiter) {
    var array = source.split(delimiter);
    return array.length <= partsNumber
        ? source
        : Arrays.stream(array)
            .skip(array.length - partsNumber)
            .collect(Collectors.joining(delimiter));
  }

  /**
   * Стандартный парсер из текста в Boolean. Если значение yes, да или true (без учёта регистра), то
   * возвращает true. Иначе - false.
   *
   * @param value текстовое значение.
   * @return Boolean эквивадент текстового значения.
   */
  public static Boolean getBooleanFromString(String value) {
    if (value == null) {
      return false;
    }
    return switch (value.strip().toLowerCase()) {
      case "yes", "да", "true" -> true;
      default -> false;
    };
  }

  public static String getStackTrace(Throwable exception) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    exception.printStackTrace(pw);
    return sw.toString();
  }

  /**
   * Метод определяет содержит ли входящая строка символы кирилицы
   *
   * @param text входящая строка
   * @return boolean значение
   */
  public static boolean isContainsCyrillicChar(String text) {
    for (int i = 0; i < text.length(); i++) {
      if (Character.UnicodeBlock.of(text.charAt(i)).equals(Character.UnicodeBlock.CYRILLIC)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Преобразует List строк в String согласно формату.
   *
   * @param values List строк
   * @param format формат преобразования, где первый и последний символы формата - префикс и суфикс
   *     строки, второй с конца сивол - разделитель, а - меняется на значение из List, остальное не
   *     меняется
   * @return String в правильном формате
   */
  public static String prepareStringFromList(List<String> values, String format) {
    String first = format.substring(0, 1);
    String last = format.substring(format.length() - 1);
    String delimiter = format.substring(format.length() - 2, format.length() - 1);
    String valueTemplate = format.substring(1, format.length() - 2);
    return values.stream()
        .map(value -> valueTemplate.replace("a", value))
        .collect(Collectors.joining(delimiter, first, last));
  }
}
