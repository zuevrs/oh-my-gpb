package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.assertions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.assertj.core.api.SoftAssertions;

/** Класс для проверки полей типа boolean */
public class BooleanAsserts {

  static final Map<String, BiConsumer<Boolean, Boolean>> BOOLEAN_MAP = new HashMap<>();
  private static SoftAssertions softAssertions;

  static {
    BOOLEAN_MAP.put(
        "равно",
        (actual, expected) ->
            softAssertions
                .assertThat(actual)
                .as("Ожидалось, что булевые значения равны")
                .isEqualTo(expected));
    BOOLEAN_MAP.put("", BOOLEAN_MAP.get("равно"));
    BOOLEAN_MAP.put(
        "не равно",
        (actual, expected) ->
            softAssertions
                .assertThat(actual)
                .as("Ожидалось, что булевые значения НЕ равны")
                .isNotEqualTo(expected));
    BOOLEAN_MAP.put("не", BOOLEAN_MAP.get("не равно"));
  }

  static void setSoftAssertions(SoftAssertions softAssertions) {
    BooleanAsserts.softAssertions = softAssertions;
  }
}
