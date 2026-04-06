package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.pdf;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.pdfbox.pdmodel.font.PDFont;

/** Класс для хранения и обработки кусков текста из PDF файла, объединёные одним шрифтом. */
@Setter
@Getter
@AllArgsConstructor
public class PDFTextSlice {

  private String text;
  private PDFont font;

  @Override
  public String toString() {
    return text;
  }

  public boolean fontIsBold() {
    return font.getName().toLowerCase().contains("bold")
        && !font.getName().toLowerCase().contains("not-bold");
  }

  public boolean fontIsItalic() {
    return font.getName().toLowerCase().contains("italic")
        && !font.getName().toLowerCase().contains("not-italic");
  }

  public boolean fontNameContains(String name) {
    if (name.isBlank()) {
      return true;
    }
    return font.getName().contains(name);
  }

  public boolean textContains(String text) {
    return this.text.contains(text);
  }
}
