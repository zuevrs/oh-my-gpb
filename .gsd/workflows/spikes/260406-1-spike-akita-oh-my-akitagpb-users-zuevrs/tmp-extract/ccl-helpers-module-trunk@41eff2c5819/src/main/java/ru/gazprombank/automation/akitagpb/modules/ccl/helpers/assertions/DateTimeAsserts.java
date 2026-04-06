package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.assertions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.assertj.core.api.SoftAssertions;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels.AssertableConditionParam;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.date.DateHelper;

/** Класс для проверки полей, содержащих дату/время */
public class DateTimeAsserts {

  static final Map<String, BiConsumer<String, AssertableConditionParam>> DATE_TIME_MAP =
      new HashMap<>();
  private static SoftAssertions softAssertions;

  static {
    DATE_TIME_MAP.put(
        "равно",
        (actual, param) -> {
          var actualTime = DateHelper.parseDateTime(actual);
          var expected = DateHelper.parseDateTime(param.getExpectedValue().toString());
          if (param.getDateOffset() != null) {
            softAssertions
                .assertThat(actualTime)
                .as(
                    String.format(
                        "Ожидалось, что дата, найденная по пути '%s', равна ожидаемой дате '%s' с допуском",
                        param.getPath(), expected))
                .isCloseTo(expected, param.getDateOffset());
          } else {
            softAssertions
                .assertThat(actualTime)
                .as(
                    String.format(
                        "Ожидалось, что дата, найденная по пути '%s', равна ожидаемой дате '%s'",
                        param.getPath(), expected))
                .isEqualTo(expected);
          }
        });
    DATE_TIME_MAP.put(
        "не равно",
        (actual, param) -> {
          var actualTime = DateHelper.parseDateTime(actual);
          var expected = DateHelper.parseDateTime(param.getExpectedValue().toString());
          softAssertions
              .assertThat(actualTime)
              .as(
                  String.format(
                      "Ожидалось, что дата, найденная по пути '%s', НЕ равна ожидаемой дате '%s'",
                      param.getPath(), expected))
              .isNotEqualTo(expected);
        });
    DATE_TIME_MAP.put(
        "после",
        (actual, param) -> {
          var actualTime = DateHelper.parseDateTime(actual);
          var expected = DateHelper.parseDateTime(param.getExpectedValue().toString());
          softAssertions
              .assertThat(actualTime)
              .as(
                  String.format(
                      "Ожидалось, что дата, найденная по пути '%s' ('%s'), идёт после заданной даты '%s'",
                      param.getPath(), actualTime, expected))
              .isAfter(expected);
        });
    DATE_TIME_MAP.put(
        "после или равно",
        (actual, param) -> {
          var actualTime = DateHelper.parseDateTime(actual);
          var expected = DateHelper.parseDateTime(param.getExpectedValue().toString());
          softAssertions
              .assertThat(actualTime)
              .as(
                  String.format(
                      "Ожидалось, что дата, найденная по пути '%s' ('%s'), идёт после или равна заданной даты '%s'",
                      param.getPath(), actualTime, expected))
              .isAfterOrEqualTo(expected);
        });
    DATE_TIME_MAP.put(
        "до",
        (actual, param) -> {
          var actualTime = DateHelper.parseDateTime(actual);
          var expected = DateHelper.parseDateTime(param.getExpectedValue().toString());
          softAssertions
              .assertThat(actualTime)
              .as(
                  String.format(
                      "Ожидалось, что дата, найденная по пути '%s' ('%s'), идёт до заданной даты '%s'",
                      param.getPath(), actualTime, expected))
              .isBefore(expected);
        });
    DATE_TIME_MAP.put(
        "до или равно",
        (actual, param) -> {
          var actualTime = DateHelper.parseDateTime(actual);
          var expected = DateHelper.parseDateTime(param.getExpectedValue().toString());
          softAssertions
              .assertThat(actualTime)
              .as(
                  String.format(
                      "Ожидалось, что дата, найденная по пути '%s' ('%s'), идёт до или равна заданной даты '%s'",
                      param.getPath(), actualTime, expected))
              .isBeforeOrEqualTo(expected);
        });
    DATE_TIME_MAP.put(
        "имеет формат",
        (actual, param) -> {
          var pattern = param.getExpectedValue().toString();
          softAssertions
              .assertThatCode(() -> DateHelper.parseDateTime(actual, pattern))
              .as(
                  String.format(
                      "Ожидалось, что дата, найденная по пути '%s' ('%s'), имеет формат '%s'",
                      param.getPath(), actual, pattern))
              .doesNotThrowAnyException();
        });
  }

  static void setSoftAssertions(SoftAssertions softAssertions) {
    DateTimeAsserts.softAssertions = softAssertions;
  }
}
