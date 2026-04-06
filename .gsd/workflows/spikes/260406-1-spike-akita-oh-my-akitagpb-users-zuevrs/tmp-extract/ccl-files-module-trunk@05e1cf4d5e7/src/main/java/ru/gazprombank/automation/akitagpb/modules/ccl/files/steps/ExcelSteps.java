package ru.gazprombank.automation.akitagpb.modules.ccl.files.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

import io.cucumber.java.ru.И;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.assertj.core.api.Assertions;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.ExcelHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.FileHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.assertions.MainAssert;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels.ExcelAssertableConditionParam;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

/** Шаги для работы с excel-файлами */
public class ExcelSteps extends BaseMethods {

  /**
   * Шаг для проверки, что в excel-файле есть страница с заданным именем
   *
   * @param fileVarOrFilePath переменная сценария, в которую сохранён excel-файл, или путь до
   *     excel-файла
   * @param sheetName имя страницы в excel-файле
   */
  @И("^в excel-файле \"(.*)\" есть страница \"(.*)\"$")
  public void assertExcelFileHasSheet(String fileVarOrFilePath, String sheetName) {
    sheetName = resolveVariables(sheetName);
    File file = FileHelper.getFile(fileVarOrFilePath);
    Workbook workbook = ExcelHelper.getExcelWorkbook(file);
    Sheet sheet = workbook.getSheet(sheetName);
    assertThat(sheet)
        .as(String.format("Страница '%s' в excel-файле '%s' не найдена", sheetName, file.getPath()))
        .isNotNull();
  }

  /**
   * Шаг для сохранения значений из ячеек excel-файла в переменные сценария. Файл в шаг передаётся
   * либо в виде пути к файлу (пример 1), либо в виде переменной, в которую предварительно сохранён
   * сам файл (пример 2). Таблица параметров шага имеет 4 параметра: - Страница - название страницы
   * (листа) в excel-файле, на которой находится нужная ячейка; - Ячейка - адрес ячейки на странице
   * (B17, A15); - Формат - опциональный параметр. Если "да" или если не указывать - будет браться
   * отформатированное excel-ем значение в ячейке. Если "нет" - будет браться значение из строки
   * формулы для данной ячейки; - Переменная - название переменной сценария для сохранения.
   *
   * <p>Примеры: 1. И из excel-файла "${test.data.base.path}/saturn/visualcomposer/${fileName}"
   * значения ячеек сохранены в переменные: | Страница | Ячейка | Формат | Переменная | | uni_import
   * | B17 | да | date1 | | uni_import | A15 | нет | percent2 | 2. И из excel-файла "${file}"
   * значения ячеек сохранены в переменные: | Страница | Ячейка | Переменная | | uni_import | B17 |
   * date1 | | uni_import | A15 | percent2 |
   *
   * @param fileVarOrFilePath переменная сценария, в которую сохранён файл, или путь до файла
   * @param conditions параметры сохранения значений ячеек
   */
  @И("^из excel-файла \"(.*)\" значения ячеек сохранены в переменные:?$")
  public void saveCellValuesFromExcel(
      String fileVarOrFilePath, List<ExcelAssertableConditionParam> conditions) {
    File file = FileHelper.getFile(fileVarOrFilePath);
    Workbook workbook = ExcelHelper.getExcelWorkbook(file);
    conditions.forEach(
        condition -> {
          Object actualValue =
              ExcelHelper.getCellValue(
                  workbook,
                  condition.getSheetName(),
                  condition.getRow(),
                  condition.getColumn(),
                  condition.getWithFormat());
          akitaScenario.setVar(condition.getVarName(), actualValue.toString());
          akitaScenario.log(String.format("Значение %s: %s", condition.getVarName(), actualValue));
        });
  }

