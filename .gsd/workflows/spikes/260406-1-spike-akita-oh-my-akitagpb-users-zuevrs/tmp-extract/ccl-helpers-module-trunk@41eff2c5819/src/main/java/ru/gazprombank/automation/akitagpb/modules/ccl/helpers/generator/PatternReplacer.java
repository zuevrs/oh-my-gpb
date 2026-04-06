package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.generator;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.generator.DataGenerationHelper.UUID_GENERATOR;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.FileHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.date.DateHelper;

/**
 * Вспомогательный класс для метода {@link DataGenerationHelper#generateVariable(String)} Содержит
 * методы, помеченные аннотацией {@link GeneratablePattern}, заменяющие в исходной строке
 * генерируемый шаблон на соответствующее сгенерированное значение.
 */
public class PatternReplacer {

  /**
   * Метод заменяет в исходной строке шаблон "текущая дата в формате ***" на соответствующее
   * значение - дату в указанном формате.
   *
   * @param value строка для обработки/генерации
   * @return возвращает исходную строку, в которой заменен шаблон на сгенерированное значение
   */
  @GeneratablePattern("текущая дата в формате")
  static String replaceCurrentDate(String value) {
    String dateFormat =
        StringHelper.getRegexpGroupValue(value, ".*текущая дата в формате \"(.+)\".*", 1);
    return value.replace(
        String.format("текущая дата в формате \"%s\"", dateFormat),
        DateHelper.formatCurrentDateTime(dateFormat));
  }

  /**
   * Метод заменяет в исходной строке шаблон "текущая дата МСК в формате ***" на соответствующее
   * значение - дату в Московском часовом поясе в указанном формате.
   *
   * @param value строка для обработки/генерации
   * @return возвращает исходную строку, в которой заменен шаблон на сгенерированное значение
   */
  @GeneratablePattern("текущая дата МСК в формате")
  static String replaceCurrentMoscowDate(String value) {
    String dateFormat =
        StringHelper.getRegexpGroupValue(value, ".*текущая дата МСК в формате \"(.+)\".*", 1);
    return value.replace(
        String.format("текущая дата МСК в формате \"%s\"", dateFormat),
        DateHelper.formatCurrentMoscowDateTime(dateFormat));
  }

  /**
   * Метод заменяет в исходной строке шаблон "текущий месяц (1-12)" на номер текущего месяца (1,..,
   * 9, 10, 11, 12).
   *
   * @param value строка для обработки/генерации
   * @return возвращает исходную строку, в которой заменен шаблон на сгенерированное значение
   */
  @GeneratablePattern("текущий месяц (1-12)")
  static String replaceCurrentMonthNumber(String value) {
    return value.replace("текущий месяц (1-12)", DateHelper.getCurrentMonthNumber());
  }

  /**
   * Метод заменяет в исходной строке шаблон "текущий месяц (01-12)" на номер текущего месяца с
   * лидирующим нулём (01,.., 09, 10, 11, 12).
   *
   * @param value строка для обработки/генерации
   * @return возвращает исходную строку, в которой заменен шаблон на сгенерированное значение
   */
  @GeneratablePattern("текущий месяц (01-12)")
  static String replaceCurrentMonthNumberZeroPadded(String value) {
    return value.replace("текущий месяц (01-12)", DateHelper.getCurrentMonthNumberZeroPadded());
  }

  /**
   * Метод заменяет в исходной строке шаблон "текущий год" на номер текущего года (2022).
   *
   * @param value строка для обработки/генерации
   * @return возвращает исходную строку, в которой заменен шаблон на сгенерированное значение
   */
  @GeneratablePattern("текущий год")
  static String replaceCurrentYear(String value) {
    return value.replace("текущий год", DateHelper.getCurrentYear());
  }

  /**
   * Метод заменяет в исходной строке шаблон "текущий период для заявки ЭБГ (1-4)" на номер
   * соответствующего ЭБГ-периода (1, 2, 3, 4).
   *
   * @param value строка для обработки/генерации
   * @return возвращает исходную строку, в которой заменен шаблон на сгенерированное значение
   */
  @GeneratablePattern("текущий период для заявки ЭБГ (1-4)")
  static String replaceCurrentEbgPeriod(String value) {
    return value.replace(
        "текущий период для заявки ЭБГ (1-4)", DataGenerationHelper.getCurrentEbgPeriod());
  }

  /**
   * Метод заменяет в исходной строке шаблон "рандомный UUID" на сгенерированный UUID.
   *
   * @param value строка для обработки/генерации
   * @return возвращает исходную строку, в которой заменен шаблон на сгенерированное значение
   */
  @GeneratablePattern("рандомный UUID")
  static String replaceRandomUuid(String value) {
    return value.replace("рандомный UUID", UUID.randomUUID().toString());
  }

  /**
   * Метод заменяет в исходной строке шаблон "рандомный UUIDv7" на сгенерированный UUID версии 7.
   *
   * @param value строка для обработки/генерации
   * @return возвращает исходную строку, в которой заменен шаблон на сгенерированное значение
   */
  @GeneratablePattern("рандомный UUIDv7")
  static String replaceRandomUuidv7(String value) {
    return value.replace("рандомный UUIDv7", UUID_GENERATOR.generate().toString());
  }

