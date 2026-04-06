package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import net.javacrumbs.jsonunit.core.Option;
import org.assertj.core.api.SoftAssertions;
import org.json.JSONArray;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels.JsonAssertableConditionParam;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.date.DateHelper;

/** Класс для проверки полей, содержащих json-массивы */
public class JsonArrayAsserts {

  static final Map<String, BiConsumer<Object, JsonAssertableConditionParam>> JSON_ARRAY_MAP =
      new HashMap<>();
  private static SoftAssertions softAssertions;

  static {
    JSON_ARRAY_MAP.put(
        "равно",
        (actual, param) -> {
          try {
            actual = StringHelper.objectToJsonString(actual);
            JsonAssertions.assertThatJson(actual)
                .whenIgnoringPaths(
                    param.getIgnoringJsonFields() != null
                        ? param.getIgnoringJsonFields()
                        : new String[0])
                .when(Option.TREATING_NULL_AS_ABSENT)
                .isArray()
                .isEqualTo(param.getExpectedValue());
          } catch (JsonProcessingException e) {
            throw new RuntimeException(
                "Не удалось преобразовать объект в json-строку\n" + e.getMessage(), e);
          } catch (AssertionError e) {
            softAssertions.fail(
                String.format(
                    "Ожидалось, что json-массив, найденный по пути '%s', равен ожидаемому json'у\n %s\n actual json:\n %s\n "
                        + "expected json: %s\n",
                    param.getPath(), e.getMessage(), actual, param.getExpectedValue()),
                e);
          }
        });
    JSON_ARRAY_MAP.put(
        "равно без учёта сортировки",
        (actual, param) -> {
          try {
            actual = StringHelper.objectToJsonString(actual);
            JsonAssertions.assertThatJson(actual)
                .whenIgnoringPaths(
                    param.getIgnoringJsonFields() != null
                        ? param.getIgnoringJsonFields()
                        : new String[0])
                .when(Option.IGNORING_ARRAY_ORDER, Option.TREATING_NULL_AS_ABSENT)
                .isArray()
                .isEqualTo(param.getExpectedValue());
          } catch (JsonProcessingException e) {
            throw new RuntimeException(
                "Не удалось преобразовать объект в json-строку\n" + e.getMessage(), e);
          } catch (AssertionError e) {
            softAssertions.fail(
                String.format(
                    "Ожидалось, что json-массив, найденный по пути '%s', равен ожидаемому json'у БЕЗ учёта сортировки\n %s\n "
                        + "actual json:\n %s\n expected json: %s\n",
                    param.getPath(), e.getMessage(), actual, param.getExpectedValue()),
                e);
          }
        });
    JSON_ARRAY_MAP.put(
        "не равно",
        (actual, param) -> {
          try {
            actual = StringHelper.objectToJsonString(actual);
            JsonAssertions.assertThatJson(actual)
                .when(Option.TREATING_NULL_AS_ABSENT)
                .isArray()
                .isNotEqualTo(param.getExpectedValue());
          } catch (JsonProcessingException e) {
            throw new RuntimeException(
                "Не удалось преобразовать объект в json-строку\n" + e.getMessage(), e);
          } catch (AssertionError e) {
            softAssertions.fail(
                String.format(
                    "Ожидалось, что json-массив, найденный по пути '%s', НЕ равен ожидаемому json'у\n %s\n actual json:\n %s\n "
                        + "expected json: %s\n",
                    param.getPath(), e.getMessage(), actual, param.getExpectedValue()),
                e);
          }
        });
    JSON_ARRAY_MAP.put(
        "содержит",
        (actual, param) -> {
          try {
            @SuppressWarnings("unchecked")
            var expectedList = (List<String>) param.getExpectedValue();
            actual = StringHelper.objectToJsonString(actual);
            JsonAssertions.assertThatJson(actual)
                .whenIgnoringPaths(
                    param.getIgnoringJsonFields() != null
                        ? param.getIgnoringJsonFields()
                        : new String[0])
                .when(Option.IGNORING_ARRAY_ORDER, Option.TREATING_NULL_AS_ABSENT)
                .isArray()
                .containsAll(expectedList);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(
                "Не удалось преобразовать объект в json-строку\n" + e.getMessage(), e);
          } catch (AssertionError e) {
            softAssertions.fail(
                String.format(
                    "Ожидалось, что json-массив, найденный по пути '%s', содержит ожидаемые значения БЕЗ учёта сортировки\n "
                        + "%s\n actual json:\n %s\n expected values: %s\n",
                    param.getPath(), e.getMessage(), actual, param.getExpectedValue()),
                e);
          }
        });
    JSON_ARRAY_MAP.put(
        "не содержит",
        (actual, param) -> {
          try {
            @SuppressWarnings("unchecked")
            var expectedList = (List<String>) param.getExpectedValue();
            actual = StringHelper.objectToJsonString(actual);
            JsonAssertions.assertThatJson(actual)
                .whenIgnoringPaths(
                    param.getIgnoringJsonFields() != null
                        ? param.getIgnoringJsonFields()
                        : new String[0])
                .when(Option.IGNORING_ARRAY_ORDER, Option.TREATING_NULL_AS_ABSENT)
                .isArray()
                .doesNotContainAnyElementsOf(expectedList);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(
                "Не удалось преобразовать объект в json-строку\n" + e.getMessage(), e);
          } catch (AssertionError e) {
            softAssertions.fail(
                String.format(
                    "Ожидалось, что json-массив, найденный по пути '%s', НЕ содержит ожидаемые значения БЕЗ учёта сортировки\n "
                        + "%s\n actual json:\n %s\n expected values: %s\n",
                    param.getPath(), e.getMessage(), actual, param.getExpectedValue()),
                e);
          }
        });
    JSON_ARRAY_MAP.put(
        "содержит только",
        (actual, param) -> {
          try {
            actual = StringHelper.objectToJsonString(actual);
            var objectFromArray = new JSONArray(actual.toString()).opt(0);
            @SuppressWarnings("unchecked")
            var expectedList =
                ((List<Object>) param.getExpectedValue())
                    .stream()
                        .map(
                            e -> {
                              if (objectFromArray == null) {
                                return e;
                              } else if (objectFromArray instanceof Number) {
                                return new BigDecimal(e.toString());
                              } else if (objectFromArray instanceof Boolean) {
                                return Boolean.parseBoolean(e.toString());
                              }
                              return e;
                            })
                        .toList();
            JsonAssertions.assertThatJson(actual)
                .whenIgnoringPaths(
                    param.getIgnoringJsonFields() != null
                        ? param.getIgnoringJsonFields()
                        : new String[0])
                .when(Option.IGNORING_ARRAY_ORDER, Option.TREATING_NULL_AS_ABSENT)
                .isArray()
                .isSubsetOf(expectedList);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(
                "Не удалось преобразовать объект в json-строку\n" + e.getMessage(), e);
          } catch (AssertionError e) {
            softAssertions.fail(
                String.format(
                    "Ожидалось, что json-массив, найденный по пути '%s', содержит только значения из ожидаемого списка\n "
                        + "%s\n actual json:\n %s\n expected values: %s\n",
                    param.getPath(), e.getMessage(), actual, param.getExpectedValue()),
                e);
          }
        });

    JSON_ARRAY_MAP.put(
        "соответствует регулярному выражению",
        (actual, param) -> {
          try {
            var expectedValue = (String) param.getExpectedValue();
            // убираем внешние []
            String expectedPattern = expectedValue.substring(1, expectedValue.length() - 1);
            actual = StringHelper.objectToJsonString(actual);
            JsonAssertions.assertThatJson(actual)
                .whenIgnoringPaths(
                    param.getIgnoringJsonFields() != null
                        ? param.getIgnoringJsonFields()
                        : new String[0])
                .when(Option.IGNORING_ARRAY_ORDER, Option.TREATING_NULL_AS_ABSENT)
                .isArray()
                .allMatch(
                    e -> {
                      Pattern p = Pattern.compile(expectedPattern);
                      Matcher m = p.matcher(e.toString());
                      return m.find();
                    });

          } catch (JsonProcessingException e) {
            throw new RuntimeException(
                "Не удалось преобразовать объект в json-строку\n" + e.getMessage(), e);
          } catch (AssertionError e) {
            softAssertions.fail(
                String.format(
                    "Ожидалось, что json-массив, найденный по пути '%s', содержит значения, которые соответствуют регулярному выражению\n "
                        + "%s\n actual json:\n %s\n expected values: %s\n",
                    param.getPath(), e.getMessage(), actual, param.getExpectedValue()),
                e);
          }
        });

    JSON_ARRAY_MAP.put(
        "имеет формат даты",
        (actual, param) -> {
          try {
            var expectedPattern = (String) param.getExpectedValue();
            actual = StringHelper.objectToJsonString(actual);
            JsonAssertions.assertThatJson(actual)
                .whenIgnoringPaths(
                    param.getIgnoringJsonFields() != null
                        ? param.getIgnoringJsonFields()
                        : new String[0])
                .when(Option.IGNORING_ARRAY_ORDER, Option.TREATING_NULL_AS_ABSENT)
                .isArray()
                .allMatch(
                    e -> {
                      try {
                        DateHelper.parseDateTime(e.toString(), expectedPattern);
                        return true;
                      } catch (DateTimeParseException exp) {
                        softAssertions.fail("Не удалось распарсить дату " + e);
                        return false;
                      }
                    });
          } catch (JsonProcessingException e) {
            throw new RuntimeException(
                "Не удалось преобразовать объект в json-строку\n" + e.getMessage(), e);
          } catch (AssertionError e) {
            softAssertions.fail(
                String.format(
                    "Ожидалось, что json-массив, найденный по пути '%s', содержит значения, которые соответствуют формату даты\n "
                        + "%s\n actual json:\n %s\n expected values: %s\n",
                    param.getPath(), e.getMessage(), actual, param.getExpectedValue()),
                e);
          }
        });
  }

  static void setSoftAssertions(SoftAssertions softAssertions) {
    JsonArrayAsserts.softAssertions = softAssertions;
  }
}