  /**
   * Шаг для получения строки из excel-файла по заданным параметрам. Таблица параметров шага имеет 3
   * параметра: - Столбец - имя столбца в excel-таблице; - Формат - опциональный параметр. Если "да"
   * или если не указывать - будет браться отформатированное excel-ем значение в ячейке. Если "нет"
   * - будет браться значение из строки формулы для данной ячейки; - Значение - ожидаемое значение.
   *
   * <p>Пример: И из excel-файла "debug/dossier_report.xlsx" со страницы "Лист1" строка с
   * параметрами из таблицы сохранена в переменную "excelRow" | Столбец | Формат | Значение | | A |
   * да | 10034 | | C | нет | 7722620599 | | F | нет | Первичное досье клиента |
   *
   * @param fileVarOrFilePath переменная сценария, в которую сохранён файл, или путь до файла
   * @param sheetName название стриницы в excel-файле
   * @param varName переменная для сохранения строки
   * @param params параметры поиска строки
   */
  @И(
      "^из excel-файла \"(.*)\" со страницы \"(.*)\" строка с параметрами из таблицы сохранена в переменную \"(.*)\"$")
  public void saveRowFromExcel(
      String fileVarOrFilePath,
      String sheetName,
      String varName,
      List<ExcelAssertableConditionParam> params) {
    File file = FileHelper.getFile(fileVarOrFilePath);
    Workbook workbook = ExcelHelper.getExcelWorkbook(file);
    var row = ExcelHelper.getRowFromExcelByCellValues(workbook, processValue(sheetName), params);
    akitaScenario.setVar(varName, row);
    akitaScenario.log(
        String.format(
            "В переменную сценария %s сохранена excel-строка № %s", varName, row.getRowNum() + 1));
  }

  /**
   * Шаг для сохранения значений из ячеек excel-строки в переменные сценария. Таблица параметров
   * шага имеет 3 параметра: - Столбец - имя столбца в excel-таблице; - Формат - опциональный
   * параметр. Если "да" или если не указывать - будет браться отформатированное excel-ем значение в
   * ячейке. Если "нет" - будет браться значение из строки формулы для данной ячейки; - Переменная -
   * название переменной сценария для сохранения.
   *
   * <p>Пример: И из excel-строки "${excelRow}" значения столбцов сохранены в переменные: | Столбец
   * | Формат | Переменная | | B | нет | var1 | | D | да | var2 | | H | нет | var3 |
   *
   * @param rowVar переменная сценария, в которую сохранена excel-строка
   * @param params параметры сохранения значений ячеек
   */
  @И("^из excel-строки \"(.*)\" значения столбцов сохранены в переменные:?$")
  public void saveCellValuesFromExcelRow(
      String rowVar, List<ExcelAssertableConditionParam> params) {
    var row = (Row) akitaScenario.getVar(rowVar.replace("${", "").replace("}", ""));
    params.forEach(
        e -> {
          var cellValue = ExcelHelper.getCellValue(row, e.getColumn(), e.getWithFormat());
          akitaScenario.setVar(e.getVarName(), cellValue);
          akitaScenario.log(String.format("Значение %s: %s", e.getVarName(), cellValue));
        });
  }

  /**
   * Шаг для проверки переданного excel'а на соответствие условиям из таблицы параметров.
   *
   * <p>Колонки таблицы параметров: - Страница - название страницы (листа) в excel-файле, на которой
   * находится нужная ячейка; - Ячейка - адрес ячейки на странице (B17, A15); - Формат -
   * опциональный параметр. Если "да" или если не указывать - будет браться отформатированное
   * excel-ем значение в ячейке. Если "нет" - будет браться значение из строки формулы для данной
   * ячейки; - Тип - тип значения поля по указанному пути (строка, число, булевый, дата); - Условие
   * - условие, которому должно отвечать значение поля (равно, не равно, а также отдельные условия
   * для каждого Типа; - Значение - ожидаемое значение; - Допуск - допустимое отклонение от
   * ожидаемого значения - для Типов 'число' (тогда допуск - это число: 3 или 5.131) и 'дата' (тогда
   * допуск - одно из значений типа 3s, 7m, 1h, 2d - секунды, минуты, часы или дни).
   *
   * <p>Примеры: 1. И для excel-файла "${test.data.base.path}/saturn/visualcomposer/${fileName}"
   * выполняются условия из таблицы: | Страница | Ячейка | Формат | Тип | Условие | Значение |
   * Допуск | | uni_import | B17 | нет | дата | равно | 02.09.2022 | 25h | | uni_import | A15 | нет
   * | число | равно | 0.11 | 0.01 | | uni_import | I12 | да | число | меньше или равно | ${double1}
   * | | | uni_import | C11 | да | строка | содержит | 8904030331 | | | uni_import | C12 | нет |
   * строка | начинается с | ${inn2.2} | | | uni_import | J12 | нет | строка | равно |
   * =MIN($I12;$X12/120*3,3) | | 2. И для excel-файла "${file}" выполняются условия из таблицы: |
   * Страница | Ячейка | Тип | Условие | Значение | Допуск | | uni_import | C12 | строка |
   * начинается с | ${inn2.2} | | | uni_import | A15 | число | равно | 0.11 | 0.01 | | uni_import |
   * C11 | строка | содержит | 8904030331 | |
   *
   * @param fileVarOrFilePath переменная сценария, в которую сохранён файл, или путь до файла
   * @param conditions список параметров проверок
   */
  @И("^для excel-файла \"(.*)\" выполняются условия из таблицы:?$")
  public void assertExcelCellValues(
      String fileVarOrFilePath, List<ExcelAssertableConditionParam> conditions) {
    File file = FileHelper.getFile(fileVarOrFilePath);
    Workbook workbook = ExcelHelper.getExcelWorkbook(file);

    MainAssert mainAssert = new MainAssert();
    conditions.forEach(
        condition -> {
          Object actualValue =
              ExcelHelper.getCellValue(
                  workbook,
                  condition.getSheetName(),
                  condition.getRow(),
                  condition.getColumn(),
                  condition.getWithFormat());
          mainAssert.getAssertion(condition.getType()).accept(condition, actualValue);
        });
    mainAssert.assertAll();
  }

