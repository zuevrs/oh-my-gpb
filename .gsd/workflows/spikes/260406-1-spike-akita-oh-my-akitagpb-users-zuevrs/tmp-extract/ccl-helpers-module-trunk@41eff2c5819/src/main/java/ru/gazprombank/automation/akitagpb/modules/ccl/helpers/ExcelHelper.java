package ru.gazprombank.automation.akitagpb.modules.ccl.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels.ExcelAssertableConditionParam;

/** Утилитный класс для работы с excel-файлами */
public class ExcelHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExcelHelper.class);

  /**
   * Получить объект Workbook из excel-файла (.xlsx или .xls)
   *
   * @param file excel-файл (.xlsx или .xls)
   * @return объект Workbook
   */
  public static Workbook getExcelWorkbook(File file) {
    try {
      return WorkbookFactory.create(file);
    } catch (IOException e) {
      throw new RuntimeException(
          "Ошибка при получении java-объекта Workbook из excel-файла " + file.getPath(), e);
    }
  }

  /**
   * Получить числовой индекс колонки по буквенному индексу из excel'а (в excel'е колонки
   * пронумерованы буквами - A, B, C.. Z, AA, AB... - нужно получить числовой индекс - 0, 1, 2...)
   *
   * @param columnValue буквенный индекс колонки в excel'е
   * @return числовой индекс колонки
   */
  public static Integer getColumnIndexFromExcelColumnString(String columnValue) {
    if (columnValue == null || columnValue.isEmpty()) {
      throw new RuntimeException(
          "Буквенный номер столбца имеет некорректное значение: '" + columnValue + "'");
    }
    int result = 0;
    var chars = columnValue.toUpperCase().toCharArray();
    for (int i = 0; i < chars.length; i++) {
      int code = chars[chars.length - 1 - i] - 'A' + 1;
      code *= Math.pow(26, i);
      result += code;
    }
    return result - 1;
  }

  /**
   * Метод для получения строки из excel-файла по заданным параметрам.
   *
   * @param workbook объект Workbook из excel-файла
   * @param sheetName название страницы в excel-файле
   * @param params параметры поиска строки
   * @return найденная строка
   */
  public static Row getRowFromExcelByCellValues(
      Workbook workbook, String sheetName, List<ExcelAssertableConditionParam> params) {
    Sheet sheet =
        Objects.requireNonNull(
            workbook.getSheet(sheetName), "Страница с названием '" + sheetName + "' не найдена");
    for (int i = 0; i < sheet.getLastRowNum() + 1; i++) {
      Row row = sheet.getRow(i);
      boolean isFound = true;
      for (var param : params) {
        var actualValue = getCellValue(row, param.getColumn(), param.getWithFormat());
        isFound = Objects.equals(actualValue, param.getExpectedValue());
        if (!isFound) {
          LOGGER.debug(
              String.format(
                  "Строка № %s не соответствует параметрам:%nЯчейка в столбце [index=%s] равна [%s]. Ожидалось - [%s]",
                  i + 1, param.getColumn(), actualValue, param.getExpectedValue()));
          break;
        }
      }
      if (isFound) {
        LOGGER.debug(String.format("Найдена строка № %s", i + 1));
        return row;
      }
    }
    throw new RuntimeException("Строка excel-таблицы по заданным параметрам не найдена");
  }

  /**
   * Получить значение из ячейки.
   *
   * @param workbook объект Workbook из excel-файла
   * @param sheetName имя страницы
   * @param rowIndex индекс строки ячейки
   * @param columnIndex индекс колонки ячейки
   * @param withFormat если true - получить значение в ячейки, отформатированное excel'ем; если
   *     false - получить значение из строки формулы для данной ячейки
   * @return значение из ячейки
   */
  public static Object getCellValue(
      Workbook workbook,
      String sheetName,
      Integer rowIndex,
      Integer columnIndex,
      boolean withFormat) {
    Sheet sheet =
        Objects.requireNonNull(
            workbook.getSheet(sheetName), "Страница с названием '" + sheetName + "' не найдена");
    Row row = sheet.getRow(rowIndex);
    return getCellValue(row, columnIndex, withFormat);
  }

  /**
   * Получить значение из ячейки.
   *
   * @param row excel-строка
   * @param columnIndex индекс колонки ячейки
   * @param withFormat если true - получить значение в ячейки, отформатированное excel'ем; если
   *     false - получить значение из строки формулы для данной ячейки
   * @return значение из ячейки
   */
  public static Object getCellValue(Row row, Integer columnIndex, boolean withFormat) {
    if (row == null) {
      return null;
    }
    Cell cell = row.getCell(columnIndex);
    if (cell == null) {
      return null;
    }
    DataFormatter formatter = new DataFormatter(Locale.getDefault());

    switch (cell.getCellType()) {
      case STRING:
        return withFormat ? formatter.formatCellValue(cell) : cell.getStringCellValue();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          return getDateCellValue(cell, withFormat, formatter);
        } else {
          return withFormat
              ? formatter.formatCellValue(cell).replaceAll("[\u00A0\\s]+", "")
              : cell.getNumericCellValue();
        }
      case FORMULA:
        if (cell.getCachedFormulaResultType() == CellType.BOOLEAN) {
          return withFormat ? cell.getBooleanCellValue() : "=" + cell.getCellFormula();
        } else if (DateUtil.isCellDateFormatted(cell)) {
          return getDateCellValue(cell, withFormat, formatter);
        } else if (withFormat) {
          var cellStyle = cell.getCellStyle();
          return formatter
              .formatRawCellContents(
                  cell.getNumericCellValue(),
                  cellStyle.getDataFormat(),
                  cellStyle.getDataFormatString())
              .replaceAll("[\u00A0\\s]+", "");
        } else {
          return "=" + cell.getCellFormula().replace(",", ";").replace(".", ",");
        }
      case BOOLEAN:
        return withFormat ? formatter.formatCellValue(cell) : cell.getBooleanCellValue();
      case BLANK:
        return "";
      default:
        throw new RuntimeException("Неопределённый тип данных ячейки.");
    }
  }

  /**
   * Получить дату из ячейки. Если withFormat = true - метод вернёт дату, отформатированную excel'ем
   * в данной яычейке. Если withFormat = false: если в значении даты указано время - метод вернёт
   * эту дату в формате "dd.MM.yyyy HH:mm:ss", иначе - в формате "dd.MM.yyyy".
   *
   * @param cell объект ячейки
   * @param withFormat если true - получить значение в ячейки, отформатированное excel'ем; если
   *     false - получить значение из строки формулы для * данной ячейки
   * @param formatter объект DataFormatter для получения отформатированного excel'ем значения
   * @return дата из ячейки
   */
  private static String getDateCellValue(Cell cell, boolean withFormat, DataFormatter formatter) {
    if (withFormat) {
      return formatter.formatCellValue(cell);
    } else {
      var dateTime = cell.getLocalDateTimeCellValue();
      return dateTime.toLocalTime().equals(LocalTime.MIDNIGHT)
          ? dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
          : dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }
  }

  /**
   * Метод для преобразования excel в json
   *
   * @param excel файл экселя
   * @return строку в виду json
   */
  public static String excelToJson(File excel) {
    // hold the excel data sheet wise
    ObjectMapper mapper = new ObjectMapper();

    ObjectNode excelData = mapper.createObjectNode();
    FileInputStream fis = null;
    Workbook workbook = null;
    try {
      // Creating file input stream
      fis = new FileInputStream(excel);

      String filename = excel.getName().toLowerCase();
      if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
        // creating workbook object based on excel file format
        if (filename.endsWith(".xls")) {
          workbook = new HSSFWorkbook(fis);
        } else {
          workbook = new XSSFWorkbook(fis);
        }

        // Reading each sheet one by one
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
          Sheet sheet = workbook.getSheetAt(i);
          String sheetName = sheet.getSheetName();

          List<String> headers = new ArrayList<>();
          ArrayNode sheetData = mapper.createArrayNode();
          // Reading each row of the sheet
          for (int j = 0; j <= sheet.getLastRowNum(); j++) {
            Row row = sheet.getRow(j);
            if (j == 0) {
              // reading sheet header's name
              for (int k = 0; k < row.getLastCellNum(); k++) {
                headers.add(row.getCell(k).getStringCellValue());
              }
            } else {
              // reading work sheet data
              ObjectNode rowData = mapper.createObjectNode();
              for (int k = 0; k < headers.size(); k++) {
                Cell cell = row.getCell(k);
                String headerName = headers.get(k);
                if (cell != null) {
                  switch (cell.getCellType()) {
                    case FORMULA -> rowData.put(headerName, ((XSSFCell) cell).getRawValue());
                    case BOOLEAN -> rowData.put(headerName, cell.getBooleanCellValue());
                    case NUMERIC -> rowData.put(headerName, cell.getNumericCellValue());
                    case BLANK -> rowData.put(headerName, "");
                    default -> rowData.put(headerName, cell.getStringCellValue());
                  }
                } else {
                  rowData.put(headerName, "");
                }
              }
              sheetData.add(rowData);
            }
          }
          excelData.set(sheetName, sheetData);
        }
        return excelData.toPrettyString();
      } else {
        throw new IllegalArgumentException("File format not supported.");
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (workbook != null) {
        try {
          workbook.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }

  /**
   * Метод преобразования части Excel файла в json-строку
   *
   * @param excel файл
   * @param params параметры шага
   * @return json-строка
   */
  public static String excelPartToJson(File excel, ExcelAssertableConditionParam params) {

    CellAddress startCellAddress = new CellAddress(params.getStartCellRange());
    CellAddress endCellAddress = new CellAddress(params.getEndCellRange());
    int startIndexColumn = startCellAddress.getColumn();
    int endIndexColumn = endCellAddress.getColumn();
    int startIndexRow = startCellAddress.getRow();
    int endIndexRow = endCellAddress.getRow();
    boolean withFormat = params.getWithFormat();

    // hold the excel data sheet wise
    ObjectMapper mapper = new ObjectMapper();

    ObjectNode excelData = mapper.createObjectNode();
    FileInputStream fis = null;
    Workbook workbook = null;
    try {
      // Creating file input stream
      fis = new FileInputStream(excel);

      String filename = excel.getName().toLowerCase();
      if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
        // creating workbook object based on excel file format
        if (filename.endsWith(".xls")) {
          workbook = new HSSFWorkbook(fis);
        } else {
          workbook = new XSSFWorkbook(fis);
        }
        // Reading each sheet one by one
        Sheet sheet;
        if (params.getSheetName() != null) {
          sheet = workbook.getSheet(params.getSheetName());
        } else {
          sheet = workbook.getSheetAt(0);
        }
        String sheetName = sheet.getSheetName();

        List<String> headers = new ArrayList<>();
        ArrayNode sheetData = mapper.createArrayNode();
        // Reading each row of the sheet
        for (int j = startIndexRow; j <= endIndexRow; j++) {
          Row row = sheet.getRow(j);
          if (j == startIndexRow) {
            // reading sheet header's name
            for (int k = startIndexColumn; k <= endIndexColumn; k++) {
              headers.add(getCellValue(row, k, withFormat).toString());
            }
          } else {
            // reading work sheet data
            ObjectNode rowData = mapper.createObjectNode();
            for (int k = startIndexColumn; k <= endIndexColumn; k++) {
              Cell cell = row.getCell(k);
              String headerName = headers.get(k);
              if (cell != null) {
                switch (cell.getCellType()) {
                  case FORMULA -> rowData.put(headerName, ((XSSFCell) cell).getRawValue());
                  case BOOLEAN -> rowData.put(headerName, cell.getBooleanCellValue());
                  case NUMERIC -> rowData.put(
                      headerName,
                      BigDecimal.valueOf(row.getCell(k).getNumericCellValue()).toString());
                  case BLANK -> rowData.put(headerName, "");
                  default -> rowData.put(headerName, cell.getStringCellValue());
                }
              } else {
                rowData.put(headerName, "");
              }
            }
            sheetData.add(rowData);
          }
        }
        excelData.set(sheetName, sheetData);
        return excelData.toPrettyString();
      } else {
        throw new IllegalArgumentException("File format not supported.");
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (workbook != null) {
        try {
          workbook.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }
}
