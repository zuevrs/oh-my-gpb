package ru.gazprombank.automation.akitagpb.modules.ccl.helpers;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.FileHelper.getFileContent;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CSVHelper {

  /**
   * Метод получает данные из csv файла и преобразует к формату DataTable для сценария cucumber
   *
   * @param path читаемый csv файл
   * @return список строк в формате DataTable
   */
  public static List<String> convertCsvFileToDataTableFormat(Path path) {
    try {
      CSVParser csvParser = new CSVParserBuilder().withSeparator(',').build();
      CSVReader reader =
          new CSVReaderBuilder(new StringReader(getFileContent(path)))
              .withCSVParser(csvParser)
              .build();
      return reader.readAll().stream()
          .map(line -> "| " + Arrays.stream(line).reduce((x, y) -> x + " | " + y).get() + " |")
          .collect(Collectors.toList());
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Не найден файл csv " + path.getFileName());
    } catch (IOException e) {
      throw new RuntimeException("Ошибка чтения файла csv " + path.getFileName());
    }
  }
}
