package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;

import io.cucumber.java.DataTableType;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.exceptions.PDFHelperException;

/**
 * Класс для хранения и обработки строчик из таблицы шага .feature файла для работы с {@link
 * PDFTextSlice} из PDF файла.
 */
@Setter
@Getter
@AllArgsConstructor
public class PDFTextParameter {

  private String text;
  private String fontName;
  private boolean italic;
  private boolean bold;

  public PDFTextParameter() {}

  @Override
  public String toString() {
    return String.format(
        "Текст: %s\nНазвание шрифта: %s\nКурсив: %s\nЖирный: %s\n", text, fontName, italic, bold);
  }

  /**
   * Метод пероборазует каждую строчку Cucumber {@link io.cucumber.datatable.DataTable} в объект
   * класса. Преобразование идёт по имени столбца.
   */
  @DataTableType
  public PDFTextParameter pdfTextParametersTransformer(Map<String, String> entry) {
    if (entry.get("Текст") == null) {
      throw new PDFHelperException(
          "Столбец Текст для проверки тескста в PDF файле - пустой. Требуется его заполнить.");
    }
    return new PDFTextParameter(
        processValue(entry.get("Текст")),
        processValue(entry.get("Шрифт") == null ? "" : entry.get("Шрифт")),
        Boolean.parseBoolean(
            processValue(entry.get("Курсив") == null ? "false" : entry.get("Курсив"))),
        Boolean.parseBoolean(
            processValue(entry.get("Жирный") == null ? "false" : entry.get("Жирный"))));
  }

  /**
   * Метод проверяет соотвествует ли срез текста всем установленным параметрам.
   *
   * @param textSlice срез текста
   * @return возвращает true, если все параметры совпадают, иначе - false.
   */
  public boolean checkTextSlice(PDFTextSlice textSlice) {
    return textSlice.textContains(text)
        && textSlice.fontNameContains(fontName)
        && textSlice.fontIsBold() == bold
        && textSlice.fontIsItalic() == italic;
  }
}