  /**
   * Шаг для проверки условия в конкретном диапазоне
   *
   * <p>И для диапазона excel-файла "${excel}" выполняются условия из таблицы: | Страница | Начало
   * диапазона | Конец диапазона | Тип | Условие | Значение | | Лист1 | B8 | B20 | дата | имеет
   * формат | dd.MM.yyyy |
   *
   * @param fileVarOrFilePath переменная сценария, в которую сохранён файл, или путь до файла
   * @param conditions список параметров проверок
   */
  @И("^для диапазона excel-файла \"(.*)\" выполняются условия из таблицы:?$")
  public void assertExcelRangeCellValues(
      String fileVarOrFilePath, List<ExcelAssertableConditionParam> conditions) {
    File file = FileHelper.getFile(fileVarOrFilePath);
    Workbook workbook = ExcelHelper.getExcelWorkbook(file);
    MainAssert mainAssert = new MainAssert();
    conditions.forEach(
        condition -> {
          CellRangeAddress cellRangeAddress =
              CellRangeAddress.valueOf(
                  condition.getStartCellRange() + ":" + condition.getEndCellRange());
          cellRangeAddress.forEach(
              cell -> {
                Object actualValue =
                    ExcelHelper.getCellValue(
                        workbook,
                        condition.getSheetName(),
                        cell.getRow(),
                        cell.getColumn(),
                        condition.getWithFormat());
                mainAssert.getAssertion(condition.getType()).accept(condition, actualValue);
              });
        });

    mainAssert.assertAll();
  }

  /**
   * Шаг для проверки ячеек excel-строки на соответствие условиям из таблицы параметров.
   *
   * <p>Колонки таблицы параметров: - Столбец - имя столбца в excel-таблице; - Формат - опциональный
   * параметр. Если "да" или если не указывать - будет браться отформатированное excel-ем значение в
   * ячейке. Если "нет" - будет браться значение из строки формулы для данной ячейки; - Тип - тип
   * значения поля по указанному пути (строка, число, булевый, дата); - Условие - условие, которому
   * должно отвечать значение поля (равно, не равно, а также отдельные условия для каждого Типа; -
   * Значение - ожидаемое значение; - Допуск - допустимое отклонение от ожидаемого значения - для
   * Типов 'число' (тогда допуск - это число: 3 или 5.131) и 'дата' (тогда допуск - одно из значений
   * типа 3s, 7m, 1h, 2d - секунды, минуты, часы или дни).
   *
   * <p>Примеры: 1. И для excel-строки "${excelRow}" выполняются условия из таблицы: | Столбец |
   * Формат | Тип | Условие | Значение | Допуск | | B | нет | дата | равно | 02.09.2022 | 25h | | A
   * | нет | число | равно | 0.11 | 0.01 | | I | да | число | меньше или равно | ${double1} | | | C
   * | да | строка | содержит | 8904030331 | | | C | нет | строка | начинается с | ${inn2.2} | | | J
   * | нет | строка | равно | =MIN($I12;$X12/120*3,3) | |
   *
   * @param rowVar переменная сценария, в которую сохранена excel-строка
   * @param conditions список параметров проверок
   */
  @И("^для excel-строки \"(.*)\" выполняются условия из таблицы:?$")
  public void assertExcelCellValuesFromExcelRow(
      String rowVar, List<ExcelAssertableConditionParam> conditions) {
    var row = (Row) akitaScenario.getVar(rowVar.replace("${", "").replace("}", ""));
    MainAssert mainAssert = new MainAssert();
    conditions.forEach(
        condition -> {
          Object actualValue =
              ExcelHelper.getCellValue(row, condition.getColumn(), condition.getWithFormat());
          mainAssert.getAssertion(condition.getType()).accept(condition, actualValue);
        });
    mainAssert.assertAll();
  }

