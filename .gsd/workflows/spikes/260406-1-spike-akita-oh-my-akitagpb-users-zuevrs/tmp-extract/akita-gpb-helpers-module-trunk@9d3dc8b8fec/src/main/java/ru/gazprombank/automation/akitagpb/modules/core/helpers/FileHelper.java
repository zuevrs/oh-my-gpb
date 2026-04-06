package ru.gazprombank.automation.akitagpb.modules.core.helpers;

import static ru.gazprombank.automation.akitagpb.modules.core.helpers.StringHelper.normalizeRelativeFilePath;
import static ru.gazprombank.automation.akitagpb.modules.core.helpers.StringHelper.processValue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.api.AkitaScenario;

/** Утилитный класс для работы с файлами */
public class FileHelper {

  public static final Path BASE_PATH = getBuildResourcesDirectory();
  private static final Logger logger = LoggerFactory.getLogger(FileHelper.class);

  /**
   * Определить, является ли переданная строка валидным путём к файлу
   *
   * @param filePath строка
   * @return является ли переданная строка валидным путём к файлу
   */
  public static boolean isFilePathValid(String filePath) {
    try {
      var fileName = FilenameUtils.getBaseName(Paths.get(filePath).getFileName().toString());
      return !filePath.matches("[\\s\\S]*[:*?\"<>]+[\\s\\S]*")
          && !fileName.matches("(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])")
          && !fileName.contains("/")
          && !fileName.contains("\\");
    } catch (InvalidPathException | NullPointerException e) {
      return false;
    }
  }

  /**
   * Получить файл проекта - из директории .../ccl-autotest
   *
   * @param relativePath путь к файлу относительно директории проекта /ccl-autotest
   * @return найденный файл
   */
  public static File getFileFromProject(String relativePath) {
    Path resultPath = Paths.get(normalizeRelativeFilePath(relativePath));
    if (!resultPath.toFile().exists()) {
      resultPath =
          Paths.get(System.getProperty("user.dir"))
              .resolve(normalizeRelativeFilePath(relativePath));
    }
    AkitaScenario.getInstance().log("Получаем файл по пути: " + resultPath);
    try {
      return resultPath.toFile().getCanonicalFile();
    } catch (IOException ex) {
      throw new RuntimeException("Ошибка при получении файла " + resultPath, ex);
    }
  }

  /**
   * Получить файл ресурсов из директории /build/resources - неважно, из main или из test. Поиск
   * файла происходит по имени файла или его относительному пути. Пример 1: в main и test ресурсах
   * есть только один файл с именем "teamCityBody.json" - поэтому мы можем передать в метод только
   * имя файла. Пример 2: в ресурсах есть 3 файла с именем "order3.json" в директориях
   * .../common/first_path, ../second_path и ../third_path - поэтому кроме имени нужно уточнить
   * директорию файла и передать в метод "first_path/order3.json"
   *
   * @param relativePath относительный путь к файлу ресурсов (может быть просто имя файла)
   * @return найденный файл
   */
  public static File getFileFromResources(String relativePath) {
    Path relativeFilePath = Paths.get(normalizeRelativeFilePath(relativePath));
    if (relativeFilePath.toFile().exists()) {
      return relativeFilePath.toFile();
    } else {
      AkitaScenario.getInstance()
          .log(
              String.format(
                  "Получаем файл по пути %s в директории %s", relativeFilePath, BASE_PATH));
      return findFileByRelativePathOrName(BASE_PATH, relativeFilePath);
    }
  }

  /**
   * Получить InputStream из ресурсов jar файла или из директории /build/resources - неважно, из
   * main или из test. Поиск файла происходит по имени файла или его относительному пути. Пример 1:
   * в main и test ресурсах есть только один файл с именем "teamCityBody.json" - поэтому мы можем
   * передать в метод только имя файла. Пример 2: в ресурсах есть 3 файла с именем "order3.json" в
   * директориях .../common/first_path, ../second_path и ../third_path - поэтому кроме имени нужно
   * уточнить директорию файла и передать в метод "first_path/order3.json"
   *
   * @param relativePath относительный путь к файлу ресурсов (может быть просто имя файла)
   * @return найденный файл
   */
  public static InputStream getResourceFile(String relativePath) {

    InputStream resourceAsStream =
        FileHelper.class.getResourceAsStream(
            relativePath.startsWith("/") ? relativePath : "/" + relativePath);
    if (resourceAsStream == null) {
      try {
        return new FileInputStream(getFileFromResources(relativePath));
      } catch (FileNotFoundException ex) {
        throw new RuntimeException(
            "Не смогли найти файл: " + relativePath + "\n" + ex.getMessage());
      }
    } else {
      return resourceAsStream;
    }
  }

