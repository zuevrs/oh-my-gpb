package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.exceptions.PDFHelperException;

/** Класс для парсинга контента PDF файла, расширяющий деволтный класс {@link PDFTextStripper}. */
@Getter
public class CustomPDFTextStripper extends PDFTextStripper {

  /** Поле для хранения срезов текста PDF файла с одинаковым шрифтом. */
  private final List<PDFTextSlice> pdfContent = new ArrayList<>();
  /** Поле класса для хранения всего текста PDF файла. */
  private final String textContent;
  /** Поле содержащие путь до PDF файла */
  private final String filePath;

  /**
   * Конструктор, который сразу выполняет парсинг PDF Файла по его пути и загружает его текст и
   * выполняет заполнение полей {@link CustomPDFTextStripper#pdfContent} и {@link
   * CustomPDFTextStripper#textContent}.
   */
  public CustomPDFTextStripper(String filePath) throws IOException {
    super();
    this.filePath = filePath;
    PDDocument document = PDDocument.load(new File(filePath));
    textContent = getText(document);
  }

  /**
   * Конструктор, который сразу выполняет парсинг PDF Файла и загружает его текст и выполняет
   * заполнение полей {@link CustomPDFTextStripper#pdfContent} и {@link
   * CustomPDFTextStripper#textContent}.
   */
  public CustomPDFTextStripper(File file) throws IOException {
    super();
    this.filePath = file.getPath();
    PDDocument document = PDDocument.load(file);
    textContent = getText(document);
  }

  /**
   * Статический метод для получения объекта класса {@link CustomPDFTextStripper} с обработкой
   * возможных исключений.
   *
   * @param pdfFilePath путь до PDF файла, относительно папки запуска.
   * @return объект класса {@link CustomPDFTextStripper} созданный при помощи конструктора {@link
   *     CustomPDFTextStripper#CustomPDFTextStripper(String)}
   */
  public static CustomPDFTextStripper parsePDFFile(String pdfFilePath) {
    try {
      return new CustomPDFTextStripper(pdfFilePath);
    } catch (IOException ex) {
      throw new PDFHelperException("Ошибка при загрузке PDF файла: %s", ex.getMessage());
    }
  }

  /**
   * Статический метод для получения объекта класса {@link CustomPDFTextStripper} с обработкой
   * возможных исключений.
   *
   * @param file объект класса {@link File} содержащий PDF файл.
   * @return объект класса {@link CustomPDFTextStripper} созданный при помощи конструктора {@link
   *     CustomPDFTextStripper#CustomPDFTextStripper(String)}
   */
  public static CustomPDFTextStripper parsePDFFile(File file) {
    try {
      return new CustomPDFTextStripper(file);
    } catch (IOException ex) {
      throw new PDFHelperException("Ошибка при загрузке PDF файла: %s", ex.getMessage());
    }
  }

  /** Метод закрытия объекта {@link PDDocument}, который создался в ходе парсинка PDF файла. */
  public void closeDocument() {
    try {
      document.close();
    } catch (IOException ex) {
      throw new PDFHelperException("Не удалось закрыть PDF документа: %s", ex.getMessage());
    }
  }

  /**
   * Перегруженный метод для обработки одной строки текста PDF файла. Позволяет получить доступ к
   * данным о шрифте и других параметрах текста. Вызов данного метода осуществляется из других
   * методов базового класса при парсинги PDF файла.
   *
   * @param text строка текста PDF файла.
   * @param textPositions список позиций всех букв в строке и их параметры.
   * @throws IOException возникает при невохомжности записать обработанную строку в установленный
   *     канал вывода.
   */
  @Override
  protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
    StringBuilder builder = new StringBuilder();
    PDFont prevBaseFont = null;

    for (TextPosition position : textPositions) {
      PDFont currentFont = position.getFont();
      if (prevBaseFont == null) {
        prevBaseFont = currentFont;
      }
      if (!currentFont.getName().equals(prevBaseFont.getName())) {
        addTextSlice(builder.toString(), prevBaseFont);
        prevBaseFont = currentFont;
      }
      builder.append(position.getUnicode());
    }
    addTextSlice(builder.toString(), prevBaseFont);

    writeString(builder.toString());
  }

  /**
   * Метод определяет, нужно ли добавлдять новый срез текста или обновить существующий. И выполняет
   * требуемое.
   *
   * @param text срез текста.
   * @param font шрифт среза текста.
   */
  private void addTextSlice(String text, PDFont font) {
    if (text.isBlank()) {
      return;
    }
    if (pdfContent.size() > 0) {
      PDFTextSlice last = pdfContent.get(pdfContent.size() - 1);
      if (last.getFont().getName().equals(font.getName())) {
        last.setText(last.getText() + " " + text);
      } else {
        pdfContent.add(new PDFTextSlice(text, font));
      }
    } else {
      pdfContent.add(new PDFTextSlice(text, font));
    }
  }
}
