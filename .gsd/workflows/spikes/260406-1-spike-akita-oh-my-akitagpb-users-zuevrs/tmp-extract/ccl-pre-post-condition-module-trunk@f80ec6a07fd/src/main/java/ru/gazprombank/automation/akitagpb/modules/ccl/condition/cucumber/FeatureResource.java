package ru.gazprombank.automation.akitagpb.modules.ccl.condition.cucumber;

import io.cucumber.core.resource.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.FileHelper;

/**
 * Класс, имплементирующий интерфейс io.cucumber.core.resource.Resource, для представления
 * feature-файла из ресурсов данного проекта
 */
public class FeatureResource implements Resource {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureResource.class);

  private InputStream inputStream;
  private URI uri;
  private String fileName;
  private String scenarioTag;

  /**
   * Конструктор feature-ресурса. Если параметр конструктора содержит имя feature-файла -
   * feature-файл данного объекта будет найден по этому имени файла. Если параметр содержит только
   * тег сценария - то feature-файл с таким сценарием будет искаться среди всех фич проекта по
   * содержимому текста (на соответствие regexp шаблону "\\sscenarioTag\\s").
   *
   * @param featureNameAndTag имя feature-файла, тег сценария или имя feature-файла + тег сценария
   *     вместе.
   */
  public FeatureResource(String featureNameAndTag) throws URISyntaxException {
    initFileNameAndScenarioTag(featureNameAndTag);
    try {
      File featureFile =
          fileName != null
              ? FileHelper.getFileFromResources(fileName)
              : FileHelper.getFileFromResourcesWithContentMatchingRegexp(
                  String.format("\\s%s\\s", scenarioTag), ".feature");
      inputStream = new FileInputStream(featureFile);
      uri = featureFile.toURI();
    } catch (Exception e) {
      LOGGER.info("{}\nПробуем найти ресурс внтри Jar файла.", e.getMessage());
      String path =
          fileName != null
              ? FileHelper.getResourceFileByFileName("testdata", fileName)
              : FileHelper.getResourceFileWithContentMatchingRegexp(
                  "testdata", scenarioTag, "feature");
      inputStream = this.getClass().getResourceAsStream("/" + path);
      uri = new URI(path);
    }
  }

  /**
   * Конструктор feature-ресурса из пути до конкретного feature файла.
   *
   * @param filePath путь до файла
   */
  public FeatureResource(Path filePath) throws IOException {
    inputStream = new FileInputStream(filePath.toFile());
    uri = filePath.toUri();
    fileName = filePath.getFileName().toString();
  }

  /**
   * Инициализировать переменные fileName и scenarioTag по входящему параметру featureNameAndTag.
   * featureNameAndTag может быть как тегом сценария (если строка начинается с тильды '~'), так и
   * именем файла (строка без тильды) или комбинированным значением - 'featureName~scenarioTag' -
   * разделены тильдой '~'. К имени файла добавляется расширение .feature (если его нет).
   *
   * @param featureNameAndTag имя feature-файла, тег сценария или имя feature-файла + тег сценария
   *     вместе.
   */
  private void initFileNameAndScenarioTag(String featureNameAndTag) {
    if (featureNameAndTag.startsWith("~")) {
      scenarioTag = featureNameAndTag.replace("~", "@");
    } else if (featureNameAndTag.contains("~")) {
      var array = featureNameAndTag.split("~");
      fileName = array[0];
      scenarioTag = "@" + array[1];
    } else {
      fileName = featureNameAndTag;
    }
    if (fileName != null) {
      fileName = fileName.endsWith(".feature") ? fileName : fileName + ".feature";
    }
  }

  public String getScenarioTag() {
    return scenarioTag;
  }

  @Override
  public URI getUri() {
    return uri;
  }

  @Override
  public InputStream getInputStream() {
    return inputStream;
  }
}
