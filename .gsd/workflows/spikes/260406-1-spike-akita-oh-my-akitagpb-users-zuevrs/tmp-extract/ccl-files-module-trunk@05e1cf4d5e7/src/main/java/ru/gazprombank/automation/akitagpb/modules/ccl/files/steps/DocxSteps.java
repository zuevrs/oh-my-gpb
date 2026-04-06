package ru.gazprombank.automation.akitagpb.modules.ccl.files.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

import io.cucumber.java.ru.И;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Text;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.DocxHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.FileHelper;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class DocxSteps extends BaseMethods {

  /**
   * Подставить в заданный DOCX-шаблон значения переменных из таблицы. Полученный файл сохранить в
   * переменную
   *
   * @param filePath путь до .docx-файла-шаблона (можно относительный путь до файла ресурсов,
   *     например: debug/template123.docx)
   * @param varName переменная сценария, в которую нужно сохранить файл
   * @param placeholders таблица переменных, которые нужно заменить в шаблоне, и значений, которые
   *     нужно подставить вместо них
   */
  @И(
      "^в DOCX-шаблон \"(.+)\" подставить переменные из таблицы, сохранив результат в файл \"(.+)\"$")
  public void getDocxByTemplate(String filePath, String varName, Map<String, String> placeholders)
      throws Docx4JException {
    String finalPath = processValue(filePath);
    InputStream docxTemplate = FileHelper.getResourceFile(finalPath);
    WordprocessingMLPackage template = WordprocessingMLPackage.load(docxTemplate);

    List<Text> texts = DocxHelper.getAllTextElementsFromObject(template.getMainDocumentPart());
    for (Text text : texts) {
      placeholders.forEach(
          (k, v) -> {
            k = resolveVariables(k);
            if (text.getValue().contains(k)) {
              text.setValue(text.getValue().replace(k, processValue(v)));
            }
          });
    }
    try {
      Files.createDirectories(Paths.get("build/" + finalPath).getParent());
      File file = new File("build/" + finalPath);
      template.save(file);
      akitaScenario.setVar(varName, file);
      akitaScenario.log("Шаблон с подставленными переменными сохранён в файл " + file.getPath());
    } catch (IOException ex) {
      throw new RuntimeException(
          "Не удалось создать DOCX Файл с заполненным шаблоном: " + ex.getMessage());
    }
  }

  /**
   * Сравнить текстовое содержимое .docx-файлов
   *
   * @param firstFileVar переменная, в которой сохранён первый файл
   * @param secondFileVar переменная, в которой сохранён второй файл
   */
  @И("^содержимое DOCX-файлов \"(.*)\" и \"(.*)\" равно$")
  public void assertDocxFilesContentEqual(String firstFileVar, String secondFileVar) {
    firstFileVar = firstFileVar.replace("${", "").replace("}", "");
    secondFileVar = secondFileVar.replace("${", "").replace("}", "");
    var firstFile = (File) akitaScenario.getVar(firstFileVar);
    var secondFile = (File) akitaScenario.getVar(secondFileVar);
    var firstContent = DocxHelper.getDocxContentFromMainDocumentPart(firstFile);
    var secondContent = DocxHelper.getDocxContentFromMainDocumentPart(secondFile);
    assertThat(firstContent)
        .as(
            String.format(
                "Содержимое DOCX-файлов не равно. Разница начинается с индекса [%d], с подстроки %n[%s]",
                StringUtils.indexOfDifference(firstContent, secondContent),
                StringUtils.difference(firstContent, secondContent)))
        .isEqualTo(secondContent);
  }

  /**
   * Шаг для сохранения текстового содержимого DOCX-файла.
   *
   * @param fileVarOrFilePath docx-файл или путь до docx-файла
   * @param varName переменная для сохраненния текстового содержимого файла
   */
  @И("^содержимое DOCX-файла \"(.*)\" сохранено в переменную \"(.*)\"$")
  public void saveDocxFilesContent(String fileVarOrFilePath, String varName) {
    String finalPath = processValue(fileVarOrFilePath);
    InputStream docxFile = FileHelper.getResourceFile(finalPath);
    var fileContent = DocxHelper.getDocxContentFromMainDocumentPart(docxFile);
    akitaScenario.setVar(varName, fileContent);
    akitaScenario.log(
        String.format("В переменную '%s' сохранено значение:\n'%s'", varName, fileContent));
  }
}
