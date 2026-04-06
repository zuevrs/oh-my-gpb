package ru.gazprombank.automation.akitagpb.modules.core.hooks;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.extern.slf4j.Slf4j;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.api.AkitaEnvironment;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

@Slf4j
public class InitialSetupHooks extends BaseMethods {

  /**
   * Создает окружение(среду) для запуска сценария (если на сценарии нет тега @NoInitBefore)
   *
   * @param scenario сценарий
   */
  @Before(value = "not @NoInitBefore", order = 10)
  public void setScenario(Scenario scenario) {
    akitaScenario.setEnvironment(new AkitaEnvironment(scenario));
    akitaScenario.setSystemVar("scenarioCoreStartTime", System.currentTimeMillis());
  }
}
