package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;

import io.cucumber.java.DataTableType;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Модельный класс для создания java-объекта из кукумбер-таблицы параметров, передаваемых в шаге
 * проверки таблицы.
 */
@Getter
@Setter
@NoArgsConstructor // без дефолтного конструктора кукумбер не сможет создать объект
public class TableAssertableConditionParam extends AssertableConditionParam {

  private String columnName;

  public TableAssertableConditionParam(
      String columnName, String type, String condition, String expectedValue) {
    super(null, type, condition, expectedValue, null);
    this.columnName = processValue(columnName);
    this.condition = condition;
  }

  /**
   * Метод для конвертации кукумбер-таблицы параметров шага. Вызывается кукумбером.
   *
   * @param entry карта параметров шага
   * @return объект класса TableAssertableConditionParam, соответствующий переданным параметрам
   */
  @DataTableType
  public TableAssertableConditionParam tableAssertableConditionParamTransformer(
      Map<String, String> entry) {
    return new TableAssertableConditionParam(
        entry.get("Имя столбца"), entry.get("Тип"), entry.get("Условие"), entry.get("Значение"));
  }
}
