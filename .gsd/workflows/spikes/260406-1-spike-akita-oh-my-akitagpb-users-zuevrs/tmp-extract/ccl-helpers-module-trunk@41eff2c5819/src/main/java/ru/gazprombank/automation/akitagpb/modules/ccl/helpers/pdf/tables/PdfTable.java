package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.tables;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.PDFHelper.getCellText;

import java.util.List;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.tables.extractionalgorithms.BlueTableExtractionAlgorithm;
import technology.tabula.Cell;
import technology.tabula.Rectangle;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Ruling;
import technology.tabula.TableWithRulingLines;
import technology.tabula.extractors.ExtractionAlgorithm;

public class PdfTable extends TableWithRulingLines {

  private Integer pageNumber;
  private String textBeforeTable;
  private Boolean hasAnyTextAfterTable;

  public PdfTable(
      Rectangle area,
      List<Cell> cells,
      List<Ruling> horizontalRulings,
      List<Ruling> verticalRulings,
      ExtractionAlgorithm extractionAlgorithm) {
    super(area, cells, horizontalRulings, verticalRulings, extractionAlgorithm);
  }

  /**
   * Получить таблицу на основе таблицы из параметра метода, начиная со строки с индексом fromRow,
   * до строки с индексом toRow.
   *
   * @param table первоначальная таблица
   * @param fromRow индекс первой строки новой таблицы
   * @param toRow индекс последней строки новой таблицы
   * @return новая таблица со строки fromRow до строки toRow
   */
  public static PdfTable getSubTable(PdfTable table, int fromRow, int toRow) {
    var result =
        new PdfTable(
            new Rectangle(), List.of(), List.of(), List.of(), new BlueTableExtractionAlgorithm());
    var newTableRows = table.getRows().subList(fromRow, toRow);
    // убираем все пустые ячейки - они не являются ячейками таблицы, в новом формате при отсутствии
    // визуальных границ ячейки пустой строки в
    // ячейке не предполагается
    newTableRows =
        newTableRows.stream()
            .map(e -> e.stream().filter(cell -> !cell.getText().isEmpty()).toList())
            .toList();
    result.getRows().addAll(newTableRows);
    var firstRowCell = result.getRows().get(0).get(0);
    var x = firstRowCell.getX();
    var y = firstRowCell.getY();
    var width = table.getWidth();
    var lastRowCell = result.getRows().get(result.getRows().size() - 1).get(0);
    var height = lastRowCell.getMaxY() - y;
    result.setRect(x, y, width, height);
    result.setPageNumber(table.getPageNumber());
    return result;
  }

  public Integer getPageNumber() {
    return pageNumber;
  }

  public void setPageNumber(Integer pageNumber) {
    this.pageNumber = pageNumber;
  }

  public String getTextBeforeTable() {
    return textBeforeTable;
  }

  public void setTextBeforeTable(String textBeforeTable) {
    this.textBeforeTable = textBeforeTable;
  }

  public Boolean hasAnyTextAfterTable() {
    return hasAnyTextAfterTable;
  }

  public void setHasAnyTextAfterTable(Boolean hasAnyTextAfterTable) {
    this.hasAnyTextAfterTable = hasAnyTextAfterTable;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Страница: ").append(pageNumber).append(System.lineSeparator());
    for (List<RectangularTextContainer> cells : this.getRows()) {
      sb.append("| ");
      for (RectangularTextContainer content : cells) {
        sb.append(getCellText(content)).append(" | ");
      }
      sb.append(System.lineSeparator());
    }
    sb.append(System.lineSeparator());
    return sb.toString();
  }
}