  /**
   * Получить содержимое файла в виде строки.
   *
   * @param file файл
   * @return содержимое файла в виде строки
   */
  public static String getFileContent(File file) {
    try {
      return Files.readString(file.toPath());
    } catch (IOException e) {
      throw new RuntimeException("Ошибка при чтении из файла " + file.toPath(), e);
    }
  }

  /**
   * Получить содержимое файла в виде строки.
   *
   * @param inputStream поток данных файла
   * @return содержимое файла в виде строки
   */
  public static String getFileContent(InputStream inputStream) {
    if (inputStream == null) {
      throw new RuntimeException(
          "Поток данных равен null. Возможно, не удалось загрузить данные из файла или он не был найден.");
    }
    try {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Ошибка при чтении из потока данных", e);
    }
  }

  /**
   * Безопасно прочитать содержимое файла, подходит для проверки безопасности.
   *
   * @param path - путь до фалйа в формате Path.
   * @return содержимое файла в формате String.
   */
  public static String getFileContent(Path path) {
    try {
      byte[] fileContentBytes = Files.readAllBytes(path);
      return new String(fileContentBytes, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new RuntimeException("Ошибка при чтении из файла " + path, ex);
    }
  }

  /**
   * Получить содержимое файла ресурсов в виде строки по относительному пути (или имени) файла.
   *
   * @param relativePath относительный путь (или имя) файла ресурсов
   * @return содержимое файла ресурсов в виде строки
   */
  public static String getResourcesFileContent(String relativePath) {
    InputStream resourceAsStream =
        FileHelper.class.getResourceAsStream(
            relativePath.startsWith("/") ? relativePath : "/" + relativePath);
    if (resourceAsStream == null) {
      return getFileContent(getFileFromResources(relativePath));
    } else {
      return getFileContent(resourceAsStream);
    }
  }

  /**
   * Получить файл ресурсов из директории /build/resources - неважно, из main или из test. Поиск
   * файла происходит по соответствию его текстового содержимого переданному regexp-шаблону. Чтобы
   * оптимизировать процесс поиска, есть опциональный парметр "fileExtension" - расширение файла.
   * Если этот параметр передан, то содержимое файла будет вычитываться только из файлов с заданным
   * расширением, а не из всех подряд.
   *
   * @param regexp шаблон, с которым ищется соответствие текстового содержимого файлов;
   * @param fileExtension расширение файла - опциональный параметр для оптимизации процесса поиска
   *     файла (вычитывать содержимое файлов только заданного расширения);
   * @return найденный файл
   */
  public static File getFileFromResourcesWithContentMatchingRegexp(
      String regexp, String fileExtension) {
    try (Stream<Path> pathStream =
        Files.find(
            BASE_PATH,
            10,
            (path, b) -> {
              if (fileExtension != null && !path.toString().endsWith(fileExtension)) {
                return false;
              }
              String content = getFileContent(path.toFile());
              Pattern pattern = Pattern.compile(regexp);
              Matcher matcher = pattern.matcher(content);
              return matcher.find();
            })) {
      var pathsList = pathStream.toList();
      if (pathsList.size() != 1) {
        String message =
            pathsList.size() == 0
                ? String.format(
                    "Файл ресурсов с содержимым, соответствующим шаблону '%s', не найден", regexp)
                : String.format(
                    "Найдено более одного файла с содержимым, соответствующим шаблону '%s'. "
                        + "Требуется указать уникальный шаблон для поиска: \n%s",
                    regexp, pathsList);
        throw new RuntimeException(message);
      }
      AkitaScenario.getInstance().log(String.format("Найден файл по пути %s", pathsList.get(0)));
      return pathsList.get(0).toFile();
    } catch (IOException e) {
      throw new RuntimeException(
          "Ошибка при поиске файла ресурсов, соответствующего шаблону '" + regexp + "'", e);
    }
  }

  /**
   * Получить файл ресурсов из classpath - внутри ли снаружи JAR файла. Поиск файла происходит по
   * соответствию его текстового содержимого переданному regexp-шаблону. Чтобы оптимизировать
   * процесс поиска, есть опциональный парметр "fileExtension" - расширение файла ресурсов. Если
   * этот параметр передан, то содержимое файла будет вычитываться только из файлов с заданным
   * расширением, а не из всех подряд.
   *
   * @param regexp шаблон, с которым ищется соответствие текстового содержимого файлов;
   * @param fileExtension расширение файла - опциональный параметр для оптимизации процесса поиска
   *     файла (вычитывать содержимое файлов только заданного расширения);
   * @return найденный файл ресурсов в виде {@link InputStream}
   */
  public static String getResourceFileWithContentMatchingRegexp(
      String prefix, String regexp, String fileExtension) {
    List<String> foundResourcePaths = new ArrayList<>();
    String resourceRegexp = fileExtension == null ? ".+" : ".+\\." + fileExtension;
    Set<String> resourceList =
        new Reflections(prefix, Scanners.Resources).getResources(resourceRegexp);
    for (String path : resourceList) {
      InputStream inputStream = FileHelper.class.getResourceAsStream("/" + path);
      String content = getFileContent(inputStream);
      Pattern pattern = Pattern.compile(regexp);
      Matcher matcher = pattern.matcher(content);
      if (matcher.find()) {
        foundResourcePaths.add(path);
      }
    }

    if (foundResourcePaths.size() != 1) {
      String message =
          foundResourcePaths.size() == 0
              ? String.format(
                  "Файл ресурсов с содержимым, соответствующим шаблону '%s', не найден", regexp)
              : String.format(
                  "Найдено более одного файла с содержимым, соответствующим шаблону '%s'. "
                      + "Требуется указать уникальный шаблон для поиска: \n%s",
                  regexp, foundResourcePaths);
      throw new RuntimeException(message);
    }
    AkitaScenario.getInstance()
        .log(String.format("Найден файл по пути %s", foundResourcePaths.get(0)));
    return foundResourcePaths.get(0);
  }

  public static String getResourceFileByFileName(String prefix, String fileName) {
    List<String> foundResourcePaths =
        new Reflections(prefix, Scanners.Resources).getResources(fileName).stream().toList();

    if (foundResourcePaths.size() != 1) {
      String message =
          foundResourcePaths.size() == 0
              ? String.format(
                  "Файл ресурсов с именем, соответствующим шаблону '%s', не найден", fileName)
              : String.format(
                  "Найдено более одного файла с именем, соответствующим шаблону '%s'. "
                      + "Требуется указать уникальный шаблон для поиска: \n%s",
                  fileName, foundResourcePaths);
      throw new RuntimeException(message);
    }
    AkitaScenario.getInstance()
        .log(String.format("Найден файл по пути %s", foundResourcePaths.get(0)));
    return foundResourcePaths.get(0);
  }

  /**
   * Метод создаёт из массива байтов временный файл с указанным расширением в директории
   * /build/resources/${sessionId} данного проекта.
   *
   * @param bytes - массив байтов, который необходимо преобразовать в файл;
   * @param extension - расширение, в котором необходимо создать файл;
   * @return Созданный из массива байтов файл
   */
  @SneakyThrows
  public static File getFileFromBytes(byte[] bytes, String extension) {
    Path tempDir =
        Paths.get(BASE_PATH.toString(), AkitaScenario.getInstance().getScenario().getId());
    Files.createDirectories(tempDir);
    File file = Files.createTempFile(tempDir, "tempFile", extension).toFile();
    try (OutputStream out = Files.newOutputStream(file.toPath())) {
      out.write(bytes);
      out.flush();
      return file;
    } catch (IOException e) {
      AkitaScenario.getInstance().log("Возникла ошибка при создании файла: \n" + e.getMessage());
      throw new RuntimeException("Возникла ошибка при создании файла", e);
    }
  }

  /**
   * Создать рандомный файл заданного размера (в МБ) с заданным расширением
   *
   * @param fileSizeInMB размер файла в МБ
   * @param extension расширение файла
   * @return файл заданного размера (в МБ) с заданным расширением
   */
  public static File getRandomFile(double fileSizeInMB, String extension) {
    byte[] bytes = new byte[(int) (fileSizeInMB * 1024 * 1024)];
    Arrays.fill(bytes, (byte) 0);
    return getFileFromBytes(bytes, extension);
  }

  /**
   * Копировать переданный файл в файл по указанному пути
   *
   * @param source исходный файл, который надо скопировать
   * @param destinationPath путь файла-копии
   * @return копия исходного файла
   */
  public static File copyFile(File source, String destinationPath) {
    try {
      File result = Paths.get(destinationPath).toFile();
      if (result.exists()) {
        throw new RuntimeException(
            String.format("Файл по заданному пути [%s] уже существует", destinationPath));
      }
      FileUtils.copyFile(source, result);
      return result;
    } catch (IOException e) {
      throw new RuntimeException("Ошибка при копировании файла", e);
    }
  }

  /**
   * Найти файл по относительному пути или имени
   *
   * @param start путь к директории, откуда начинать искать файл
   * @param filePath относительный путь к файлу (или просто имя файла), который нужно найти в
   *     указанной директории
   * @return найденный файл
   */
  private static File findFileByRelativePathOrName(Path start, Path filePath) {
    List<Path> pathsList = new ArrayList<>();
    try (Stream<Path> pathStream = Files.walk(start)) {
      pathStream
          .filter(
              path ->
                  Files.isReadable(path) && Files.isRegularFile(path) && path.endsWith(filePath))
          .forEach(pathsList::add);
    } catch (Exception e) {
      logger.error("Ошибка доступа к файлу или папке: {}", e.getMessage());
    }

    if (pathsList.size() != 1) {
      String message =
          pathsList.size() == 0
              ? String.format(
                  "Файл по заданному пути '%s' не найден или возникли ошибки при поискее", filePath)
              : String.format(
                  "Найдено более одного файла по заданному пути '%s'. Требуется указать более точный путь: \n%s",
                  filePath, pathsList);
      throw new RuntimeException(message);
    }

    AkitaScenario.getInstance().log(String.format("Найден файл по пути %s", pathsList.get(0)));
    return pathsList.get(0).toFile();
  }

  /**
   * Получить путь к директории /build/resources
   *
   * @return путь к директории /build/resources
   */
  private static Path getBuildResourcesDirectory() {
    return Paths.get(
        System.getProperty("user.dir"),
        System.getenv("BASE_PATH") == null ? "build/resources" : System.getenv("BASE_PATH"));
  }

  /**
   * Метод для проверки что файл содержит строку
   *
   * @param file файл, в котором осуществялется поиск
   * @param value значение, которое ищем
   * @return boolean результат
   */
  public static boolean fileContainsString(File file, String value) {
    return getFileContent(file).contains(value);
  }

  /**
   * Получить файл по значению переменной fileVarOrFilePath (это может быть переменная сценария, в
   * которой сохранён сам файл, или путь до файла).
   *
   * @param fileVarOrFilePath переменная сценария, в которую сохранён файл, или путь до файла
   * @return файл
   */
  public static File getFile(String fileVarOrFilePath) {
    File file;
    Object value =
        AkitaScenario.getInstance().tryGetVar(fileVarOrFilePath.replace("${", "").replace("}", ""));
    if (value != null && value.getClass() == File.class) {
      file = (File) value;
    } else {
      String filePath = processValue(fileVarOrFilePath);
      file =
          filePath.contains("build/") || filePath.contains("resources/")
              ? getFileFromProject(filePath)
              : getFileFromResources(filePath);
    }
    return file;
  }

  /**
   * Метод рекурсивно достает все файлы из архива, т.е. если в архиве лежит еще один архив, он будет
   * также разархивирован.
   *
   * @param zipFile файл архива
   * @return список файлов из архива
   */
  public static List<File> unzip(File zipFile) {
    Path targetDir = Paths.get(zipFile.getParent(), FilenameUtils.getBaseName(zipFile.getName()));
    List<File> unzipFiles = new ArrayList<>();
    try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile))) {
      Files.createDirectories(targetDir);
      ZipEntry entry;
      while ((entry = zin.getNextEntry()) != null) {
        var filePath =
            Paths.get(targetDir.toString(), entry.getName().replaceAll("[:*?\"<>\\\\/]+", ""));
        var file = filePath.toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
          for (int c = zin.read(); c != -1; c = zin.read()) {
            fos.write(c);
          }
          fos.flush();
        }
        if (FilenameUtils.getExtension(file.getName()).equals("zip")) {
          unzipFiles.addAll(unzip(file));
        } else {
          unzipFiles.add(file);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Ошибка при распаковке архива", e);
    }
    return unzipFiles;
  }

  /**
   * Создать все папки по указанному пути относительнно {@link #BASE_PATH}.
   *
   * @param path путь относительно {@link #BASE_PATH}
   */
  public static Path createFolders(String path) {
    try {
      Path tempDir = Paths.get(BASE_PATH.toString(), path);
      return Files.createDirectories(tempDir);
    } catch (IOException ex) {
      throw new RuntimeException(
          "Не удалось создать все необходимые папки. Ощибка: " + ex.getMessage());
    }
  }
}
