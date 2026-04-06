package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;

import io.cucumber.java.DataTableType;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Модельный класс для получения java-объекта из таблицы параметров кукумбер-шага, реализующего
 * сравнение строковых полей из лога Кибаны.
 */
@Getter
@Setter
@NoArgsConstructor // без дефолтного конструктора кукумбер не сможет создать объект
public class KibanaLogFieldAssertableConditionParam extends AssertableConditionParam {

  private String field;

  /**
   * Конструктор параметра-условия
   *
   * @param field "Поле" или "Параметр" из лога Кибаны
   * @param condition условие проверки
   * @param expectedValue ожидаемое значение
   */
  public KibanaLogFieldAssertableConditionParam(
      String field, String condition, String expectedValue) {
    super(null, "строка", condition, expectedValue, null);
    this.field = processValue(field);
  }

  /**
   * Метод для конвертации кукумбер-таблицы параметров шага проверки строковых полей из лога Кибаны
   * в объект класса. Вызывается кукумбером.
   *
   * @param entry карта параметров шага
   * @return объект класса KibanaLogFieldAssertableConditionParam, соответствующий переданным
   *     параметрам
   */
  @DataTableType
  public KibanaLogFieldAssertableConditionParam kibanaLogFieldConditionParamTransformer(
      Map<String, String> entry) {
    return new KibanaLogFieldAssertableConditionParam(
        entry.containsKey("Поле") ? entry.get("Поле") : entry.get("Параметр"),
        entry.get("Условие"),
        entry.get("Значение"));
  }
}
