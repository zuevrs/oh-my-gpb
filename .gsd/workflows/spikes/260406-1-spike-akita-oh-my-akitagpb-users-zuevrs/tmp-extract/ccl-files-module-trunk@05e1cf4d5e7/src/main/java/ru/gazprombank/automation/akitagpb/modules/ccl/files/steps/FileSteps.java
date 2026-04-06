package ru.gazprombank.automation.akitagpb.modules.ccl.files.steps;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

import io.cucumber.java.ru.И;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.FileHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.PropertyLoader;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class FileSteps extends BaseMethods {

  /**
   * Сохраняем содержимое файла в переменную. Пример 1: И содержимое файла
   * "${test.data.base.path}/common/camunda/fetchAndLock.json" сохраняем в переменную сценария
   * "file" Пример 2: И содержимое файла ресурсов "fetchAndLock.json" сохраняем в переменную
   * сценария "file"
   *
   * @param isResourcesFile если использовать в шаге "файл ресурсов" - то достаточно передать имя
   *     файла (или уникальный относительный путь) из main/resources или test/resources, без части
   *     пути "src/main..." (т.е. без префикса типа ${test.data.base.path})
   * @param value путь к файлу / имя файла ресурсов
   * @param varName имя переменной сценария.
   */
  @И("^содержимое файла( ресурсов)? \"(.*)\" сохраняем в переменную сценария \"(.*)\"$")
  public void saveStringFromFileValue(String isResourcesFile, String value, String varName) {
    String filePath = resolveVariables(value);
    String content =
        isResourcesFile != null
            ? FileHelper.getResourcesFileContent(filePath)
            : FileHelper.getFileContent(FileHelper.getFileFromProject(filePath));
    akitaScenario.setVar(varName, content);
    akitaScenario.log(String.format("Значение %s: %s", varName, content));
  }

  /**
   * Проверяем что файл загрузился в директорию директория задана в файле selenide.properties в
   * параметре "experimentalOption.prefs.download.default_directory"
   *
   * @param fileName имя файла
   */
  @И(
      "^файл \"(.*)\" загрузился в директорию загрузки( и сохранён в переменную сценария \"(.*)\")?$")
  public void fileDownloaded(String fileName, String varName) {
    File[] expectedFiles = checkAndGetDownloadedFile(fileName);
    if (varName != null) {
      File copy =
          FileHelper.copyFile(
              expectedFiles[0], "build/downloads/" + akitaScenario.getScenario().getId(), true);
      akitaScenario.setVar(varName, copy);
      akitaScenario.log(String.format("Значение %s: %s", varName, copy));
    }
    this.deleteFiles(expectedFiles);
  }

  /**
   * Проверяем что файл загрузился в директорию (директория задана в файле selenide.properties в
   * параметре "experimentalOption.prefs.download.default_directory"). После проверки исходный файл
   * копируется в файл по указанному пути. Пример: И файл "Чек-лист контролей.pdf" загрузился в
   * директорию загрузки и сохранён по пути "X:/Desktop/copy.pdf"
   *
   * @param fileName имя файла
   * @param filePath путь, куда нужно скопировать загруженный файл
   */
  @И("^файл \"(.*)\" загрузился в директорию загрузки и сохранён по пути \"(.*)\"$")
  public void downloadAndSaveFile(String fileName, String filePath) {
    File[] expectedFiles = checkAndGetDownloadedFile(fileName);
    filePath = processValue(filePath);
    var copy = FileHelper.copyFile(expectedFiles[0], filePath);
    akitaScenario.log("Исходный файл был скопирован в файл по пути " + copy.getAbsolutePath());
    this.deleteFiles(expectedFiles);
  }

  /**
   * Проверить, что файл загрузился в директорию (директория задана в файле selenide.properties в
   * параметре "experimentalOption.prefs.download.default_directory") и вернуть массив загруженных
   * файлов.
   *
   * @param expectedFileName ожидаемое имя файла в директории загрузки
   * @return массив загруженных файлов
   */
  private File[] checkAndGetDownloadedFile(String expectedFileName) {
    String value = resolveVariables(expectedFileName);
    File downloads =
        new File(
            PropertyLoader.getPropertyOrValue(
                "experimentalOption.prefs.download.default_directory"));
    await()
        .atMost(5, SECONDS)
        .ignoreExceptions()
        .until(
            () ->
                Objects.requireNonNull(
                            downloads.listFiles(
                                (files, file) ->
                                    !file.endsWith(".crdownload") && file.contains(value)))
                        .length
                    > 0);
    akitaScenario.log("Список файлов в директории: ");
    akitaScenario.log(downloads.list());
    File[] expectedFiles = downloads.listFiles((files, file) -> file.contains(value));
    akitaScenario.log("Список найденных файлов: ");
    Arrays.stream(expectedFiles).forEach(file -> akitaScenario.log(file.getAbsolutePath()));
    assertThat(expectedFiles).withFailMessage("Ошибка поиска файла").isNotNull();
    assertThat(expectedFiles).withFailMessage("Ошибка поиска файла").isNotEmpty();
    assertThat(expectedFiles.length).withFailMessage("Файл не загрузился").isGreaterThan(0);
    assertThat(expectedFiles.length)
        .withFailMessage(
            String.format(
                "В папке присутствуют более одного файла с одинаковым названием, содержащим текст [%s]",
                value))
        .isEqualTo(1);
    return expectedFiles;
  }

  /**
   * Сохранить рандомный файл заданного размера (в МБ) с заданным расширением в переменную сценария
   *
   * @param fileSize размер файла в МБ
   * @param fileExtension расширение файла
   * @param varName название переменной сценраия
   */
  @И("^сохраняем рандомный файл размером \"(.*)\" МБ в формате \"(.*)\" в переменную \"(.*)\"$")
  public void saveRandomFile(String fileSize, String fileExtension, String varName) {
    double size = Double.parseDouble(resolveVariables(fileSize));
    fileExtension = resolveVariables(fileExtension);
    fileExtension = fileExtension.startsWith(".") ? fileExtension : "." + fileExtension;
    File file = FileHelper.getRandomFile(size, fileExtension);
    akitaScenario.setVar(varName, file);
    akitaScenario.log(String.format("Значение %s: %s", varName, "файл " + file));
  }

  /**
   * Сохранить имя файла из указанной переменной
   *
   * @param fileVar переменная сценария, в которой сохранён файл
   * @param varName переменная сценраия для сохранения имени файла
   */
  @И("^сохраняем имя файла \"(.*)\" в переменную \"(.*)\"$")
  public void saveFileName(String fileVar, String varName) {
    fileVar = fileVar.replace("${", "").replace("}", "");
    var file = (File) akitaScenario.getVar(fileVar);
    var fileName = file.getName();
    akitaScenario.setVar(varName, fileName);
    akitaScenario.log(String.format("Значение %s: %s", varName, fileName));
  }

  /**
   * @param varName переменная из которой получаем массив байтов
   * @param fileName имя файла, в который преобразуем
   * @param fileVarName имя переменной, в которую сохраняем преобразованный файл
   */
  @SneakyThrows
  @И(
      "^значение переменной \"(.*)\" преобразуем в файл с именем \"(.*)\" и сохраняем в переменную \"(.*)\"$")
  public void test(String varName, String fileName, String fileVarName) {
    byte[] fileContent = (byte[]) akitaScenario.getVar(varName);
    fileName = resolveVariables(fileName);
    Path path =
        Paths.get(
            System.getProperty("user.dir"),
            "/build/downloads/",
            akitaScenario.getScenario().getId());
    Files.createDirectories(path);
    path = Paths.get(path.toString(), fileName);
    Files.write(path, fileContent);
    akitaScenario.setVar(fileVarName, path.toFile());
    akitaScenario.log(
        String.format(
            "Преобразовали переменную '%s' в файл с именем '%s' и сохранили в директории '%s'",
            varName, fileName, path));
  }

  @И("^сохранить файл \"(.+)\" в переменную сценария \"(.+)\"$")
  public void saveFile(String filePath, String varName) {
    File file = new File(resolveVariables(filePath));
    akitaScenario.setVar(varName, file);
  }

  /**
   * Шаг для распаковки zip-архива. Список файлов из архива сохраняется в переменную.
   *
   * @param fileVarOrFilePath zip-файл или путь до zip-файла
   * @param varName имя переменной в которую сохраняем полученный список файлов
   */
  @И(
      "^распаковать архив \"(.+)\" и сохранить список разархивированных файлов в переменную \"(.+)\"$")
  public void unzipFileIntoFilesList(String fileVarOrFilePath, String varName) {
    File zipFile = FileHelper.getFile(fileVarOrFilePath);
    var files = FileHelper.unzip(zipFile);
    files.forEach(e -> akitaScenario.log("Получен файл из архива:\n" + e.getAbsolutePath()));
    akitaScenario.setVar(varName, files);
  }

  /**
   * Шаг для проверки списка файлов на содержание в нём файлов с ожидаемыми именами. Ожидаемое имя
   * файла может быть точное, а может быть шаблоном (регулярным выражением). Пример: И список файлов
   * "files" содержит файлы с именами: | 01_Бухгалтерский баланс за 9 месяцев 2023 года.docx | |
   * 02_Отчет.*.docx | <- это шаблон, по которому будет найден файл с именем, начинающимся с
   * "02_Отчет", и имеющий расширение .docx
   *
   * @param filesListVar переменная, в которую заранее сохранён список файлов
   * @param fileNames список ожидаемых имён файлов
   */
  @И("^список файлов \"(.+)\" содержит файлы с именами:?$")
  @SuppressWarnings("unchecked")
  public void assertFilesListContainsFilesWithNames(String filesListVar, List<String> fileNames) {
    var files =
        new ArrayList<>((List<File>) akitaScenario.getVar(StringHelper.getVarName(filesListVar)));
    SoftAssertions assertions = new SoftAssertions();
    fileNames.forEach(
        name -> {
          var nameTemplate = processValue(name);
          akitaScenario.log(String.format("Поиск файла с названием '%s'", nameTemplate));
          var file =
              files.stream()
                  .filter(
                      e -> nameTemplate.equals(e.getName()) || e.getName().matches(nameTemplate))
                  .findFirst()
                  .orElse(null);
          if (file != null) {
            files.remove(file);
            akitaScenario.log(
                String.format(
                    "По шаблону '%s' найден файл:\n'%s'", nameTemplate, file.getAbsolutePath()));
          }
          assertions
              .assertThat(file)
              .as(String.format("Файл по шаблону '%s' в списке не найден", nameTemplate))
              .isNotNull();
        });
    assertions.assertAll();
  }

  /**
   * Шаг для поиска файла из списка файлов по имени и сохранения его в переменную. Ожидаемое имя
   * файла может быть точное, а может быть шаблоном (регулярным выражением). Примеры: И из списка
   * файлов "files" сохранить файл с именем "06_Бухгалтерский баланс за 2022 год.docx" в переменную
   * "docxFile" И из списка файлов "files" сохранить файл с именем "06_Бухгалтерский баланс.*.docx"
   * в переменную "docxFile"
   *
   * @param filesListVar переменная, в которую заранее сохранён список файлов
   * @param fileName ожидаемое имя файла
   * @param varName переменная для сохраненния найденного файла
   */
  @И("^из списка файлов \"(.+)\" сохранить файл с именем \"(.+)\" в переменную \"(.+)\"$")
  @SuppressWarnings("unchecked")
  public void saveFileFromFilesListByName(String filesListVar, String fileName, String varName) {
    var files =
        new ArrayList<>((List<File>) akitaScenario.getVar(StringHelper.getVarName(filesListVar)));
    var nameTemplate = processValue(fileName);
    var file =
        files.stream()
            .filter(e -> fileName.equals(e.getName()) || e.getName().matches(fileName))
            .findFirst()
            .orElseThrow(
                () ->
                    new RuntimeException(
                        String.format("Файл с именем '%s' в списке не найден", nameTemplate)));
    akitaScenario.setVar(varName, file);
    akitaScenario.log(
        String.format("В переменную '%s' сохранён файл:\n'%s'", varName, file.getAbsolutePath()));
  }
}
