package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import net.javacrumbs.jsonunit.core.Option;
import net.minidev.json.JSONArray;
import org.assertj.core.api.SoftAssertions;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels.JsonAssertableConditionParam;

/** Класс для проверки полей, содержащих json-объекты */
public class JsonObjectAsserts {

  static final Map<String, BiConsumer<Object, JsonAssertableConditionParam>> JSON_OBJECT_MAP =
      new HashMap<>();
  private static SoftAssertions softAssertions;

  static {
    JSON_OBJECT_MAP.put(
        "равно",
        (actual, param) -> {
          try {
            actual =
                actual.getClass().equals(JSONArray.class)
                    ? StringHelper.objectToJsonString(((JSONArray) actual).get(0))
                    : StringHelper.objectToJsonString(actual);
            JsonAssertions.assertThatJson(actual)
                .whenIgnoringPaths(
                    param.getIgnoringJsonFields() != null
                        ? param.getIgnoringJsonFields()
                        : new String[0])
                .when(Option.TREATING_NULL_AS_ABSENT)
                .isEqualTo(param.getExpectedValue());
          } catch (JsonProcessingException e) {
            throw new RuntimeException(
                "Не удалось преобразовать объект в json-строку\n" + e.getMessage(), e);
          } catch (AssertionError e) {
            softAssertions.fail(
                String.format(
                    "Ожидалось, что json-объект, найденный по пути '%s', равен ожидаемому json'у\n %s\n actual json:\n %s\n "
                        + "expected json: %s\n",
                    param.getPath(), e.getMessage(), actual, param.getExpectedValue()),
                e);
          }
        });
    JSON_OBJECT_MAP.put(
        "равно без учёта сортировки",
        (actual, param) -> {
          try {
            actual =
                actual.getClass().equals(JSONArray.class)
                    ? StringHelper.objectToJsonString(((JSONArray) actual).get(0))
                    : StringHelper.objectToJsonString(actual);
            JsonAssertions.assertThatJson(actual)
                .whenIgnoringPaths(
                    param.getIgnoringJsonFields() != null
                        ? param.getIgnoringJsonFields()
                        : new String[0])
                .when(Option.IGNORING_ARRAY_ORDER, Option.TREATING_NULL_AS_ABSENT)
                .isEqualTo(param.getExpectedValue());
          } catch (JsonProcessingException e) {
            throw new RuntimeException("Не удалось преобразовать объект в json-строку", e);
          } catch (AssertionError e) {
            softAssertions.fail(
                String.format(
                    "Ожидалось, что json-объект, найденный по пути '%s', равен ожидаемому json'у БЕЗ учёта сортировки\n %s\n "
                        + "actual json:\n %s\n expected json: %s\n",
                    param.getPath(), e.getMessage(), actual, param.getExpectedValue()),
                e);
          }
        });
    JSON_OBJECT_MAP.put(
        "равно без учёта значений",
        (actual, param) -> {
          try {
            actual =
                actual.getClass().equals(JSONArray.class)
                    ? StringHelper.objectToJsonString(((JSONArray) actual).get(0))
                    : StringHelper.objectToJsonString(actual);
            JsonAssertions.assertThatJson(actual)
                .when(Option.IGNORING_VALUES)
                .isEqualTo(param.getExpectedValue());
          } catch (JsonProcessingException e) {
            throw new RuntimeException("Не удалось преобразовать объект в json-строку", e);
          } catch (AssertionError e) {
            softAssertions.fail(
                String.format(
                    "Ожидалось, что json-объект, найденный по пути '%s', равен ожидаемому json'у БЕЗ учёта значений\n %s\n "
                        + "actual json:\n %s\n expected json: %s\n",
                    param.getPath(), e.getMessage(), actual, param.getExpectedValue()),
                e);
          }
        });
    JSON_OBJECT_MAP.put(
        "не равно",
        (actual, param) -> {
          try {
            actual =
                actual.getClass().equals(JSONArray.class)
                    ? StringHelper.objectToJsonString(((JSONArray) actual).get(0))
                    : StringHelper.objectToJsonString(actual);
            JsonAssertions.assertThatJson(actual)
                .when(Option.TREATING_NULL_AS_ABSENT)
                .isNotEqualTo(param.getExpectedValue());
          } catch (JsonProcessingException e) {
            throw new RuntimeException(
                "Не удалось преобразовать объект в json-строку\n" + e.getMessage(), e);
          } catch (AssertionError e) {
            softAssertions.fail(
                String.format(
                    "Ожидалось, что json-объект, найденный по пути '%s', НЕ равен ожидаемому json'у\n %s\n actual json:\n %s\n "
                        + "expected json: %s\n",
                    param.getPath(), e.getMessage(), actual, param.getExpectedValue()),
                e);
          }
        });
  }

  static void setSoftAssertions(SoftAssertions softAssertions) {
    JsonObjectAsserts.softAssertions = softAssertions;
  }
}