  /**
   * Метод заменяет в исходной строке шаблон "текущее время в миллисекундах" на соответствующее
   * значение (число типа long).
   *
   * @param value строка для обработки/генерации
   * @return возвращает исходную строку, в которой заменен шаблон на сгенерированное значение
   */
  @GeneratablePattern("текущее время в миллисекундах")
  static String replaceCurrentTimeMillis(String value) {
    return value.replace(
        "текущее время в миллисекундах", String.valueOf(System.currentTimeMillis()));
  }

  /**
   * Метод заменяет в исходной строке шаблон "содержимое файла ресурсов" на полученное из указанного
   * файла содержимое.
   *
   * @param value строка для обработки/генерации
   * @return возвращает исходную строку, в которой заменен шаблон на сгенерированное значение
   */
  @GeneratablePattern("содержимое файла ресурсов")
  static String replaceResourcesFileContent(String value) {
    String filePath =
        StringHelper.getRegexpGroupValue(value, ".*содержимое файла ресурсов \"(.+)\".*", 1);
    return value.replace(
        String.format("содержимое файла ресурсов \"%s\"", filePath),
        FileHelper.getResourcesFileContent(processValue(filePath)));
  }

  /**
   * Метод заменяет в исходной строке шаблон "случайное число" на сгенерированное по заданным
   * параметрам число.
   *
   * <p>случайное число -> сгенерированное число, от 0 до 100000; случайное число от * ->
   * сгенерированное число с нижним пределом (верхний предел +100000); случайное число до * ->
   * сгенерированное число с верхним пределом (нижний предел = 0); случайное число от * до * ->
   * сгенерированное число в указанных пределах; случайное число длиной * -> сгенерированное число
   * длиной * цифр;
   *
   * @param value строка для обработки/генерации
   * @return возвращает исходную строку, в которой заменен шаблон на сгенерированное значение
   */
  @GeneratablePattern("случайное число")
  static String replaceRandomNumber(String value) {
    String pattern = ".*случайное число( длиной (\\d+)|( от (\\d+))?( до (\\d+))?)?.*";
    String length = StringHelper.getRegexpGroupValue(value, pattern, 2);
    String from = StringHelper.getRegexpGroupValue(value, pattern, 4);
    String to = StringHelper.getRegexpGroupValue(value, pattern, 6);
    String target = "случайное число";
    target = length == null ? target : target + " длиной " + length;
    target = from == null ? target : target + " от " + from;
    target = to == null ? target : target + " до " + to;
    return value.replace(
        target,
        length == null
            ? DataGenerationHelper.getRandomNumber(from, to).toString()
            : StringHelper.getRandomNumericString(Integer.parseInt(length)));
  }

  /**
   * Метод заменяет в исходной строке шаблон "случайная( кириллическая)? строка" на сгенерированную
   * по заданным параметрам строку.
   *
   * <p>случайная строка длиной * -> сгенерированная строка длиной * из латинских символов;
   * случайная кириллическая строка длиной * -> сгенерированная строка длиной * из кириллических
   * символов; случайная строка из цифр длиной * -> сгенерированное число длиной * цифр
   * (альтернатива "случайное число длиной *");
   *
   * @param value строка для обработки/генерации
   * @return возвращает исходную строку, в которой заменен шаблон на сгенерированное значение
   */
  @GeneratablePattern("случайная( кириллическая)? строка")
  static String replaceRandomString(String value) {
    String pattern = ".*случайная( кириллическая)? строка( из цифр)?( длиной (\\d+))?.*";
    boolean isLatin = StringHelper.getRegexpGroupValue(value, pattern, 1) == null;
    boolean isNumeric = StringHelper.getRegexpGroupValue(value, pattern, 2) != null;
    String length = StringHelper.getRegexpGroupValue(value, pattern, 4);
    String target = "случайная";
    target = isLatin ? target + " строка" : target + " кириллическая строка";
    target = isNumeric ? target + " из цифр" : target;
    target = length == null ? target : target + " длиной " + length;
    return value.replace(target, StringHelper.getRandomString(length, isLatin, isNumeric));
  }

  /**
   * Метод заменяет в исходной строке шаблон "случайный ИНН ЮЛ" на сгенерированный 10-значный
   * валидный ИНН.
   */
  @GeneratablePattern("случайный ИНН (ЮЛ|ФЛ)")
  static String replaceINN(String value) {
    String pattern = ".*случайный ИНН (ЮЛ|ФЛ).*";
    String type = StringHelper.getRegexpGroupValue(value, pattern, 1);
    String inn;
    List<Integer> check;
    if (Objects.equals(type, "ЮЛ")) {
      inn = StringHelper.getRandomNumericString(9);
      // массив для вычисления контрольной суммы
      check = List.of(2, 4, 10, 3, 5, 9, 4, 6, 8);
    } else {
      inn = StringHelper.getRandomNumericString(11);
      // массив для вычисления контрольной суммы
      check = List.of(3, 7, 2, 4, 10, 3, 5, 9, 4, 0, 0);
    }
    int checkSum = 0;
    for (int i = 1; i <= inn.length(); i++) {
      checkSum = checkSum + Integer.parseInt(inn.substring(i - 1, i)) * check.get(i - 1);
    }
    checkSum = (checkSum % 11) % 10;

    return value.replace("случайный ИНН " + type, inn + checkSum);
  }
}
