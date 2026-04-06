package ru.gazprombank.automation.akitagpb.modules.core.hooks;

import static org.reflections.Reflections.log;
// import static
// ru.gazprombank.automation.akitagpb.modules.core.helpers.AllureHelper.allureReportCleaner;

import com.codeborne.selenide.logevents.SelenideLogger;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.restassured.AllureRestAssured;
import io.qameta.allure.selenide.AllureSelenide;
import io.restassured.RestAssured;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

@Slf4j
public class AllureHooks {

  @Before
  public void setupAllureSelenideListener() {
    if (!SelenideLogger.hasListener("AllureSelenide")) {
      SelenideLogger.addListener(
          "AllureSelenide",
          new AllureSelenide().screenshots(true).includeSelenideSteps(true).savePageSource(true));
    }
  }

  @Before
  public void setupAllureRestAssuredFilter() {
    if (RestAssured.filters().isEmpty()
        || !RestAssured.filters().stream()
            .anyMatch(filter -> filter instanceof AllureRestAssured)) {
      RestAssured.filters(new AllureRestAssured());
    }
  }

  @After(value = "@allureReportCleaner")
  public void allureReportCleanHook(Scenario scenario) throws IOException {
    log.info("#### ALLURE REPORT CLEAN HOOK ###");
    allureReportCleaner(
        new File("build/allure-results"),
        BaseMethods.getInstance()
            .getPropertyOrStringVariableOrValue(
                ScopedVariables.resolveVars("patternValuesForCleanAllure")));
  }

  @After
  public static void globalTumblerForCenzAllure() throws IOException {
    log.info("#### ALLURE REPORT CLEAN HOOK BY TUMBLER FOR ALL TESTS ###");
    if ((BaseMethods.getInstance()
            .getPropertyOrStringVariableOrValue(
                ScopedVariables.resolveVars("globalTumblerAllureCenz")))
        .equals("true")) {
      allureReportCleaner(
          new File("build/allure-results"),
          BaseMethods.getInstance()
              .getPropertyOrStringVariableOrValue(
                  ScopedVariables.resolveVars("patternValuesForCleanAllure")));
    }
  }

  /**
   * Метод проходится по указанной папке (например build/allure-results), ищет указанный паттерн и
   * удаляет полностью строку с данным паттерном
   */
  public static void allureReportCleaner(File directory, String pattern) throws IOException {
    long startTime = System.currentTimeMillis();

    String[] elementArray = pattern.split(", ");
    List<String> elementsList = Arrays.asList(elementArray);

    if (directory.isDirectory()) {
      try (Stream<File> filesStream = Arrays.stream(directory.listFiles())) {
        filesStream
            .parallel()
            .forEach(
                file -> {
                  try {
                    processFile(file, elementsList);
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                });
      }
    }

    long endTime = System.currentTimeMillis();
    log.info("Total time for cleaning Allure report: " + (endTime - startTime) + " ms");
  }

  private static void processFile(File file, List<String> elementsList) throws IOException {
    Path path = Paths.get(file.getPath());
    List<String> fileContent = new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));

    List<String> updateContent =
        fileContent.stream()
            .map(
                line ->
                    elementsList.stream().anyMatch(line::contains)
                        ? "*данные удалены для отчета*"
                        : line)
            .collect(Collectors.toList());
    Files.write(path, updateContent);
  }
}
