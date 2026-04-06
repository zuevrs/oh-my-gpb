package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.tables.PdfTable;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.tables.TableExtractionAlgorithmFactory;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.tables.extractionalgorithms.AbstractTableExtractionAlgorithm;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.api.AkitaScenario;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.RectangularTextContainer;
import technology.tabula.TextChunk;

public class PDFHelper {

  /**
   * Преобразование pdf файла в изображение
   *
   * @param pdfFile pdf файл
   * @return сконвертированное изображение
   */
  @SneakyThrows
  public static BufferedImage convertPdfToImage(File pdfFile) {
    PDDocument document = PDDocument.load(pdfFile);
    PDFRenderer pdfRenderer = new PDFRenderer(document);

    int pageCount = document.getNumberOfPages();
    BufferedImage firstPage = pdfRenderer.renderImageWithDPI(0, 300);
    int width = firstPage.getWidth();
    int height = firstPage.getHeight();

    BufferedImage resultImage =
        new BufferedImage(width, pageCount * height, BufferedImage.TYPE_INT_RGB);

    for (int indexPage = 0; indexPage < document.getNumberOfPages(); indexPage++) {
      BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(indexPage, 300);
      resultImage.getGraphics().drawImage(bufferedImage, 0, indexPage * height, null);
    }

    return resultImage;
  }

  /**
   * Получить текст со страницы PDF-файла из заданного по координатам участка
   *
   * @param page страница PDF-файла
   * @param x координата X начала прямоугольного участка
   * @param y координата Y начала прямоугольного участка
   * @param width ширина прямоугольного участка
   * @param height высота прямоугольного участка
   * @return текст из указанного прямоугольного участка
   */
  public static String getTextFromPdfPageByArea(
      Page page, double x, double y, double width, double height) {
    try {
      PDFTextStripperByArea stripper = new PDFTextStripperByArea();
      stripper.setSortByPosition(true);
      Rectangle rectangle = new Rectangle((int) x, (int) y, (int) width, (int) height);
      stripper.addRegion("region", rectangle);
      stripper.extractRegions(page.getPDPage());
      return stripper.getTextForRegion("region");
    } catch (IOException e) {
      throw new RuntimeException("Ошибка при получении текст из PDF-файла по координатам", e);
    }
  }

  /**
   * Получить таблицу из PDF-файла в виде списка списков строк по её индексу (порядковый номер
   * таблицы во всём PDF-файле минус 1). В данном шаге таблицы, которые не помещаются на одной
   * странице, соединяются в одну, т.к. таблица берётся со всего файла, а не с отдельной страницы.
   *
   * @param pdfFile PDF-файл
   * @param index индекс таблицы в файле (порядковый номер таблицы во всём PDF-файле минус 1)
   * @param pdfDesign условное название дизайна ПДЙ-файла
   * @return таблица из PDF-файла в виде списка списков строк
   */
  public static List<List<String>> getTable(File pdfFile, Integer index, String pdfDesign) {
    var tables = extractAllTablesFromPdfFile(pdfFile, pdfDesign);
    assertThat(tables).as("Таблиц в PDF-файле не найдено").hasSizeGreaterThan(0);
    var table = index == null ? tables.get(tables.size() - 1) : tables.get(index);
    AkitaScenario.getInstance().log(String.format("Таблица: %n%s", table));
    return convertPdfTableToList(table);
  }

  /**
   * Получить таблицу со страницы PDF-файла в виде списка списков строк по её индексу (порядковый
   * номер таблицы на данной странице файла минус 1). Если таблица не помещается на одной странице,
   * то данный шаг вернёт только часть этой таблицы с указанной страницы файла.
   *
   * @param pdfFile PDF-файл
   * @param pageNumber номер страницы (начиная с 1), на которой искать таблицу
   * @param index индекс таблицы на странице (порядковый номер таблицы на данной странице файла
   *     минус 1)
   * @param pdfDesign условное название дизайна ПДЙ-файла
   * @return таблица из PDF-файла в виде списка списков строк
   */
  public static List<List<String>> getTableFromPage(
      File pdfFile, int pageNumber, Integer index, String pdfDesign) {
    var tables = extractOnePageTablesFromPdfFile(pdfFile, pageNumber, pdfDesign);
    assertThat(tables)
        .as(String.format("Таблиц в PDF-файле на странице %s не найдено", pageNumber))
        .hasSizeGreaterThan(0);
    var table = index == null ? tables.get(tables.size() - 1) : tables.get(index);
    AkitaScenario.getInstance().log(String.format("Таблица: %n%s", table));
    return convertPdfTableToList(table);
  }

