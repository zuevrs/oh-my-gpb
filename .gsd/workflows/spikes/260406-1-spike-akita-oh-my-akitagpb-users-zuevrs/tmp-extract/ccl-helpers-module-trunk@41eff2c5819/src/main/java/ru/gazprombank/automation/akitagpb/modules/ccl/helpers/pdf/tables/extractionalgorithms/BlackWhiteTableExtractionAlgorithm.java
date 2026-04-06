package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.tables.extractionalgorithms;

import java.util.ArrayList;
import java.util.List;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.tables.PdfTable;
import technology.tabula.Cell;
import technology.tabula.Page;
import technology.tabula.Rectangle;
import technology.tabula.Ruling;
import technology.tabula.TextChunk;
import technology.tabula.TextElement;
import technology.tabula.Utils;

/**
 * Класс для получения таблиц из PDF-файла со стандартными черно-белыми таблицами, имеющими чёткие
 * визуальные границы ячеек.
 */
public class BlackWhiteTableExtractionAlgorithm extends AbstractTableExtractionAlgorithm {

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
      correctTable(t);
      removeEmptyTextChunks(t);
      t.setPageNumber(page.getPageNumber());
      spreadsheets.add(t);
    }
    Utils.sort(spreadsheets, RECTANGLE_COMPARATOR);
    return spreadsheets;
  }

  /**
   * В одном из форматов таблиц название таблицы над самой таблицей распарсивается в первый ряд
   * таблицы. Данный метод удаляет такой ряд из таблицы и устанавливает этой таблице
   * скорректированный размер.
   *
   * @param table таблица
   */
  private void correctTable(PdfTable table) {
    if (!table.getRows().isEmpty()
        && table.getRows().get(0).stream().anyMatch(e -> e.getClass().equals(TextChunk.class))) {
      var row = table.getRows().remove(0);
      var rowHeight =
          row.stream()
              .filter(e -> e.getClass().equals(Cell.class))
              .findFirst()
              .orElseThrow(() -> new RuntimeException("Не найдено ячеек в данном ряду"))
              .getHeight();
      table.setRect(
          table.getX(), table.getY() + rowHeight, table.getWidth(), table.getHeight() - rowHeight);
    }
  }
}
