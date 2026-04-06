package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.assertions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.assertj.core.api.SoftAssertions;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels.AssertableConditionParam;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels.JsonAssertableConditionParam;

/**
 * Класс, главный для ассёртов. Все взаимодействия с данными ассёртами происходят через объект
 * данного класса.
 */
public class MainAssert {

  private static final Map<String, BiConsumer<AssertableConditionParam, Object>> ROUTER =
      new HashMap<>();
  private SoftAssertions softAssertions;

  static {
    ROUTER.put(
        "строка",
        (param, actualValue) ->
            StringAsserts.STRING_MAP
                .get(param.getCondition())
                .accept(actualValue.toString(), param.getExpectedValue()));
    ROUTER.put(
        "null",
        (param, actualValue) -> NullAsserts.NULL_MAP.get(param.getCondition()).accept(actualValue));
    ROUTER.put(
        "число",
        (param, actualValue) ->
            NumberAsserts.NUMBER_MAP
                .get(param.getCondition())
                .accept(parseNumber(actualValue.toString()), param));
    ROUTER.put(
        "булевый",
        (param, actualValue) ->
            BooleanAsserts.BOOLEAN_MAP
                .get(param.getCondition())
                .accept(
                    Boolean.parseBoolean(actualValue.toString()),
                    Boolean.parseBoolean(param.getExpectedValue().toString())));
    ROUTER.put(
        "json-объект",
        (param, actualValue) ->
            JsonObjectAsserts.JSON_OBJECT_MAP
                .get(param.getCondition())
                .accept(actualValue, (JsonAssertableConditionParam) param));
    ROUTER.put(
        "json-массив",
        (param, actualValue) ->
            JsonArrayAsserts.JSON_ARRAY_MAP
                .get(param.getCondition())
                .accept(actualValue, (JsonAssertableConditionParam) param));
    ROUTER.put(
        "дата",
        (param, actualValue) ->
            DateTimeAsserts.DATE_TIME_MAP
                .get(param.getCondition())
                .accept(actualValue.toString(), param));
  }

  public MainAssert() {
    initSoftAssertions();
  }

  /**
   * Получить объект BiConsumer для выполнения ассёрта по конкретному типу данных (число, строка и
   * т.д.)
   *
   * @param type тип данных ("число", "строка", "null", "булевый", "json-объект", "json-массив",
   *     "дата")
   * @return объект BiConsumer для выполнения ассёрта
   */
  public BiConsumer<AssertableConditionParam, Object> getAssertion(String type) {
    return ROUTER.get(type);
  }

  /** Выполнить все проверки soft-assert'а в конце */
  public void assertAll() {
    softAssertions.assertAll();
  }

  /** Инициализировать soft-assert'ы во всех классах-ассёртах */
  private void initSoftAssertions() {
    softAssertions = new SoftAssertions();
    StringAsserts.setSoftAssertions(softAssertions);
    NullAsserts.setSoftAssertions(softAssertions);
    NumberAsserts.setSoftAssertions(softAssertions);
    BooleanAsserts.setSoftAssertions(softAssertions);
    JsonObjectAsserts.setSoftAssertions(softAssertions);
    JsonArrayAsserts.setSoftAssertions(softAssertions);
    DateTimeAsserts.setSoftAssertions(softAssertions);
  }

  public static Double parseNumber(String value) {
    return Double.parseDouble(value.replaceAll("[\u00A0\\s]+", ""));
  }
}
