package ru.gazprombank.automation.akitagpb.modules.ccl.files.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.И;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.ListHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.CustomPDFTextStripper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.PDFHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.PDFTextParameter;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class PDFSteps extends BaseMethods {

  /**
   * Шаг проверяет, содержится или отсутствует текст в PDF файле. Проверка выполняется по всему
   * тексту PDF файла и не учитывает шрифты..
   *
   * <p>Для варианта шага со словами "текст" будет выполнена замена всех символов переноса на
   * символы пробела, и убраны все двойные пробелы.
   *
   * <p>Для варианта шага со словом "текст с переносом строк" проверка будет выполнятся на исходном
   * тексте, в котором сохранены все символы переноса строк.
   *
   * @param varName имя переменной содержащей PDF файл или путь до PDF файла.
   * @param appear флаг проверки содержания или отсутствия текста.
   * @param newLine флаг проверки в исходном тексте или тексте без переноса строк.
   * @param text текст, наличие или отсутствие которого проверяется.
   */
  @И("^в PDF файле \"(.+)\" (содержится|отсутствует) (текст|текст с переносом строк) \"(.+)\"$")
  public void containsText(String varName, String appear, String newLine, String text) {
    var pdf =
        akitaScenario.tryGetVar(StringHelper.getVarName(varName)) != null
            ? CustomPDFTextStripper.parsePDFFile(
                (File) akitaScenario.getVar(StringHelper.getVarName(varName)))
            : CustomPDFTextStripper.parsePDFFile(resolveVariables(varName));
    String resolveText = processValue(text);
    String pdfTextContent =
        newLine.equals("текст")
            ? pdf.getTextContent().replaceAll("[\\t\\n\\r]", " ").replaceAll("\\s{2,}", " ").strip()
            : pdf.getTextContent();
    pdf.closeDocument();
    if ("содержится".equals(appear)) {
      assertThat(pdfTextContent)
          .as("PDF файл %s не содержит текст: %s", pdf.getFilePath(), resolveText)
          .contains(resolveText);
    } else {
      assertThat(pdfTextContent)
          .as("PDF файл %s содержит текст: %s", pdf.getFilePath(), resolveText)
          .doesNotContain(resolveText);
    }
  }

  /**
   * Шаг преобразования pdf-файла в изображение
   *
   * @param pdfVar переменная в хранилище или путь до pdf-файла
   * @param imageVarName имя сохряняемой переменной
   */
  @SneakyThrows
  @И("^преобразовать PDF файл \"(.+)\" в изображение и сохранить в переменную \"(.+)\"$")
  public void convertPDF(String pdfVar, String imageVarName) {
    File pdfFile =
        akitaScenario.tryGetVar(StringHelper.getVarName(pdfVar)) != null
            ? (File) akitaScenario.getVar(StringHelper.getVarName(pdfVar))
            : new File(resolveVariables(pdfVar));

    BufferedImage resultImage = PDFHelper.convertPdfToImage(pdfFile);
    akitaScenario.setVar(resolveVariables(imageVarName), resultImage);
  }

  /**
   * Шаг проверяет, содержится или отсутствует строка содержащая текст в PDF файле. Проверка
   * выполняется построчно по всему тексту PDF файла и не учитывает шрифты.
   *
   * <p>
   *
   * @param varName имя переменной содержащей PDF файл или путь до PDF файла.
   * @param appear флаг проверки содержания или отсутствия текста.
   * @param text текст, наличие или отсутствие которого проверяется.
   */
  @И("^в PDF файле \"(.+)\" (содержится|отсутствует) текст без переноса строк \"(.+)\"$")
  public void containsLineText(String varName, String appear, String text) {
    var pdf =
        akitaScenario.tryGetVar(StringHelper.getVarName(varName)) != null
            ? CustomPDFTextStripper.parsePDFFile(
                (File) akitaScenario.getVar(StringHelper.getVarName(varName)))
            : CustomPDFTextStripper.parsePDFFile(resolveVariables(varName));
    String resolveText = processValue(text);
    pdf.closeDocument();
    boolean find =
        Arrays.stream(pdf.getTextContent().split("\\r\\n"))
            .anyMatch(str -> str.contains(resolveText));
    if ("содержится".equals(appear)) {
      assertThat(find)
          .as("PDF файл %s не содержит текст: %s", pdf.getFilePath(), resolveText)
          .isTrue();
    } else {
      assertThat(find)
          .as("PDF файл %s содержит текст: %s", pdf.getFilePath(), resolveText)
          .isFalse();
    }
  }

  /**
   * Шаг проверяет, содержится или отсутствует текст c определёнными параметрами в PDF файле. Для
   * проверки текста с параметрами используются срезу текста, в которых заранее убраны переносы
   * строк.
   *
   * <p>Параметры, которые можно проверить:<br>
   * Текст (обязательный) - текст, который нужно проверить.<br>
   * Шрифт (опциональный) - имя шрифта.<br>
   * Курсив (опциональный) - должен ли быть текст записан курсивом.<br>
   * Жирный (опционально) - должен ли быть текст записан жирным шрифтом.<br>
   *
   * <p>Параметры указываются в виде таблицы. Пример:<br>
   * | Текст | Курсив | Жирный | Шрифт |<br>
   * | ${text} | true | | |<br>
   * | «04» мая 2023 года | | true | TimesNewRoman |<br>
   *
   * @param varName имя переменной содержащей PDF файл или путь до PDF файла.
   * @param appear флаг проверки содержания или отсутствия текста.
   * @param parameters таблица с параметрами текста для проверки.
   */
  @И("^в PDF файле \"(.+)\" (содержится|отсутствует) текст c параметрами$")
  public void containsTextWithParameters(
      String varName, String appear, List<PDFTextParameter> parameters) {
    SoftAssertions softAssertions = new SoftAssertions();
    var pdf =
        akitaScenario.tryGetVar(StringHelper.getVarName(varName)) != null
            ? CustomPDFTextStripper.parsePDFFile(
                (File) akitaScenario.getVar(StringHelper.getVarName(varName)))
            : CustomPDFTextStripper.parsePDFFile(resolveVariables(varName));
    boolean appearFlag = "содержится".equals(appear);
    for (var parameter : parameters) {
      String assertDescription =
          appearFlag
              ? String.format(
                  "В PDF файле %s нет текста с параметрами:\n%s", pdf.getFilePath(), parameter)
              : String.format(
                  "В PDF файле %s есть текст с параметрами:\n%s", pdf.getFilePath(), parameter);
      softAssertions
          .assertThat(pdf.getPdfContent().stream().anyMatch(parameter::checkTextSlice))
          .as(assertDescription)
          .isEqualTo(appearFlag);
    }
    pdf.closeDocument();
    softAssertions.assertAll();
  }

  /**
   * Шаг для проверки таблицы из PDF-файла на равенство таблице, переданной в параметрах. Таблица в
   * файле ищется по тексту перед ней.
   *
   * @param varName имя переменной, в которой сохранён PDF-файл
   * @param textBeforeTable текст перед таблицей
   * @param table ожидаемая таблица
   */
  @И("^в PDF файле \"(.+)\" таблица, найденная после текста \"(.+)\", равна:?$")
  public void assertDefaultDesignPdfTableEquals(
      String varName, String textBeforeTable, DataTable table) {
    assertPdfTableEquals(varName, null, textBeforeTable, table);
  }

  @И("^в PDF файле \"(.+)\" (с синим дизайном) таблица, найденная после текста \"(.+)\", равна:?$")
  public void assertOtherDesignPdfTableEquals(
      String varName, String pdfDesign, String textBeforeTable, DataTable table) {
    assertPdfTableEquals(varName, pdfDesign, textBeforeTable, table);
  }

  private void assertPdfTableEquals(
      String varName, String pdfDesign, String textBeforeTable, DataTable table) {
    File pdfFile = (File) akitaScenario.getVar(varName);
    List<List<String>> expected = resolveVarsAtPdfTablesExpectedParams(table.asLists());
    textBeforeTable = processValue(textBeforeTable);
    var actual = PDFHelper.getTableAfterTextFromPdfFile(pdfFile, textBeforeTable, pdfDesign);
    assertPdfTablesEquals(expected, actual);
  }

  /**
   * Шаг для проверки, что таблица из PDF-файла содержит строки, переданные в параметрах. Таблица в
   * файле ищется по тексту перед ней.
   *
   * @param varName имя переменной, в которой сохранён PDF-файл
   * @param textBeforeTable текст перед таблицей
   * @param table ожидаемые строки таблицы
   */
  @И("^в PDF файле \"(.+)\" таблица, найденная после текста \"(.+)\", содержит строки:?$")
  public void assertDefaultDesignPdfTableContainsRows(
      String varName, String textBeforeTable, DataTable table) {
    assertPdfTableContainsRows(varName, null, textBeforeTable, table);
  }

  @И(
      "^в PDF файле \"(.+)\" (с синим дизайном) таблица, найденная после текста \"(.+)\", содержит строки:?$")
  public void assertOtherDesignPdfTableContainsRows(
      String varName, String pdfDesign, String textBeforeTable, DataTable table) {
    assertPdfTableContainsRows(varName, pdfDesign, textBeforeTable, table);
  }

  private void assertPdfTableContainsRows(
      String varName, String pdfDesign, String textBeforeTable, DataTable table) {
    File pdfFile = (File) akitaScenario.getVar(varName);
    List<List<String>> expected = resolveVarsAtPdfTablesExpectedParams(table.asLists());
    textBeforeTable = processValue(textBeforeTable);
    var actual = PDFHelper.getTableAfterTextFromPdfFile(pdfFile, textBeforeTable, pdfDesign);
    assertPdfTableContainsRows(expected, actual);
  }

  /**
   * Шаг для проверки таблицы из PDF-файла на равенство таблице, переданной в параметрах. Таблица в
   * файле ищется по порядковому номеру.
   *
   * @param varName имя переменной, в которой сохранён PDF-файл
   * @param firstOrLastTable "первая таблица" -> индекс = 0; "последняя таблица" -> индекс = null
   * @param index порядковый номер таблицы в файле (начиная с 1) -> индекс таблицы = порядковыйй
   *     номер - 1
   * @param table ожидаемая таблица
   */
  @И(
      "^в PDF файле \"(.+)\" (?:(первая|последняя) таблица|таблица с порядковым номером (\\d+)) равна:?$")
  public void assertDefaultDesignPdfTableWithIndexEquals(
      String varName, String firstOrLastTable, Integer index, DataTable table) {
    assertPdfTableWithIndexEquals(varName, null, firstOrLastTable, index, table);
  }

  @И(
      "^в PDF файле \"(.+)\" (с синим дизайном) (?:(первая|последняя) таблица|таблица с порядковым номером (\\d+)) равна:?$")
  public void assertOtherDesignPdfTableWithIndexEquals(
      String varName, String pdfDesign, String firstOrLastTable, Integer index, DataTable table) {
    assertPdfTableWithIndexEquals(varName, pdfDesign, firstOrLastTable, index, table);
  }

  private void assertPdfTableWithIndexEquals(
      String varName, String pdfDesign, String firstOrLastTable, Integer index, DataTable table) {
    File pdfFile = (File) akitaScenario.getVar(varName);
    List<List<String>> expected = resolveVarsAtPdfTablesExpectedParams(table.asLists());
    index = getTableIndex(firstOrLastTable, index);
    var actual = PDFHelper.getTable(pdfFile, index, pdfDesign);
    assertPdfTablesEquals(expected, actual);
  }

  /**
   * Шаг для проверки, что таблица из PDF-файла содержит строки, переданные в параметрах. Таблица в
   * файле ищется по порядковому номеру.
   *
   * @param varName имя переменной, в которой сохранён PDF-файл
   * @param firstOrLastTable "первая таблица" -> индекс = 0; "последняя таблица" -> индекс = null
   * @param index порядковый номер таблицы в файле (начиная с 1) -> индекс таблицы = порядковыйй
   *     номер - 1
   * @param table ожидаемые строки таблицы
   */
  @И(
      "^в PDF файле \"(.+)\" (?:(первая|последняя) таблица|таблица с порядковым номером (\\d+)) содержит строки:?$")
  public void assertDefaultDesignPdfTableWithIndexContainsRows(
      String varName, String firstOrLastTable, Integer index, DataTable table) {
    assertPdfTableWithIndexContainsRows(varName, null, firstOrLastTable, index, table);
  }

  @И(
      "^в PDF файле \"(.+)\" (с синим дизайном) (?:(первая|последняя) таблица|таблица с порядковым номером (\\d+)) содержит строки:?$")
  public void assertOtherDesignPdfTableWithIndexContainsRows(
      String varName, String pdfDesign, String firstOrLastTable, Integer index, DataTable table) {
    assertPdfTableWithIndexContainsRows(varName, pdfDesign, firstOrLastTable, index, table);
  }

  private void assertPdfTableWithIndexContainsRows(
      String varName, String pdfDesign, String firstOrLastTable, Integer index, DataTable table) {
    File pdfFile = (File) akitaScenario.getVar(varName);
    List<List<String>> expected = resolveVarsAtPdfTablesExpectedParams(table.asLists());
    index = getTableIndex(firstOrLastTable, index);
    var actual = PDFHelper.getTable(pdfFile, index, pdfDesign);
    assertPdfTableContainsRows(expected, actual);
  }

  /**
   * Шаг для проверки таблицы с указанной страницы PDF-файла на равенство таблице, переданной в
   * параметрах. Таблица ищется по порядковому номеру данной таблицы на указанной странице.
   *
   * @param varName имя переменной, в которой сохранён PDF-файл
   * @param firstOrLastTable "первая таблица" -> индекс = 0; "последняя таблица" -> индекс = null
   * @param index порядковый номер таблицы на странице (начиная с 1) -> индекс таблицы = порядковыйй
   *     номер - 1
   * @param pageNumber номер страницы в PDF-файле (начиная с 1)
   * @param table ожидаемая таблица
   */
  @И(
      "^в PDF файле \"(.+)\" (?:(первая|последняя) таблица|таблица с порядковым номером (\\d+)) на странице (\\d+) равна:?$")
  public void assertDefaultDesignPdfTableFromPageEquals(
      String varName, String firstOrLastTable, Integer index, Integer pageNumber, DataTable table) {
    assertPdfTableFromPageEquals(varName, null, firstOrLastTable, index, pageNumber, table);
  }

  @И(
      "^в PDF файле \"(.+)\" (с синим дизайном) (?:(первая|последняя) таблица|таблица с порядковым номером (\\d+)) на странице (\\d+) равна:?$")
  public void assertOtherDesignPdfTableFromPageEquals(
      String varName,
      String pdfDesign,
      String firstOrLastTable,
      Integer index,
      Integer pageNumber,
      DataTable table) {
    assertPdfTableFromPageEquals(varName, pdfDesign, firstOrLastTable, index, pageNumber, table);
  }

  private void assertPdfTableFromPageEquals(
      String varName,
      String pdfDesign,
      String firstOrLastTable,
      Integer index,
      Integer pageNumber,
      DataTable table) {
    File pdfFile = (File) akitaScenario.getVar(varName);
    List<List<String>> expected = resolveVarsAtPdfTablesExpectedParams(table.asLists());
    index = getTableIndex(firstOrLastTable, index);
    var actual = PDFHelper.getTableFromPage(pdfFile, pageNumber, index, pdfDesign);
    assertPdfTablesEquals(expected, actual);
  }

  /**
   * Шаг для проверки, что таблица с указанной страницы PDF-файла содержит строки, переданные в
   * параметрах. Таблица ищется по порядковому номеру данной таблицы на указанной странице.
   *
   * @param varName имя переменной, в которой сохранён PDF-файл
   * @param firstOrLastTable "первая таблица" -> индекс = 0; "последняя таблица" -> индекс = null
   * @param index порядковый номер таблицы на странице (начиная с 1) -> индекс таблицы = порядковыйй
   *     номер - 1
   * @param pageNumber номер страницы в PDF-файле (начиная с 1)
   * @param table ожидаемые строки таблицы
   */
  @И(
      "^в PDF файле \"(.+)\" (?:(первая|последняя) таблица|таблица с порядковым номером (\\d+)) на странице (\\d+) содержит строки:?$")
  public void asserDefaultDesigntPdfTableFromPageContainsRows(
      String varName, String firstOrLastTable, Integer index, Integer pageNumber, DataTable table) {
    assertPdfTableFromPageContainsRows(varName, null, firstOrLastTable, index, pageNumber, table);
  }

  @И(
      "^в PDF файле \"(.+)\" (с синим дизайном) (?:(первая|последняя) таблица|таблица с порядковым номером (\\d+)) на странице (\\d+) содержит строки:?$")
  public void assertOtherDesignPdfTableFromPageContainsRows(
      String varName,
      String pdfDesign,
      String firstOrLastTable,
      Integer index,
      Integer pageNumber,
      DataTable table) {
    assertPdfTableFromPageContainsRows(
        varName, pdfDesign, firstOrLastTable, index, pageNumber, table);
  }

  private void assertPdfTableFromPageContainsRows(
      String varName,
      String pdfDesign,
      String firstOrLastTable,
      Integer index,
      Integer pageNumber,
      DataTable table) {
    File pdfFile = (File) akitaScenario.getVar(varName);
    List<List<String>> expected = resolveVarsAtPdfTablesExpectedParams(table.asLists());
    index = getTableIndex(firstOrLastTable, index);
    var actual = PDFHelper.getTableFromPage(pdfFile, pageNumber, index, pdfDesign);
    assertPdfTableContainsRows(expected, actual);
  }

  private void assertPdfTablesEquals(List<List<String>> expected, List<List<String>> actual) {
    assertThat(actual)
        .as("Таблицы не равны")
        .usingRecursiveFieldByFieldElementComparator(
            RecursiveComparisonConfiguration.builder()
                .withEqualsForType(String::equalsIgnoreCase, String.class)
                .build())
        .isEqualTo(expected);
  }

  private void assertPdfTableContainsRows(List<List<String>> expected, List<List<String>> actual) {
    assertThat(ListHelper.toLowerCase(actual))
        .as("Таблица не содержит ожидаемые строки")
        .containsAll(ListHelper.toLowerCase(expected));
  }

  private List<List<String>> resolveVarsAtPdfTablesExpectedParams(List<List<String>> table) {
    return table.stream()
        .map(
            list ->
                list.stream()
                    .map(
                        e -> {
                          e = e == null ? "" : processValue(e);
                          e =
                              e.equalsIgnoreCase("true")
                                  ? "да"
                                  : e.equalsIgnoreCase("false") ? "нет" : e;
                          return e;
                        })
                    .collect(Collectors.toList()))
        .collect(Collectors.toList());
  }

  private Integer getTableIndex(String firstOrLastRow, Integer index) {
    return firstOrLastRow != null
        ? firstOrLastRow.startsWith("последн") ? null : 0
        : (Integer) (index - 1);
  }
}
