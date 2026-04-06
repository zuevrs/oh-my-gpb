package ru.gazprombank.automation.akitagpb.modules.ccl.additional.steps;

import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.date.DateCalculator.calculateDate;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.date.DateCalculator.getDifferenceBetweenDates;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.generator.DataGenerationHelper.generateVariable;

import io.cucumber.java.ru.И;
import io.restassured.internal.support.Prettifier;
import io.restassured.response.Response;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.NumberHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.assertions.MainAssert;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels.DateAssertableConditionParam;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels.DateCalculatorParam;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels.StringAssertableConditionParam;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.date.DateHelper;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

/** Класс для описания общих шагов Cucumber. */
public class CommonSteps extends BaseMethods {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommonSteps.class);

  /**
   * Сохраняем переданное значение в переменную сценария для дальнейшего использования. Сохраняется
   * именно текст - значение не преобразовывается никаким образом.
   *
   * @param value значение, которое надо сохранить.
   * @param varName имя переменной сценария.
   */
  @И("^значение \"(.*)\" сохраняем в переменную сценария \"(.*)\"$")
  public void saveStringValue(String value, String varName) {
    akitaScenario.setVar(varName, value);
  }

  /**
   * Сохраняем в переменные сценария значения, переданные в таблице параметров. Значением может быть
   * как просто текст, так и "шаблон" для генерации необходимого значения. Шаблоны: текущая дата в
   * формате "***" -> дата в указанном формате (формат должен быть задан в кавычках); текущая дата
   * МСК в формате "***" -> дата в Московском часовом поясе в указанном формате (формат должен быть
   * задан в кавычках); текущий месяц (1-12) -> порядковый номер текущего месяца; текущий месяц
   * (01-12) -> номер текущего месяца, дополненный нулём (формат "MM"); текущий год -> номер
   * текущего года (формат "yyyy"); текущий период для заявки ЭБГ (1-4) -> число от 1 до 4 в
   * зависимости от номера месяца (для значения period заявки ЭБГ); рандомный UUID ->
   * сгененированный UUID; случайный ИНН ЮЛ -> сгененированный ИНН; текущее время в миллисекундах ->
   * число типа long; случайное число -> сгенерированное число, от 0 до 100000; случайное число от *
   * -> сгенерированное число с нижним пределом (верхний предел +100000); случайное число до * ->
   * сгенерированное число с верхним пределом (нижний предел = 0); случайное число от * до * ->
   * сгенерированное число в указанных пределах; случайное число длиной * -> сгенерированное число
   * длиной * цифр; случайная строка длиной * -> сгенерированная строка длиной * из латинских
   * символов; случайная кириллическая строка длиной * -> сгенерированная строка длиной * из
   * кириллических символов; случайная строка из цифр длиной * -> сгенерированное число длиной *
   * цифр (альтернатива "случайное число длиной *"); содержимое файла ресурсов "***" -> текст из
   * заданного файла ресурсов (имя/путь файла должен быть задан в кавычках). При этом переменные
   * вида ${name} из этого текста будут параметризованы, если эти переменные были сохранены заранее.
   * Файл ищется и в main/resources, и в test/resources, поэтому можно передавать только имя файла -
   * если оно уникально в пределах всех ресурсов, или уточнять путь к файлу директориями - если
   * файлов с одинаковым именем несколько (например, "first_path/order3.json").
   *
   * <p>Также, шаблоны и просто текст можно комбинировать, например: "текущий месяц
   * (1-12)_12345_текущий год" -> 10_12345_2022
   *
   * @param dataTable таблица переменных и значений, которые будут в них сохранены.
   */
  @И("^сохраняем в переменные сценария значения из таблицы:?$")
  public void saveGeneratedValuesIntoVars(Map<String, String> dataTable) {
    dataTable.forEach(
        (key, value) -> {
          String resultValue = generateVariable(value);
          akitaScenario.setVar(key, resultValue);
          akitaScenario.log(String.format("Значение %s: %s", key, resultValue));
        });
  }

  /**
   * Применяет к строке, содержащейся в переменной сценария указанное регулярное выражение и
   * сохраняет первый найденный результат в новую переменную.
   *
   * @param varName имя переменной сценария
   * @param regexp регулярное выражение
   * @param newVarName имя новой переменной
   */
  @И(
      "^к переменной \"(.+)\" применить регулярное выражение \"(.+)\" и сохранить результат в переменную \"(.+)\"$")
  public void doRegExp(String varName, String regexp, String newVarName) {
    String value = String.valueOf(akitaScenario.getVar(varName));
    Pattern pattern = Pattern.compile(regexp);
    Matcher matcher = pattern.matcher(value);
    if (matcher.find()) {
      String result = matcher.group();
      akitaScenario.log(
          String.format(
              "В строке \"%s\" найдено совпадение для регулярного выражения \"%s\": %s",
              value, regexp, result));
      akitaScenario.setVar(newVarName, result);
    } else {
      fail(
          String.format(
              "Не найдено совпадений в строке \"%s\" для регулярного выражения \"%s\".",
              value, regexp));
    }
  }

  /**
   * Шаг-калькулятор дат. С помощью него можно: - изменить формат даты - убавить от даты/прибавить к
   * дате указанный интервал времени (годы - y (Y), месяцы - M, дни - d (D), часы - h (H), минуты -
   * m, секунды - s, наносекунды - S) - получить разницу (Действие = минус) между двумя датами в
   * указанных единицах времени (годах, месяцах, днях, часах, минутах, секундах, наносекундах) -
   * установить для исходной даты значение каких-либо единиц времени (первый/последний день месяца,
   * установить минуты на 0 и т.д.) - получить более раннюю / более позднюю дату из двух
   *
   * <p>Пример: И вычислить даты, сохранив результат в переменные из таблицы | Значение 1 | Действие
   * | Значение 2 | Формат | Переменная | | ${date1} | | | d | dayOfMonth | | ${date1} | минус | 1y
   * 2M 6d 4m 5s 6000S | yyyy-MM-dd HH:mm:ss.SSS | newDate1 | | ${date1} | плюс | 1y 2M 3d 4m 5s
   * 6000S | yyyy-MM-dd'T'HH:mm:ss | newDate2 | | ${date2} | минус | ${date1} | s | secondsDiff | |
   * ${newDate1} | минус | ${date1} | d | daysDiff | | ${date1} | установить | 0H 0m 0s 0S |
   * yyyy-MM-dd HH:mm:ss.SSS | newDate4 | | ${date1} | установить | первый день месяца | dd.MM.yyyy
   * | newDate5 | | ${date1} | установить | последний день месяца | dd.MM.yyyy | newDate6 | |
   * ${date1} | ранняя из | ${newDate2} | dd.MM.yyyy HH:mm:ss | earlierDate | | ${date1} | поздняя
   * из | ${newDate2} | dd.MM.yyyy HH:mm:ss | latestDate | | ${date1} | плюс рабочих дней | 6d |
   * dd.MM.yyyy HH:mm:ss | latestDate |
   *
   * <p>Формат - это паттерн выходного значения, которое будет сохранено в переменные. Сами даты из
   * Значения 1/Значения 2 парсятся по паттернам, заданным в конфиг-файле в опции
   * date.comparison.patterns, и в шаге эти паттерны дат передавать не надо.
   *
   * @param params список параметров шага
   */
  @И("^вычислить даты, сохранив результат в переменные из таблицы:?$")
  public void calculateDates(List<DateCalculatorParam> params) {
    params.forEach(
        e -> {
          String result;
          var originDate = DateHelper.parseDateTime(processValue(e.getOriginDate()));
          Predicate<DateCalculatorParam> predicate =
              p ->
                  p.getDiffValue() == null
                      || p.getDiffValue().getClass() == HashMap.class
                      || p.getDiffValue().toString().contains("день")
                      || p.getAction().contains("из");
          if (predicate.test(e)) {
            result =
                calculateDate(originDate, e.getAction(), e.getDiffValue())
                    .format(DateTimeFormatter.ofPattern(e.getFormat()));
          } else {
            var diffValue = DateHelper.parseDateTime(processValue(e.getDiffValue().toString()));
            result = getDifferenceBetweenDates(originDate, diffValue, e.getFormat()).toString();
          }
          akitaScenario.setVar(e.getVarName(), result);
          akitaScenario.log(String.format("Значение %s: %s", e.getVarName(), result));
        });
  }

  /**
   * Шаг для проверки дат на соответствие условиям из таблицы параметров.
   *
   * <p>Колонки таблицы параметров: - Значение 1 - первая сравниваемая дата - Условие - условие,
   * которому должны соответствовать сравниваемые даты (равно, не равно, до, после, имеет формат) -
   * Значение 2 - вторая сравниваемая дата - Допуск - допустимое отклонение от ожидаемого значения
   * для условия 'равно' - НЕобязательно поле (одно из значений типа 3s, 7m, 1h, 2d - секунды,
   * минуты, часы или дни)
   *
   * <p>Пример: И для дат выполняются условия из таблицы: | Значение 1 | Условие | Значение 2 |
   * Допуск | | ${currentDate} | равно | ${newDate4} | | | ${date2} | равно | ${newDate4} | 12h | |
   * ${date2} | не равно | ${newDate4} | | | ${firstDayOfMonth} | до | ${lastDayOfMonth} | | |
   * ${lastDayOfMonth} | после | ${firstDayOfMonth} | | | ${date2} | имеет формат | dd.MM.yyyy
   * HH:mm:ss.SSS | |
   *
   * @param conditions список параметров проверок
   */
  @И("^для дат выполняются условия из таблицы:?$")
  public void assertDates(List<DateAssertableConditionParam> conditions) {
    MainAssert mainAssert = new MainAssert();
    conditions.forEach(e -> mainAssert.getAssertion(e.getType()).accept(e, e.getActualValue()));
    mainAssert.assertAll();
  }

  /**
   * Шаг для выполнения Groovy скрипта. Применяется для выполнения арифметический действий -
   * сложение, вычитание, умножение и деление. Для действий применяются стандартные правила порядка
   * выполнения операций - сначала умножение и деление, потом сложение и вычитание и так далее. Для
   * использование переменных нужно писать их название без знаков ${}.<br>
   * Например: var1 + var2 * 2 - var3
   *
   * @param expression Groovy выражение
   * @param varName имя переменной, в которой будет хранится результат выполнения выражения
   */
  @И("^выполнить выражение \"(.+)\" и сохранить его результат в переменную \"(.+)\"$")
  public void evaluateExpression(String expression, String varName) {
    String result = String.valueOf(akitaScenario.getVars().evaluate(expression));
    akitaScenario.log("Результат выполнения выражения: " + result);
    akitaScenario.setVar(varName, result);
  }

  /**
   * Шаг для написания числа прописью в указанном формате (формат из библиотеки icu4j). Для
   * подсказки данное число во всех доступных форматах будет выведено в консоль. Пример: 11.6 в
   * формате "%spellout-cardinal-masculine" - одиннадцать целых шесть десятых 1 в формате
   * "%spellout-ordinal-neuter-genitive" - Первого
   *
   * @param number число, которое нужно записать прописью
   * @param isCapitalizedOrCaps отформатировать полученную запись: "с большой буквы", "в верхнем
   *     регистре" или null, если такое форматирование не нужно
   * @param format формат из библиотеки icu4j. Для подсказки данное число во всех доступных форматах
   *     будет выведено в консоль.
   * @param varName имя переменной сценария
   */
  @И(
      "^сохранить число \"(.+)\" прописью( с большой буквы| в верхнем регистре)? в формате \"(.+)\" в переменную \"(.+)\"$")
  public void numberToString(
      String number, String isCapitalizedOrCaps, String format, String varName) {
    var parsedNumber = new BigDecimal(processValue(number));
    format = processValue(format);
    // hint для определения нужного формата
    LOGGER.debug(StringHelper.spellOutNumberWithAllFormats(parsedNumber));
    var result = StringHelper.spellOutNumber(parsedNumber, format, isCapitalizedOrCaps);
    akitaScenario.log(
        String.format("Число '%s' прописью в формате '%s' - '%s'", number, format, result));
    akitaScenario.setVar(varName, result);
  }

  /**
   * Шаг для написания суммы в рублях прописью. Пример: 12345.67 - Двенадцать тысяч триста сорок
   * пять рублей 67 копеек 500000 - Пятьсот тысяч рублей 00 копеек
   *
   * @param amountString сумма
   * @param isCapitalizedOrCaps отформатировать полученную запись: "с большой буквы", "в верхнем
   *     регистре" или null, если такое форматирование не нужно
   * @param varName имя переменной сценария
   */
  @И(
      "^сохранить сумму \"(.+)\" в рублях прописью( с большой буквы| в верхнем регистре)? в переменную \"(.+)\"")
  public void currencyAmountToString(
      String amountString, String isCapitalizedOrCaps, String varName) {
    var amount = new BigDecimal(processValue(amountString));
    var result = StringHelper.spellOutCurrencyAmountRub(amount, isCapitalizedOrCaps);
    akitaScenario.log(String.format("Сумма '%s' в рублях прописью - '%s'", amountString, result));
    akitaScenario.setVar(varName, result);
  }

  /**
   * Метод для написания процентов прописью. Пример: 2.00 - две целых ноль десятых процента 1.10 -
   * одна целая одна десятая процента 0.25 - ноль целых двадцать пять сотых процента
   *
   * @param percentString число процентов
   * @param isCapitalizedOrCaps отформатировать полученную запись: "с большой буквы", "в верхнем
   *     регистре" или null, если такое форматирование не нужно
   * @param varName имя переменной сценария
   */
  @И(
      "^сохранить число процентов \"(.+)\" прописью( с большой буквы| в верхнем регистре)? в переменную \"(.+)\"")
  public void percentToString(String percentString, String isCapitalizedOrCaps, String varName) {
    var amount = new BigDecimal(processValue(percentString));
    var result = StringHelper.spellOutPercents(amount, isCapitalizedOrCaps);
    akitaScenario.log(String.format("Число процентов '%s' прописью - '%s'", percentString, result));
    akitaScenario.setVar(varName, result);
  }

  /**
   * Шаг для форматирования вида числа. Примеры: - формат "#": число 12345 -> "12345"; число 123.1
   * -> "123"; число 0.5 -> "0" - формат "#00": число 12345 -> "12345"; число 12 -> "12"; число 1 ->
   * "01"; число 123.1 -> "123" - формат "# ###": число 12345 -> "12 345"; число 123 -> "123" -
   * формат "#.##": число 123 -> "123"; число 1.234 -> "1.23"; число 1.235 -> "1.24"; число 0.55 ->
   * "0.55" - формат "#,00": число 123 -> "123,00"; число 0.23 -> ",23" (без нуля) - формат "#
   * ##0,00": число 12345 -> "12 345,00"; число 0.23 -> "0,23"
   *
   * @param numberString число, которое нужно форматировать
   * @param format формат (шаблон)
   * @param varName имя переменной сценария
   */
  @И("^сохранить число \"(.+)\" в формате \"(.+)\" в переменную \"(.+)\"")
  public void formatNumber(String numberString, String format, String varName) {
    format = processValue(format);
    var number = new BigDecimal(processValue(numberString));
    var result = NumberHelper.formatNumber(number, format);
    akitaScenario.log(
        String.format("Число '%s' в формате '%s' - '%s'", numberString, format, result));
    akitaScenario.setVar(varName, result);
  }

  /**
   * Сохранить из ответа (Response) тело в виде массива байтов (нужно, если в ответ приходит файл).
   *
   * @param responseName переменная сценария, в которой сохранён респонс
   * @param variableName переменная сценария, в которую нужно сохранить тело ответа в виде массива
   *     байт
   */
  @И("^из ответа \"(.*)\" значение тела сохранено в виде массива байт в переменную \"(.*)\"$")
  public void getBytesBodyAndSaveToVariable(String responseName, String variableName) {
    Response response = (Response) this.akitaScenario.getVar(responseName);
    this.akitaScenario.setVar(variableName, response.getBody().asByteArray());
    this.akitaScenario.log(
        "Тело ответа : \n" + (new Prettifier()).getPrettifiedBodyIfPossible(response, response));
  }

  /**
   * Декодировать строку, закодированную в base-64, и сохранить полученный массив байт.
   *
   * @param base64 base64-строка
   * @param varName переменная сценария, в которую нужно сохранить полученный массива байт
   */
  @И(
      "^декодировать Base64-строку \"(.*)\" и сохранить полученный массив байт в переменную \"(.*)\"$")
  public void decodeBase64StringToBytes(String base64, String varName) {
    base64 = processValue(base64);
    var bytes = Base64.getDecoder().decode(base64);
    this.akitaScenario.setVar(varName, bytes);
    this.akitaScenario.log("Массив байт сохранён в переменную " + varName);
  }

  /**
   * Ожидание в течение заданного в переменной количества секунд
   *
   * @param varName имя переменной или число.
   */
  @И("^выполнено ожидание в течение \"(.*)\" (?:секунд|секунды)")
  public void waitForSecondsVar(String varName) {
    String value = processValue(varName);
    try {
      Thread.sleep(1000 * Long.parseLong(value));
    } catch (NumberFormatException ex) {
      throw new RuntimeException(
          String.format(
              "Значение %s нельзя использовать как количество секунд для ожидания\n Ошибка: %s",
              value, ex.getMessage()));
    } catch (InterruptedException e) {
      throw new RuntimeException("Произошла ошибка во время ожидания", e);
    }
  }

  /**
   * Шаг для проверки строки на соответствие условиям. Таблица параметров шага содержит 2 колонки:
   * "Условие" (равно, не равно, пустая, не пустая, содержит, не содержит, начинается с,
   * заканчивается на, регулярное выражение) и "Значение" (ожидаемое значение). Пример: И для строки
   * "${text}" выполняются условия из таблицы: | Условие | Значение | | регулярное выражение |
   * [\s\S]*Получены ошибки при получении в АС Олимп: 4203[\s\S]* | | содержит | Получены ошибки при
   * получении в АС Олимп: 4203 | | начинается с |
   * ru.gpb.ccl.limitmanager.exception.LimitManagerException |
   *
   * @param varName имя переменной сценария или проверяемая строка
   * @param conditions условия проверки
   */
  @И("^для строки \"(.*)\" выполняются условия из таблицы:?")
  public void assertStringValue(String varName, List<StringAssertableConditionParam> conditions) {
    String value = processValue(varName);
    MainAssert mainAssert = new MainAssert();
    conditions.forEach(e -> mainAssert.getAssertion(e.getType()).accept(e, value));
    mainAssert.assertAll();
  }

  /**
   * Шаг для формирования тела запроса, которое состоит из сотавных частей (multipart) Данный шаг
   * помогает избежать проблемы с отправкой русских символов в названии файла, при запросах
   * multipart
   *
   * <p>Пример: И формируем тело api-запроса, сохраняем полученный массив байт в переменную
   * "requestMultiPartBody", разделитель сохраняем в переменную "boundary" | type | name |
   * contentType | value | | file | data | application/octet-stream |
   * ${test.data.base.path}/platform/serviceapi/file-store/files/Шаблон согласия на запрос кредитной
   * истории в БКИ.pdf | | text | metaData | application/json |
   * {"fileTypeId":"80efdacc-c003-4333-8dde-30e56b2e8788","applicationId":"3fa85f64-5717-4562-b3fc-2c963f66afa6","sigVerified":true,"businessKey":"3fa85f64-5717-4562-b3fc-2c963f66afa6","action":"some","sadkoFileKey":0,"userLogin":
   * "GFF"}} |
   *
   * @param varName имя переменной, в которую будет сохранятся итоговое значение
   * @param boundaryName имя переменной, в которую будет сохранятся разделитель, который
   *     используется для склейки частей в теле
   * @param table таблица с параметрами
   */
  @SneakyThrows
  @И(
      "^формируем тело api-запроса, сохраняем полученный массив байт в переменную \"(.*)\", разделитель сохраняем в переменную \"(.*)\"$")
  public void step(String varName, String boundaryName, List<Map<String, String>> table) {
    List<byte[]> byteArrays = new ArrayList<>();
    // Разделитель частей в multipart
    String boundary = getBoundary();
    akitaScenario.setVar(boundaryName, boundary);

    for (Map<String, String> line : table) {
      String type = line.get("type");
      String name = line.get("name");
      String mimeType = line.get("contentType");
      String value = processValue(line.get("value"));

      byteArrays.add(("--" + boundary).getBytes(StandardCharsets.UTF_8));
      byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
      byte[] dataByte;

      if (type.equals("file")) {
        File file = new File(value);
        String filename = file.getName();
        byteArrays.add(
            String.format(
                    "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"; filename*=UTF-8''%s",
                    name, filename, URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8));
        dataByte = Files.readAllBytes(file.toPath());
      } else {
        byteArrays.add(
            String.format("Content-Disposition: form-data; name=\"%s\";", name)
                .getBytes(StandardCharsets.UTF_8));
        dataByte = (value).getBytes();
      }
      byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
      byteArrays.add(String.format("Content-Type: %s", mimeType).getBytes(StandardCharsets.UTF_8));
      byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
      byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
      byteArrays.add(dataByte);
      byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));

    // Формируем итоговое значение тела сообщения
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (byte[] byteArray : byteArrays) {
      out.write(byteArray);
    }
    byte[] resultBody = out.toByteArray();

    akitaScenario.setVar(varName, resultBody);
    akitaScenario.log(new String(resultBody));
  }

  private String getBoundary() {
    return new BigInteger(256, new Random()).toString();
  }
}
