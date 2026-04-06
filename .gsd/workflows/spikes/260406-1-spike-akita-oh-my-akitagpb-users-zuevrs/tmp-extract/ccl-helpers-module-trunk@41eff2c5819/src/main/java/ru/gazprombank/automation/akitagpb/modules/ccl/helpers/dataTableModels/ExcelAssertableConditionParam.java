package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

import io.cucumber.java.DataTableType;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.poi.ss.util.CellAddress;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.ExcelHelper;

/**
 * Модельный класс для получения java-объекта из таблицы параметров кукумбер-шага, реализующего
 * сравнение ожидаемых значений с фактическими, полученными из excel-файла.
 */
@Getter
@Setter
@NoArgsConstructor // без дефолтного конструктора кукумбер не сможет создать объект
public class ExcelAssertableConditionParam extends AssertableConditionParam {

  private String sheetName;
  private Integer row;
  private Integer column;
  private Boolean withFormat;
  private String varName;
  private String startCellRange;
  private String endCellRange;

  public ExcelAssertableConditionParam(
      String sheetName,
      String cellAddress,
      String format,
      String varName,
      String type,
      String condition,
      String expectedValue,
      String offset,
      String columnName,
      String startCellRange,
      String endCellRange) {
    super(
        String.format("%s:%s, format=%s", sheetName, cellAddress, format),
        type,
        condition,
        expectedValue,
        offset);

    this.sheetName = sheetName;
    this.withFormat = format == null || format.equalsIgnoreCase("да");
    this.varName = varName;
    this.startCellRange = startCellRange;
    this.endCellRange = endCellRange;
    if (columnName == null) {
      CellAddress address = new CellAddress(cellAddress);
      this.row = address.getRow();
      this.column = address.getColumn();
    } else {
      this.column = ExcelHelper.getColumnIndexFromExcelColumnString(columnName);
    }
  }

  /**
   * Метод для конвертации кукумбер-таблицы параметров шага проверки excel'а в объект класса.
   * Вызывается кукумбером.
   *
   * @param map карта параметров шага
   * @return объект класса ExcelConditionParam, соответствующий переданным параметрам
   */
  @DataTableType
  public ExcelAssertableConditionParam excelConditionParamTransformer(Map<String, String> map) {
    Map<String, String> entry =
        map.entrySet().stream()
            .filter(e -> e.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> resolveVariables(e.getValue())));
    return new ExcelAssertableConditionParam(
        entry.get("Страница"),
        entry.get("Ячейка"),
        entry.get("Формат"),
        entry.get("Переменная"),
        entry.get("Тип"),
        entry.get("Условие"),
        entry.get("Значение"),
        entry.get("Допуск"),
        entry.get("Столбец"),
        entry.get("Начало диапазона"),
        entry.get("Конец диапазона"));
  }
}
