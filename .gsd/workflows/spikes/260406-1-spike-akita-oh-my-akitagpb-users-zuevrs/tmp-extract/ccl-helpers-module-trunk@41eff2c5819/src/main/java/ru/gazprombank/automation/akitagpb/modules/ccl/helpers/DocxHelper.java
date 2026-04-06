package ru.gazprombank.automation.akitagpb.modules.ccl.helpers;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import lombok.SneakyThrows;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.Text;

/** Вспомогательный класс для работы с .docx-файлами */
public class DocxHelper {

  /**
   * Получить список всех элементов класса org.docx4j.wml.Text
   *
   * @param object объект, из которого нужно получить текстовые элементы
   * @return список всех элементов класса org.docx4j.wml.Text
   */
  public static List<Text> getAllTextElementsFromObject(Object object) {
    List<Text> result = new ArrayList<>();
    if (object instanceof JAXBElement) {
      object = ((JAXBElement<?>) object).getValue();
    }
    if (object.getClass().equals(Text.class)) {
      result.add((Text) object);
    } else if (object instanceof ContentAccessor) {
      List<?> children = ((ContentAccessor) object).getContent();
      for (Object child : children) {
        result.addAll(getAllTextElementsFromObject(child));
      }
    }
    return result;
  }

  /** Получить текстовое содержимое docx-файла из его главного документа document.xml */
  @SneakyThrows
  public static String getDocxContentFromMainDocumentPart(File docxFile) {
    WordprocessingMLPackage template = WordprocessingMLPackage.load(docxFile);
    //noinspection rawtypes
    return template.getMainDocumentPart().getJAXBNodesViaXPath("//w:t", true).stream()
        .map(e -> ((Text) ((JAXBElement) e).getValue()).getValue())
        .collect(Collectors.joining(""))
        .replace(" ", " ")
        .replaceAll("\\s{2,}", " ");
  }

  /** Получить текстовое содержимое docx-файла из его главного документа document.xml */
  @SneakyThrows
  public static String getDocxContentFromMainDocumentPart(InputStream docxFile) {
    WordprocessingMLPackage template = WordprocessingMLPackage.load(docxFile);
    //noinspection rawtypes
    return template.getMainDocumentPart().getJAXBNodesViaXPath("//w:t", true).stream()
        .map(e -> ((Text) ((JAXBElement) e).getValue()).getValue())
        .collect(Collectors.joining(""))
        .replace(" ", " ")
        .replaceAll("\\s{2,}", " ");
  }
}
