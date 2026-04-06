package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.date;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;

/** Утилитный класс - калькулятор дат. */
public class DateCalculator {

  /** Соответствие паттернов и единиц времени, для парсинга строки вида "1y 2M 6d 4m 5s 6000S" */
  public static final Map<String, ChronoUnit> chronoUnitPatternsMap =
      Map.of(
          "((\\d+)|(\\$\\{[A-Za-z\\d_-]+}))[yY]", ChronoUnit.YEARS,
          "((\\d+)|(\\$\\{[A-Za-z\\d_-]+}))[M]", ChronoUnit.MONTHS,
          "((\\d+)|(\\$\\{[A-Za-z\\d_-]+}))[dD]", ChronoUnit.DAYS,
          "((\\d+)|(\\$\\{[A-Za-z\\d_-]+}))[hH]", ChronoUnit.HOURS,
          "((\\d+)|(\\$\\{[A-Za-z\\d_-]+}))[m]", ChronoUnit.MINUTES,
          "((\\d+)|(\\$\\{[A-Za-z\\d_-]+}))[s]", ChronoUnit.SECONDS,
          "((\\d+)|(\\$\\{[A-Za-z\\d_-]+}))[S]", ChronoUnit.NANOS);
  /**
   * Соответствие единиц времени классов ChronoUnit и ChronoField - т.к. методы класса ZonedDateTime
   * minus() и plus() принимают ChronoUnit, а метод with() принимает ChronoField
   */
  private static final Map<ChronoUnit, ChronoField> chronoUnitToChronoFieldConversion =
      Map.of(
          ChronoUnit.YEARS, ChronoField.YEAR,
          ChronoUnit.MONTHS, ChronoField.MONTH_OF_YEAR,
          ChronoUnit.DAYS, ChronoField.DAY_OF_MONTH,
          ChronoUnit.HOURS, ChronoField.HOUR_OF_DAY,
          ChronoUnit.MINUTES, ChronoField.MINUTE_OF_HOUR,
          ChronoUnit.SECONDS, ChronoField.SECOND_OF_MINUTE,
          ChronoUnit.NANOS, ChronoField.NANO_OF_SECOND);
  /** Соответствие операций расчёта дат (минус, плюс, установить) и функций для этих операций */
  @SuppressWarnings("unchecked")
  private static final Map<String, BiFunction<ZonedDateTime, Object, ZonedDateTime>>
      dateCalculationActionsMap =
          Map.of(
              "минус",
                  (a, b) -> {
                    var result = new AtomicReference<>(a);
                    ((Map<ChronoUnit, Long>) b)
                        .forEach((k, v) -> result.set(result.get().minus(v, k)));
                    return result.get();
                  },
              "плюс",
                  (a, b) -> {
                    var result = new AtomicReference<>(a);
                    ((Map<ChronoUnit, Long>) b)
                        .forEach((k, v) -> result.set(result.get().plus(v, k)));
                    return result.get();
                  },
              "установить",
                  (a, b) -> {
                    var result = new AtomicReference<>(a);
                    if (b.toString().equals("первый день месяца")) {
                      result.set(a.withDayOfMonth(1));
                    } else if (b.toString().equals("последний день месяца")) {
                      result.set(
                          a.withDayOfMonth(a.getMonth().length(a.toLocalDate().isLeapYear())));
                    } else if (b.toString().equals("последний день недели")) {
                      result.set(a.plusDays(7L - a.getDayOfWeek().getValue()));
                    } else {
                      ((Map<ChronoUnit, Long>) b)
                          .forEach(
                              (k, v) ->
                                  result.set(
                                      result
                                          .get()
                                          .with(chronoUnitToChronoFieldConversion.get(k), v)));
                    }
                    return result.get();
                  },
              "ранняя из",
                  (a, b) -> {
                    var date = DateHelper.parseDateTime(resolveVariables(b.toString()));
                    return a.isBefore(date) ? a : date;
                  },
              "поздняя из",
                  (a, b) -> {
                    var date = DateHelper.parseDateTime(resolveVariables(b.toString()));
                    return a.isAfter(date) ? a : date;
                  },
              "плюс рабочих дней",
                  (a, b) -> {
                    var result = new AtomicReference<>(a);
                    int workDays = ((Map<ChronoUnit, Long>) b).get(ChronoUnit.DAYS).intValue();
                    ZonedDateTime resultDay = result.get();

                    int counterWorkDay = 0;
                    while (counterWorkDay < workDays) {
                      resultDay = resultDay.plusDays(1);
                      if (!DateHelper.isHoliday(resultDay.toLocalDate())) {
                        counterWorkDay++;
                      }
                    }
                    return resultDay;
                  });
  /**
   * Соответствие функций определения интервала между датами в зависимости от единицы времени (год,
   * месяц, день и т.д.)
   */
  private static final Map<String, BiFunction<ZonedDateTime, ZonedDateTime, Long>>
      chronoUnitDurationMap =
          Map.of(
              "yY", ChronoUnit.YEARS::between,
              "M", ChronoUnit.MONTHS::between,
              "dD", (a, b) -> Duration.between(a, b).abs().toDays(),
              "hH", (a, b) -> Duration.between(a, b).abs().toHours(),
              "m", (a, b) -> Duration.between(a, b).abs().toMinutes(),
              "s", (a, b) -> Duration.between(a, b).abs().toSeconds(),
              "S", (a, b) -> Duration.between(a, b).abs().toNanos());

