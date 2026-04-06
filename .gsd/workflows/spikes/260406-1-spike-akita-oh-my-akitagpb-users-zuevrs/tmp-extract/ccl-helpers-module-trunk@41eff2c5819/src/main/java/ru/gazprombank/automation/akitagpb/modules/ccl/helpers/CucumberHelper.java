package ru.gazprombank.automation.akitagpb.modules.ccl.helpers;

import io.cucumber.java.Scenario;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CucumberHelper {

  private CucumberHelper() {}

  /**
   * Метод для получения списка условий(post и pre condition) из фича-сценария
   *
   * @param scenario фича сценарий
   * @param tagName тег (@PostCondition, @PreCondition)
   * @return список тегов
   */
  public static List<String> getConditionTag(Scenario scenario, String tagName) {
    return scenario.getSourceTagNames().stream()
        .filter(e -> e.startsWith(tagName))
        .map(e -> e.substring(tagName.length() + 1))
        .map(e -> e.matches("^-?\\d+:.+") ? e : "0:" + e)
        .sorted(Comparator.comparingInt(o -> Integer.parseInt(o.split(":")[0])))
        .collect(Collectors.toList());
  }
}