  /**
   * Шаг для конвертации Excel в json
   *
   * @param fileVarOrFilePath файл или путь до файла экселя
   * @param varName имя переменной в которую сохраняем полученный json
   */
  @И("^содержимое excel-файла \"(.*)\" сохраняем в виде json в переменную \"(.*)\"$")
  public void convertExcelToJson(String fileVarOrFilePath, String varName) {
    File file = FileHelper.getFile(fileVarOrFilePath);
    String value = ExcelHelper.excelToJson(file);

    akitaScenario.log(
        String.format(
            "Конвертируем excel-файл в json-строку и сохраняем в переменную %s", varName));
    akitaScenario.log(String.format("Значение переменной %s = %s", varName, value));
    akitaScenario.setVar(varName, value);
  }

  /**
   * Шаг для преобразования части Excel файла в json При выполнении шага в таблице параметров
   * указывается диапазон ячеек(левая верхняя и нижняя правая ячейка) И часть содержимого
   * excel-файла "${excel}" сохраняем в виде json в переменную "excelJson" | Страница | Начало
   * диапазона | Конец диапазона | Формат | | Лист1 | A7 | I17 | нет |
   */
  @И("^часть содержимого excel-файла \"(.*)\" сохраняем в виде json в переменную \"(.*)\"$")
  public void convertExcelToJson2(
      String fileVarOrFilePath, String varName, ExcelAssertableConditionParam params) {
    File file = FileHelper.getFile(fileVarOrFilePath);
    String value = ExcelHelper.excelPartToJson(file, params);

    akitaScenario.log(
        String.format(
            "Конвертируем часть excel-файл '%s - %s' в json-строку и сохраняем в переменную %s",
            params.getStartCellRange(), params.getEndCellRange(), varName));
    akitaScenario.log(String.format("Значение переменной %s = %s", varName, value));
    akitaScenario.setVar(varName, value);
  }

  /**
   * Шаг для проверки цвета заливки ячейки в таблице Цвет указывается в виде строки формата "R;G;B"
   * где цвета представлены в виде чисел RGB. Можно взять из самой таблицы экселя: у ячейки
   * выпадающее меню цвета заливки - Другие цвета - вкладка Спектр.
   *
   * @param fileVarOrFilePath файл или путь до файла экселя
   * @param sheetName Имя листа в таблице
   * @param cellAddress Адрес ячейки (пр. A1)
   * @param color Цвет ячейки в формате RGB - "123;123;123"
   */
  @И("^в excel-файле \"(.*)\" на странице \"(.*)\" ячейка \"(.*)\" имеет цвет заливки \"(.*)\"$")
  public void saveRowFromExcel(
      String fileVarOrFilePath, String sheetName, String cellAddress, String color) {
    var file = FileHelper.getFile(fileVarOrFilePath);
    var workbook = ExcelHelper.getExcelWorkbook(file);
    var sheet =
        Objects.requireNonNull(
            workbook.getSheet(sheetName), "Страница с названием '" + sheetName + "' не найдена");

    var rowIndex =
        Integer.parseInt(
                Objects.requireNonNull(StringHelper.getRegexpGroupValue(cellAddress, "(\\d+)", 1)))
            - 1;
    var columnIndex =
        ExcelHelper.getColumnIndexFromExcelColumnString(
            StringHelper.getRegexpGroupValue(cellAddress, "([A-Za-z]+)", 1));
    var cellStyle = sheet.getRow(rowIndex).getCell(columnIndex).getCellStyle();

    if (Objects.equals(color, "255;255;255")) { // для белого
      Assertions.assertThat(cellStyle.getFillForegroundColor() == 64)
          .as("Цвет у ячейки %s не соответствует ожидаемому белому", cellAddress);
    }
    var cellColorColor = ((XSSFColor) cellStyle.getFillForegroundColorColor());
    Assertions.assertThat(cellColorColor != null)
        .as("У ячейки %s ожидался цвет %s, но получен белый", cellAddress, color)
        .isTrue();
    var cellColor = cellColorColor.getRGB();

    var byteStream = new ByteArrayOutputStream();
    Arrays.stream(color.split(";"))
        .mapToInt(Integer::parseInt)
        .forEachOrdered(i -> byteStream.write((byte) i));
    var byteArray = byteStream.toByteArray();

    Assertions.assertThat(Arrays.equals(cellColor, byteArray))
        .as("Актуальный цвет у ячейки %s не соответствует ожидаемому %s", cellAddress, color)
        .isTrue();
  }
}