  /**
   * Метод-калькулятор дат. С помощью него можно: - убавить от даты/прибавить к дате указанный
   * интервал времени (годы - y (Y), месяцы - M, дни - d (D), часы - h (H), минуты - m, секунды - s,
   * наносекунды - S) - установить для исходной даты значение каких-либо единиц времени
   * (первый/последний день месяца, установить минуты на 0 и т.д.)
   *
   * @param firstValue исходная дата
   * @param action действие над датой (минус, плюс, установить)
   * @param secondValue значение, которое нужно вычесть/прибавить/установить ("1y 2M 3d 4m 5s
   *     6000S", "первый день месяца", "последний день месяца")
   * @return полученное значение ZonedDateTime
   */
  @SuppressWarnings("unchecked")
  public static ZonedDateTime calculateDate(
      ZonedDateTime firstValue, String action, Object secondValue) {
    if (action == null) {
      return firstValue;
    }
    if (secondValue.getClass() == HashMap.class) {
      secondValue =
          ((HashMap<ChronoUnit, String>) secondValue)
              .entrySet().stream()
                  .collect(
                      Collectors.toMap(
                          Entry::getKey, e -> Long.parseLong(resolveVariables(e.getValue()))));
    }
    return dateCalculationActionsMap.get(action).apply(firstValue, secondValue);
  }

  /**
   * Метод для получения интервала времени между двумя датами в указанных единицах.
   *
   * @param first первая дата
   * @param second вторая дата
   * @param format единица времени, в которой нужно вычислить интервал между датами (годы - y (Y),
   *     месяцы - M, дни - d (D), часы - h (H), минуты - m, секунды - s, наносекунды - S)
   * @return интервал времени между датами в указанных единицах
   */
  public static Long getDifferenceBetweenDates(
      ZonedDateTime first, ZonedDateTime second, String format) {
    return chronoUnitDurationMap.entrySet().stream()
        .filter(e -> e.getKey().contains(format))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Формат [" + format + "] неопределён."))
        .getValue()
        .apply(first, second);
  }

  /**
   * Метод для получения из строки вида "1y 2M ${days}d 4m 5s 6000S" мапы единиц времени
   * (ChronoUnit'ов) с количеством этих единиц (ChronoUnit.YEARS = 1, ChronoUnit.MONTHS = 2,
   * ChronoUnit.DAYS = ${days} и т.д.)
   *
   * @param value строка вида "1y 2M 3d 4m 5s 6000S"
   * @return полученная карта Map<ChronoUnit, String>
   */
  public static Map<ChronoUnit, String> getChronoUnitsMap(String value) {
    var result = new HashMap<ChronoUnit, String>();
    chronoUnitPatternsMap.forEach(
        (k, v) -> {
          var number = StringHelper.getRegexpGroupValue(value, k, 1);
          if (number != null) {
            result.put(v, number);
          }
        });
    return result;
  }
}
