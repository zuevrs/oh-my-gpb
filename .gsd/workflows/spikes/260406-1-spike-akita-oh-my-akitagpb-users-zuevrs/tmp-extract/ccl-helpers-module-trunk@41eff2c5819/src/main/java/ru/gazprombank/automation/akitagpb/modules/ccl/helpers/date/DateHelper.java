package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.date;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.getRegexpGroupValue;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import org.assertj.core.data.TemporalUnitLessThanOffset;
import org.assertj.core.data.TemporalUnitOffset;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;

/** Утилитный класс для работы с датой/временем */
public class DateHelper {

  public static final List<String> PATTERNS =
      ConfigLoader.getConfigValueOrDefault("date.comparison.patterns", new ArrayList<>());

  /**
   * Метод для форматирования текущей даты/времени по указанному формату
   *
   * @param pattern формат даты/времени
   * @return текущие дата/время в заданном формате
   */
  public static String formatCurrentDateTime(String pattern) {
    return ZonedDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
  }

  /**
   * Метод для форматирования текущей даты/времени в Московском часовом поясе по указанному формату
   *
   * @param pattern формат даты/времени
   * @return текущие дата/время в Московском часовом поясе в заданном формате
   */
  public static String formatCurrentMoscowDateTime(String pattern) {
    return ZonedDateTime.now()
        .withZoneSameInstant(ZoneId.of("Europe/Moscow"))
        .format(DateTimeFormatter.ofPattern(pattern));
  }

  /**
   * Форматировать дату из первоначального формата в новый формат
   *
   * @param dateTimeValue строковое значение даты
   * @param newPattern новый формат даты/времени
   * @return дата-время в новом формате
   */
  public static String formatDateTime(String dateTimeValue, String newPattern) {
    return parseDateTime(dateTimeValue).format(DateTimeFormatter.ofPattern(newPattern));
  }

  public static String formatDate(Date date, String pattern) {
    return new SimpleDateFormat(pattern).format(date);
  }

  /**
   * Получить дату LocalDateTime по строковому значению и паттерну
   *
   * @param value строковое значение даты
   * @param pattern паттерн даты
   * @return дату LocalDateTime
   */
  public static LocalDateTime getLocalDateTime(String value, String pattern) {
    return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern));
  }

  /**
   * Получить число дней в месяце
   *
   * @param year год
   * @param month месяц
   * @return число дней в месяце
   */
  public static Integer getLengthOfMonth(int year, int month) {
    return LocalDate.of(year, month, 1).lengthOfMonth();
  }

  /**
   * Метод определяет порядковый номер текущего месяца (1-12)
   *
   * @return возвращает полученное значение месяца в виде строки.
   */
  public static String getCurrentMonthNumber() {
    return String.valueOf(LocalDate.now().getMonthValue());
  }

  /**
   * Метод определяет номер текущего года (2022)
   *
   * @return возвращает полученное значение года в виде строки.
   */
  public static String getCurrentYear() {
    return String.valueOf(LocalDate.now().getYear());
  }

  /**
   * Метод формирует значение текущего месяца в формате "MM" (01-12)
   *
   * @return возвращает полученное значение в виде строки.
   */
  public static String getCurrentMonthNumberZeroPadded() {
    return formatCurrentDateTime("MM");
  }

  /** Метод возвращает текущие дату и время в формате для Kibana. */
  public static String getKibanaCurrentDateTime() {
    SimpleDateFormat kibanaTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    kibanaTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return kibanaTimeFormat.format(new Date());
  }

  /** Метод возвращает дату и время в формате для Kibana. */
  public static String getKibanaDateTime(ZonedDateTime time) {
    return time.withZoneSameInstant(ZoneId.of("UTC"))
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
  }

  /**
   * Создать объект TemporalUnitOffset (допускаемая разница времени при сравнении дат) из строки
   * формата "3s" (3 секунды), "7m" (7 минут), "1h" (1 час), "2d" (2 дня).
   *
   * @param offset строка формата 3s, 7m, 1h, 2d
   * @return объект TemporalUnitOffset
   */
  public static TemporalUnitOffset getTemporalOffset(String offset) {
    ChronoUnit unit;
    long value = Long.parseLong(Objects.requireNonNull(getRegexpGroupValue(offset, "(\\d+)", 1)));
    if (offset.contains("s")) {
      unit = ChronoUnit.SECONDS;
    } else if (offset.contains("m")) {
      unit = ChronoUnit.MINUTES;
    } else if (offset.contains("h")) {
      unit = ChronoUnit.HOURS;
    } else if (offset.contains("d")) {
      unit = ChronoUnit.DAYS;
    } else {
      throw new RuntimeException(
          "Допуск для даты должен содержать один из параметров 's', 'm', 'h', 'd' (секунды, минуты, часы или дни)");
    }
    return new TemporalUnitLessThanOffset(value, unit);
  }

  /**
   * Распарсить дату из строки в объект ZonedDateTime - с учётом часового пояса (если он не указан в
   * строке, будет применён ZoneOffset.systemDefault()). Паттерны для парсинга указаны в
   * application.conf в параметре "date.comparison.patterns". Метод пытается распарсить строку по
   * всем паттернам поочерёдно.
   *
   * @param value дата/время в виде строковом
   * @return объект ZonedDateTime
   */
  public static ZonedDateTime parseDateTime(String value) {
    ZonedDateTime result = null;
    for (var pattern : PATTERNS) {
      try {
        result = parseDateTime(value, pattern);
        break;
      } catch (DateTimeParseException ignore) {
      }
    }
    if (result == null) {
      throw new RuntimeException(
          String.format(
              "Не удалось распарсить дату '%s' ни по одному паттерну из конфига 'date.comparison.patterns'",
              value));
    }
    return result;
  }

  /**
   * Распарсить дату из строки в объект ZonedDateTime по указанному паттерну.
   *
   * @param value дата/время в виде строковом
   * @param pattern паттерн, по которому нужно распарсить переданнцю дату value
   * @return объект ZonedDateTime
   * @throws DateTimeParseException если по указанному паттерну не получилось распарсить дату
   */
  public static ZonedDateTime parseDateTime(String value, String pattern) {
    Object result =
        DateTimeFormatter.ofPattern(pattern)
            .parseBest(value, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
    if (result.getClass() == ZonedDateTime.class) {
      return (ZonedDateTime) result;
    } else if (result.getClass() == LocalDateTime.class) {
      return ((LocalDateTime) result).atZone(ZoneOffset.systemDefault());
    } else {
      return ((LocalDate) result).atStartOfDay(ZoneOffset.systemDefault());
    }
  }

  /**
   * Проверка что день является праздничным или выходным днём
   *
   * @return признак того является ли день праздничным
   */
  public static boolean isHoliday(LocalDate localDate) {
    int year = localDate.getYear();
    List<LocalDate> holidays = CalendarHolidays.getInstance().getHolidays(year);
    List<LocalDate> transitionsDays = CalendarHolidays.getInstance().getTransitionsDays(year);

    if (localDate.getDayOfWeek().equals(DayOfWeek.SATURDAY)
        || localDate.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
      return !transitionsDays.contains(localDate);
    }

    return holidays.contains(localDate);
  }
}
