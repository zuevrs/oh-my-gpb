package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.tables;

import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.tables.extractionalgorithms.AbstractTableExtractionAlgorithm;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.tables.extractionalgorithms.BlackWhiteTableExtractionAlgorithm;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf.tables.extractionalgorithms.BlueTableExtractionAlgorithm;

public class TableExtractionAlgorithmFactory {

  /**
   * Получить алгоритм извлечения таблиц из ПДФ-файла по условному названию дизайна ПДФ-файла
   *
   * @param pdfDesign условное название дизайна ПДЙ-файла
   * @return алгоритм извлечения таблиц
   */
  public static AbstractTableExtractionAlgorithm getAlgorithm(String pdfDesign) {
    if (pdfDesign != null && pdfDesign.trim().equals("с синим дизайном")) {
      return new BlueTableExtractionAlgorithm();
    } else {
      return new BlackWhiteTableExtractionAlgorithm();
    }
  }
}
