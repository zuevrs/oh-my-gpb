package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;

import io.cucumber.java.DataTableType;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Модельный класс для получения java-объекта из таблицы параметров кукумбер-шага, реализующего
 * сравнение дат.
 */
@Getter
@Setter
@NoArgsConstructor // без дефолтного конструктора кукумбер не сможет создать объект
public class DateAssertableConditionParam extends AssertableConditionParam {

  private String actualValue;

  /**
   * Конструктор параметра-условия
   *
   * @param condition условие проверки
   * @param expectedValue ожидаемое значение
   */
  public DateAssertableConditionParam(
      String actualValue, String condition, String expectedValue, String offset) {
    super(null, "дата", condition, expectedValue, offset);
    this.actualValue = processValue(actualValue);
  }

  /**
   * Метод для конвертации кукумбер-таблицы параметров шага проверки строк в объект класса.
   * Вызывается кукумбером.
   *
   * @param entry карта параметров шага
   * @return объект класса StringAssertableConditionParam, соответствующий переданным параметрам
   */
  @DataTableType
  public DateAssertableConditionParam stringConditionParamTransformer(Map<String, String> entry) {
    return new DateAssertableConditionParam(
        entry.get("Значение 1"),
        entry.get("Условие"),
        entry.get("Значение 2"),
        entry.get("Допуск"));
  }
}
