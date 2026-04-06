package ru.gazprombank.automation.akitagpb.modules.core.cucumber.api;

import io.cucumber.java.Scenario;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables;

/**
 * Класс, связанный с AkitaScenario, используется для хранения страниц и переменных внутри сценария
 */
@Slf4j
public class AkitaEnvironment {

  private static final Logger LOGGER = LoggerFactory.getLogger(AkitaEnvironment.class);

  /** Сценарий (Cucumber.api), с которым связана среда */
  private Scenario scenario;

  /**
   * Переменные, объявленные пользователем внутри сценария ThreadLocal обеспечивает отсутствие
   * коллизий при многопоточном запуске
   */
  private ThreadLocal<ScopedVariables> variables = new ThreadLocal<>();

  public AkitaEnvironment(Scenario scenario) {
    this.scenario = scenario;
  }

  public AkitaEnvironment() {}

  /** Выводит дополнительный информационный текст в отчет (уровень логирования INFO) */
  public void log(Object object) {
    scenario.log(String.valueOf(object));
    LOGGER.info(String.valueOf(object));
  }

  public void log(String message, Object object) {
    scenario.log(message + ": \n" + object);
    LOGGER.info(message + ": \n" + object);
  }

  public ScopedVariables getVars() {
    return getVariables();
  }

  public Object getVar(String name) {
    return getVariables().get(name);
  }

  public void setVar(String name, Object object) {
    getVariables().put(name, object);
  }

  public Scenario getScenario() {
    return scenario;
  }

  public String replaceVariables(String textToReplaceIn) {
    return getVariables().replaceVariables(textToReplaceIn);
  }

  private ScopedVariables getVariables() {
    if (variables.get() == null) {
      variables.set(new ScopedVariables());
    }
    return variables.get();
  }
}