  /**
   * Получить таблицу из PDF-файла в виде списка списков строк, найденную после указанного текста
   * (обычно - заголовка таблицы). Переданный в параметре текст перед таблицей проверяется с
   * фактическим на contains в последних 10 строках текста. В данном шаге таблицы, которые не
   * помещаются на одной странице, соединяются в одну, т.к. таблица берётся со всего файла, а не с
   * отдельной страницы.
   *
   * @param pdfFile PDF-файл
   * @param textBeforeTable текст, после которого должна быть найдена ожидаемая таблица
   * @param pdfDesign условное название дизайна ПДЙ-файла
   * @return таблица из PDF-файла в виде списка списков строк
   */
  public static List<List<String>> getTableAfterTextFromPdfFile(
      File pdfFile, String textBeforeTable, String pdfDesign) {
    var tables =
        extractAllTablesFromPdfFile(pdfFile, pdfDesign).stream()
            .filter(e -> e.getTextBeforeTable().contains(textBeforeTable))
            .collect(Collectors.toList());
    AkitaScenario.getInstance()
        .log(
            String.format("После текста '%s' найдено таблиц - %s", textBeforeTable, tables.size()));
    int i = 1;
    for (PdfTable table : tables) {
      AkitaScenario.getInstance().log(String.format("Таблица %s: %n%s", i++, table));
    }
    assertThat(tables)
        .as(
            String.format(
                "По данному тексту число найденных таблиц в PDF-файле не равно 1 (%s). Проверьте файл и уточните текст для поиска",
                tables.size()))
        .hasSize(1);
    return convertPdfTableToList(tables.get(0));
  }

  /**
   * Получить все таблицы из PDF-файла. Таблицы, которые не помещаются на одной странице,
   * соединяются в одну.
   *
   * @param file PDF-файл
   * @param pdfDesign условное название дизайна ПДЙ-файла
   * @return таблицы из PDF-файла
   */
  private static List<PdfTable> extractAllTablesFromPdfFile(File file, String pdfDesign) {
    try (PDDocument document = PDDocument.load(file)) {
      AbstractTableExtractionAlgorithm algorithm =
          TableExtractionAlgorithmFactory.getAlgorithm(pdfDesign);
      PageIterator pi = new ObjectExtractor(document).extract();
      List<PdfTable> tables = new ArrayList<>();
      while (pi.hasNext()) {
        Page page = pi.next();
        List<PdfTable> onePageTables = algorithm.extractPage(page);
        fillAdditionalTablesParams(onePageTables, page);
        tables.addAll(onePageTables);
      }
      tables = mergeSeparatedTables(tables);
      AkitaScenario.getInstance()
          .log(String.format("Всего в PDF-файле найдено таблиц - %s", tables.size()));
      return tables;
    } catch (IOException e) {
      throw new RuntimeException("Ошибка при получении таблиц из PDF-файла", e);
    }
  }

  /**
   * Получить таблицы с одной страницы PDF-файла. Если таблица не помещается на одной странице, то
   * данный шаг вернёт только часть этой таблицы.
   *
   * @param file PDF-файл
   * @param pageNumber номер страницы (начиная с 1)
   * @param pdfDesign условное название дизайна ПДЙ-файла
   * @return таблицы с указанной страницы PDF-файла
   */
  private static List<PdfTable> extractOnePageTablesFromPdfFile(
      File file, int pageNumber, String pdfDesign) {
    try (PDDocument document = PDDocument.load(file)) {
      AbstractTableExtractionAlgorithm algorithm =
          TableExtractionAlgorithmFactory.getAlgorithm(pdfDesign);
      Page page = new ObjectExtractor(document).extract(pageNumber);
      List<PdfTable> onePageTables = algorithm.extractPage(page);
      fillAdditionalTablesParams(onePageTables, page);
      AkitaScenario.getInstance()
          .log(
              String.format(
                  "В PDF-файле на странице %s найдено таблиц - %s",
                  pageNumber, onePageTables.size()));
      return onePageTables;
    } catch (IOException e) {
      throw new RuntimeException("Ошибка при получении таблиц из PDF-файла", e);
    }
  }

  /**
   * Преобразовать таблицу из объекта PdfTable в список списков строк
   *
   * @param table объект PdfTable
   * @return таблица в виде списка списков строк
   */
  private static List<List<String>> convertPdfTableToList(PdfTable table) {
    return table.getRows().stream()
        .map(
            list ->
                list.stream()
                    // Note: Cell.getText() uses \r to concat text chunks
                    .map(PDFHelper::getCellText)
                    .collect(Collectors.toList()))
        .collect(Collectors.toList());
  }

