package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels;

import io.cucumber.java.DataTableType;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Модельный класс для получения java-объекта из таблицы параметров кукумбер-шага, реализующего
 * сравнение ожидаемых строк с фактическими.
 */
@Getter
@Setter
@NoArgsConstructor // без дефолтного конструктора кукумбер не сможет создать объект
public class StringAssertableConditionParam extends AssertableConditionParam {

  /**
   * Конструктор параметра-условия
   *
   * @param condition условие проверки
   * @param expectedValue ожидаемое значение
   */
  public StringAssertableConditionParam(String condition, String expectedValue) {
    super(null, "строка", condition, expectedValue, null);
  }

  /**
   * Метод для конвертации кукумбер-таблицы параметров шага проверки строк в объект класса.
   * Вызывается кукумбером.
   *
   * @param entry карта параметров шага
   * @return объект класса StringAssertableConditionParam, соответствующий переданным параметрам
   */
  @DataTableType
  public StringAssertableConditionParam stringConditionParamTransformer(Map<String, String> entry) {
    return new StringAssertableConditionParam(entry.get("Условие"), entry.get("Значение"));
  }
}
