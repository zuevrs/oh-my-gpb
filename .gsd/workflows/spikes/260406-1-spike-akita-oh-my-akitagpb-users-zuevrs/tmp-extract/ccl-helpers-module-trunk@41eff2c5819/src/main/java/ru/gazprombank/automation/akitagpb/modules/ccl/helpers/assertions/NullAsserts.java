package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.assertions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.assertj.core.api.SoftAssertions;

/** Класс для проверки полей на null */
public class NullAsserts {

  static final Map<String, Consumer<Object>> NULL_MAP = new HashMap<>();
  private static SoftAssertions softAssertions;

  static {
    NULL_MAP.put(
        "равно",
        (actual) ->
            softAssertions.assertThat(actual).as("Ожидалось, что объект равен 'null'").isNull());
    NULL_MAP.put("", NULL_MAP.get("равно"));
    NULL_MAP.put(
        "не равно",
        (actual) ->
            softAssertions
                .assertThat(actual)
                .as("Ожидалось, что объект НЕ равен 'null'")
                .isNotNull());
    NULL_MAP.put("не", NULL_MAP.get("не равно"));
  }

  static void setSoftAssertions(SoftAssertions softAssertions) {
    NullAsserts.softAssertions = softAssertions;
  }
}
