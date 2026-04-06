package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.assertions;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.assertions.MainAssert.parseNumber;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Offset;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels.AssertableConditionParam;

/** Класс для проверки полей, содержащих числовое значение (целые и дробные числа) */
public class NumberAsserts {

  static final Map<String, BiConsumer<Double, AssertableConditionParam>> NUMBER_MAP =
      new HashMap<>();
  private static SoftAssertions softAssertions;

  static {
    NUMBER_MAP.put(
        "равно",
        (actual, param) -> {
          var expected = parseNumber(param.getExpectedValue().toString());
          if (param.getNumberOffset() != null) {
            softAssertions
                .assertThat(actual)
                .as(
                    String.format(
                        "Ожидалось, что число, найденное по пути '%s', равно ожидаемому числу '%s' с допуском '%s'",
                        param.getPath(), expected, param.getNumberOffset()))
                .isCloseTo(expected, Offset.offset(param.getNumberOffset()));
          } else {
            softAssertions
                .assertThat(actual)
                .as(
                    String.format(
                        "Ожидалось, что число, найденное по пути '%s', равно ожидаемому числу '%s'",
                        param.getPath(), expected))
                .isEqualTo(expected);
          }
        });
    NUMBER_MAP.put(
        "не равно",
        (actual, param) -> {
          var expected = parseNumber(param.getExpectedValue().toString());
          softAssertions
              .assertThat(actual)
              .as(
                  String.format(
                      "Ожидалось, что число, найденное по пути '%s', НЕ равно ожидаемому числу '%s'",
                      param.getPath(), expected))
              .isNotEqualTo(expected);
        });
    NUMBER_MAP.put(
        "больше",
        (actual, param) -> {
          var expected = parseNumber(param.getExpectedValue().toString());
          softAssertions
              .assertThat(actual)
              .as(
                  String.format(
                      "Ожидалось, что число, найденное по пути '%s', больше заданного числа '%s'",
                      param.getPath(), expected))
              .isGreaterThan(expected);
        });
    NUMBER_MAP.put(
        "больше или равно",
        (actual, param) -> {
          var expected = parseNumber(param.getExpectedValue().toString());
          softAssertions
              .assertThat(actual)
              .as(
                  String.format(
                      "Ожидалось, что число, найденное по пути '%s', больше или равно заданного числа '%s'",
                      param.getPath(), expected))
              .isGreaterThanOrEqualTo(expected);
        });
    NUMBER_MAP.put(
        "меньше",
        (actual, param) -> {
          var expected = parseNumber(param.getExpectedValue().toString());
          softAssertions
              .assertThat(actual)
              .as(
                  String.format(
                      "Ожидалось, что число, найденное по пути '%s', меньше заданного числа '%s'",
                      param.getPath(), expected))
              .isLessThan(expected);
        });
    NUMBER_MAP.put(
        "меньше или равно",
        (actual, param) -> {
          var expected = parseNumber(param.getExpectedValue().toString());
          softAssertions
              .assertThat(actual)
              .as(
                  String.format(
                      "Ожидалось, что число, найденное по пути '%s', меньше или равно заданного числа '%s'",
                      param.getPath(), expected))
              .isLessThanOrEqualTo(expected);
        });
    NUMBER_MAP.put(
        "одно из",
        (actual, param) -> {
          @SuppressWarnings("unchecked")
          var expected =
              ((List<Object>) param.getExpectedValue())
                  .stream().map(e -> parseNumber(e.toString())).collect(Collectors.toList());
          softAssertions
              .assertThat(actual)
              .as(
                  String.format(
                      "Ожидалось, что число, найденное по пути '%s', равно одному из переданных значений - [%s]",
                      param.getPath(), expected))
              .isIn(expected);
        });
    NUMBER_MAP.put("один из", NUMBER_MAP.get("одно из"));
  }

  static void setSoftAssertions(SoftAssertions softAssertions) {
    NumberAsserts.softAssertions = softAssertions;
  }
}
