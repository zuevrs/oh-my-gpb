package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels;

import io.cucumber.java.DataTableType;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.date.DateCalculator;

/**
 * Модельный класс для создания java-объекта из кукумбер-таблицы параметров, передаваемых в
 * шаге-калькуляторе дат.
 */
@Getter
@Setter
@NoArgsConstructor // без дефолтного конструктора кукумбер не сможет создать объект
public class DateCalculatorParam {

  private String originDate;
  private String action;
  private Object diffValue;
  private String format;
  private String varName;

  public DateCalculatorParam(
      String originDate, String action, String diffValue, String format, String varName) {
    this.originDate = originDate;
    this.action = action;
    this.diffValue = diffValue;
    if (diffValue != null) {
      diffValue = StringHelper.processValue(diffValue);
      if (diffValue.matches("(((?:\\d+)|(?:\\$\\{[A-Za-z\\d_-]+}))[yYMdDhHmsS]\\s?){1,}")) {
        this.diffValue = DateCalculator.getChronoUnitsMap(diffValue);
      }
    }
    this.format = format;
    this.varName = varName;
  }

  /**
   * Метод для конвертации кукумбер-таблицы параметров шага-калькулятора дат. Вызывается кукумбером.
   *
   * @param entry карта параметров шага
   * @return объект класса DateCalculatorParam, соответствующий переданным параметрам
   */
  @DataTableType
  public DateCalculatorParam dateCalculatorParamTransformer(Map<String, String> entry) {
    return new DateCalculatorParam(
        entry.get("Значение 1"),
        entry.get("Действие"),
        entry.get("Значение 2"),
        entry.get("Формат"),
        entry.get("Переменная"));
  }
}
