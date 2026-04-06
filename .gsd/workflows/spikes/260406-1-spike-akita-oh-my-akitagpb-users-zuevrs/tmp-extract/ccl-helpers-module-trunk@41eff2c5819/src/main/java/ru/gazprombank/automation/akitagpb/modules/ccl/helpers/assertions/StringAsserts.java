package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.assertions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.assertj.core.api.SoftAssertions;

/** Класс для проверки строковых полей */
public class StringAsserts {

  static final Map<String, BiConsumer<String, Object>> STRING_MAP = new HashMap<>();
  private static SoftAssertions softAssertions;

  static {
    STRING_MAP.put(
        "равно",
        (actual, expected) ->
            softAssertions
                .assertThat(actual)
                .as("Ожидалось, что строки равны")
                .isEqualTo(expected.toString()));
    STRING_MAP.put("равна", STRING_MAP.get("равно"));
    STRING_MAP.put(
        "не равно",
        (actual, expected) ->
            softAssertions
                .assertThat(actual)
                .as("Ожидалось, что строки НЕ равны")
                .isNotEqualTo(expected.toString()));
    STRING_MAP.put("не равна", STRING_MAP.get("не равно"));
    STRING_MAP.put(
        "пустая",
        (actual, expected) ->
            softAssertions.assertThat(actual).as("Ожидалось, что строка пустая").isEmpty());
    STRING_MAP.put(
        "не пустая",
        (actual, expected) ->
            softAssertions.assertThat(actual).as("Ожидалось, что строка НЕ пустая").isNotEmpty());
    STRING_MAP.put(
        "содержит",
        (actual, expected) ->
            softAssertions
                .assertThat(actual)
                .as("Ожидалось, что строка содержит подстроку")
                .contains(expected.toString()));
    STRING_MAP.put(
        "не содержит",
        (actual, expected) ->
            softAssertions
                .assertThat(actual)
                .as("Ожидалось, что строка НЕ содержит подстроку")
                .doesNotContain(expected.toString()));
    STRING_MAP.put(
        "начинается с",
        (actual, expected) ->
            softAssertions
                .assertThat(actual)
                .as("Ожидалось, что строка начинается с подстроки")
                .startsWith(expected.toString()));
    STRING_MAP.put(
        "заканчивается на",
        (actual, expected) ->
            softAssertions
                .assertThat(actual)
                .as("Ожидалось, что строка заканчивается на подстроку")
                .endsWith(expected.toString()));
    STRING_MAP.put(
        "регулярное выражение",
        (actual, expected) ->
            softAssertions
                .assertThat(actual)
                .as("Ожидалось, что строка соответствует регулярному выражению")
                .matches(expected.toString()));
    STRING_MAP.put(
        "одна из",
        (actual, expected) -> {
          @SuppressWarnings("unchecked")
          var expectedList = (List<String>) expected;
          softAssertions
              .assertThat(actual)
              .as("Ожидалось, что строка равна одному из переданных значений")
              .isIn(expectedList);
        });
    STRING_MAP.put("одно из", STRING_MAP.get("одна из"));
    STRING_MAP.put("один из", STRING_MAP.get("одна из"));
  }

  static void setSoftAssertions(SoftAssertions softAssertions) {
    StringAsserts.softAssertions = softAssertions;
  }
}
