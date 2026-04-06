package ru.gazprombank.automation.akitagpb.modules.ccl.condition.cucumber;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.getStackTrace;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.core.steps.isteps.IBaseMethods.akitaScenario;

import java.util.List;

public class RunFeatureHelper {

  /**
   * Вспомогательный метод для запуска cucumber-feature
   *
   * @param featureNamesAndTags список тегов и фич
   */
  public static void runFeatures(List<String> featureNamesAndTags) {
    var runner = RunnerCreator.getRunner();
    for (String nameOrTag : featureNamesAndTags) {
      try {
        var pickle = PickleSupplier.getPickle(processValue(nameOrTag));
        akitaScenario.log("\nЗАПУСК СЦЕНАРИЯ '" + nameOrTag + "'");
        runner.runPickle(pickle);
      } catch (Exception e) {
        akitaScenario.log(
            "\nОШИБКА! СЦЕНАРИЙ '"
                + nameOrTag
                + "' НЕ БУДЕТ ЗАПУЩЕН. \n"
                + e.getMessage()
                + "\n"
                + getStackTrace(e));
      }
    }
  }
}