  /**
   * Вспомогательный метод который получает значение ячейки
   *
   * @param cell ячейка из pdf документа
   * @return склеенное из честей значение ячейки
   */
  public static String getCellText(RectangularTextContainer cell) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < cell.getTextElements().size(); i++) {
      result.append(((TextChunk) (cell.getTextElements().get(i))).getText()).append(" ");
    }
    return result.toString().replace("\r", " ").trim();
  }

  /**
   * Соединить таблицы, которые не поместилтись на одной странице. Логика: если последняя таблица на
   * одной странице не имеет какого-либо текста после себя, и таблица на следующей странице не имеет
   * текста перед собой - то это одна таблица, и эта следующая таблица соединяется с предыдущей.
   *
   * @param tables список таблиц до соединеня
   * @return соединённые таблицы
   */
  private static List<PdfTable> mergeSeparatedTables(List<PdfTable> tables) {
    List<PdfTable> result = new ArrayList<>();
    for (int i = tables.size() - 1; i > 0; i--) {
      var current = tables.get(i);
      var previous = tables.get(i - 1);
      if (current.getPageNumber() == previous.getPageNumber() + 1
          && !previous.hasAnyTextAfterTable()
          && current.getTextBeforeTable().isBlank()) {
        previous.getRows().addAll(current.getRows());
      } else {
        result.add(0, current);
      }
    }
    if (!tables.isEmpty()) {
      result.add(0, tables.get(0));
    }
    return result;
  }

  /**
   * Заполнить дополнительные поля объектов таблиц: - найти и установить текст, расположенный перед
   * каждой таблицей (последние 10 строк); - найти и установить значение hasAnyTextAfterTable -
   * имеет ли последняя таблица текст после себя.
   *
   * @param onePageTables список таблиц на одной странице
   * @param page объект этой страницы
   */
  private static void fillAdditionalTablesParams(List<PdfTable> onePageTables, Page page) {
    if (onePageTables == null || onePageTables.isEmpty()) {
      return;
    }
    setTextBeforeFirstTable(onePageTables, page);
    setTextBeforeOtherTables(onePageTables, page);
    setLastTableHasAnyTextAfter(onePageTables, page);
  }

  /**
   * Найти и установить текст, расположенный перед первой таблицей на странице (последние 10 строк)
   *
   * @param onePageTables список таблиц на одной странице
   * @param page объект этой страницы
   */
  private static void setTextBeforeFirstTable(List<PdfTable> onePageTables, Page page) {
    PdfTable firstTable = onePageTables.get(0);
    var text =
        getTextFromPdfPageByArea(
            page, 0, 0, firstTable.getX() + firstTable.getWidth() + 50, firstTable.getY());
    firstTable.setTextBeforeTable(StringHelper.getLastParts(text, 10, "\r\n"));
  }

  /**
   * Найти и установить текст, расположенный перед всеми таблицами на странице, кроме первой
   * (последние 10 строк)
   *
   * @param onePageTables список таблиц на одной странице
   * @param page объект этой страницы
   */
  private static void setTextBeforeOtherTables(List<PdfTable> onePageTables, Page page) {
    for (int i = 1; i < onePageTables.size(); i++) {
      PdfTable previous = onePageTables.get(i - 1);
      PdfTable t = onePageTables.get(i);
      var text =
          getTextFromPdfPageByArea(
              page,
              t.getX() - 50,
              previous.getY() + previous.getHeight(),
              t.getWidth() + 100,
              t.getY() - (previous.getY() + previous.getHeight()));
      t.setTextBeforeTable(StringHelper.getLastParts(text, 10, "\r\n"));
    }
  }

  /**
   * Найти и установить значение hasAnyTextAfterTable - имеет ли последняя таблица на странице текст
   * после себя. Если текст после таблицы - это только номер старницы (число 1, 2, ...) - тогда
   * значение hasAnyTextAfterTable также будет false.
   *
   * @param onePageTables список таблиц на одной странице
   * @param page объект этой страницы
   */
  private static void setLastTableHasAnyTextAfter(List<PdfTable> onePageTables, Page page) {
    PdfTable lastTable = onePageTables.get(onePageTables.size() - 1);
    String textAfterLastTable =
        getTextFromPdfPageByArea(
            page,
            lastTable.getX() - 50,
            lastTable.getY() + lastTable.getHeight(),
            lastTable.getWidth() + 100,
            1000);
    lastTable.setHasAnyTextAfterTable(
        !textAfterLastTable.isBlank() && !textAfterLastTable.matches("(\\s+)?\\d+(\\s+)?"));
  }
}
