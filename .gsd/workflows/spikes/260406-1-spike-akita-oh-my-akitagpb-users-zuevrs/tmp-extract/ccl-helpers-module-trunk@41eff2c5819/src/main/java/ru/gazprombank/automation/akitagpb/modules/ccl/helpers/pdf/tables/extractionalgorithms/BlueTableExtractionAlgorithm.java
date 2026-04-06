package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.tables.extractionalgorithms;

import java.util.ArrayList;
import java.util.List;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.tables.PdfTable;
import technology.tabula.Cell;
import technology.tabula.Page;
import technology.tabula.Rectangle;
import technology.tabula.Ruling;
import technology.tabula.TextElement;
import technology.tabula.Utils;

/**
 * Класс для получения таблиц из PDF-файла, имеющего синий дизайн ГПБ. В таком формате нет чётких
 * визуальных границ ячеек (вертикальных границ).
 */
public class BlueTableExtractionAlgorithm extends AbstractTableExtractionAlgorithm {

  /** Extract a list of Table from page using rulings as separators */
  @Override
  public List<PdfTable> extract(Page page, List<Ruling> rulings) {
    // split rulings into horizontal and vertical
    List<Ruling> horizontalR = new ArrayList<>();
    List<Ruling> verticalR = new ArrayList<>();

    for (Ruling r : rulings) {
      if (r.horizontal()) {
        horizontalR.add(r);
      } else if (r.vertical()) {
        verticalR.add(r);
      }
    }
    horizontalR = Ruling.collapseOrientedRulings(horizontalR);
    verticalR = Ruling.collapseOrientedRulings(verticalR);

    List<Cell> cells = findCells(horizontalR, verticalR);
    List<Rectangle> spreadsheetAreas = findSpreadsheetsFromCells(cells);

    List<PdfTable> spreadsheets = new ArrayList<>();
    for (Rectangle area : spreadsheetAreas) {

      List<Cell> overlappingCells = new ArrayList<>();
      for (Cell c : cells) {
        if (c.intersects(area)) {

          c.setTextElements(TextElement.mergeWords(page.getText(c)));
          overlappingCells.add(c);
        }
      }

      List<Ruling> horizontalOverlappingRulings = new ArrayList<>();
      for (Ruling hr : horizontalR) {
        if (area.intersectsLine(hr)) {
          horizontalOverlappingRulings.add(hr);
        }
      }
      List<Ruling> verticalOverlappingRulings = new ArrayList<>();
      for (Ruling vr : verticalR) {
        if (area.intersectsLine(vr)) {
          verticalOverlappingRulings.add(vr);
        }
      }

      PdfTable t =
          new PdfTable(
              area,
              overlappingCells,
              horizontalOverlappingRulings,
              verticalOverlappingRulings,
              this);
      t.setPageNumber(page.getPageNumber());
      removeEmptyTextChunks(t);
      spreadsheets.addAll(getBluePdfFormatTables(t));
    }
    Utils.sort(spreadsheets, RECTANGLE_COMPARATOR);
    return spreadsheets;
  }

  /**
   * Если ПДФ файл имеет новый синий дизайн, то оригинальная либа tabula распарсивает в одну таблицу
   * всю ПДФ-страницу целиком, вмести с любым текстовым содержимым этой страницы. Данный метод
   * выделяет из этой одной целой "таблицы" реальные таблицы, если они там есть.
   *
   * @param table страница из ПДФ-файла, преобразованная в таблицу библиотекой tabula
   * @return список реальных таблиц с этой страницы
   */
  private List<PdfTable> getBluePdfFormatTables(PdfTable table) {
    List<PdfTable> result = new ArrayList<>();
    int fromRow;
    int toRow = 0;
    while (toRow != table.getRows().size()) {
      fromRow = getFirstRowWithoutEmptyCellsSequenceIndex(table, toRow);
      toRow = getRowWithEmptyCellsSequenceIndex(table, fromRow);
      toRow = toRow == -1 ? table.getRows().size() : toRow;
      result.add(PdfTable.getSubTable(table, fromRow, toRow));
    }
    return result;
  }

  /**
   * В первоначально полученной таблице PdfTable table могут быть реальные таблицы, а могцт быть
   * просто строки с текстом. В строках реальных таблиц нет более одной пустой ячейки подряд. Данный
   * метод ищет индекс первой встречной такой строки в первоначальной таблице, начиная с указанного
   * индекса.
   *
   * @param table первоначально полученная таблица PdfTable
   * @param fromRow индекс, с какой строки начинать искать
   * @return индекс первой найденной строки таблицы, не имеющей более одной пустой ячейки подряд
   *     (или -1)
   */
  private int getFirstRowWithoutEmptyCellsSequenceIndex(PdfTable table, int fromRow) {
    var rows = table.getRows();
    for (int i = fromRow; i < rows.size(); i++) {
      var row = rows.get(i);
      for (int j = 0; j < row.size() - 1; j++) {
        if (row.get(j).getText().isEmpty()
            && (row.size() == 1 || row.get(j + 1).getText().isEmpty())) {
          break;
        }
        if (j == row.size() - 2) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Метод для нахождения индекса строки таблицы, у которой есть более одной пустой строки подряд.
   * Такая строка не является реальной таблицей для ПДФ-файла с новым синим дизайном.
   *
   * @param table первоначально полученная таблица PdfTable
   * @param fromRow ндекс, с какой строки начинать искать
   * @return индекс первой найденной строки таблицы, имеющей более одной пустой ячейки подряд (или
   *     -1)
   */
  private int getRowWithEmptyCellsSequenceIndex(PdfTable table, int fromRow) {
    var rows = table.getRows();
    for (int i = fromRow; i < rows.size(); i++) {
      var row = rows.get(i);
      for (int j = 0; j < row.size() - 1; j++) {
        if (row.get(j).getClass().equals(Cell.class)
            && row.get(j).getText().isEmpty()
            && (row.size() == 1
                || row.get(j).getClass().equals(Cell.class)
                    && row.get(j + 1).getText().isEmpty())) {
          return i;
        }
      }
    }
    return -1;
  }
}
