package ru.gazprombank.automation.akitagpb.modules.ccl.condition.hooks;

import static ru.gazprombank.automation.akitagpb.modules.ccl.condition.cucumber.RunFeatureHelper.runFeatures;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.CucumberHelper;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class PreConditionHooks extends BaseMethods {

  private static final String PRE_CONDITION_TAG = "@PreCondition";
  private static final Map<String, Map<String, Object>> PRECONDITION_STORAGE = new HashMap<>();
  private static final List<String> systemVariables =
      ConfigLoader.getConfigValue("system_variables.conf", "names");

  /**
   * Before-хук для сценариев с тегом/тегами @PreCondition:<featureNameAndTag> Запускает все фичи
   * (шаги в этих фичах) с именами фич и/или тегами сценария <featureNameAndTag> (фичи должны
   * находиться в ресурсах проекта (main или test, в любой директории)). Запускаемые фичи
   * <featureNameAndTag> на своём сценарии должны иметь тег @NoInitBefore (чтобы этот сценарий
   * получил доступ к переменным основного теста).
   *
   * <p>Примеры: 1. только по имени feature-файла (в такой фиче должен быть только один сценарий):
   * - @PreCondition:example -> будет запущен сценарий из фичи example.feature (если она будет
   * найдена в ресурсах проекта); - @PreCondition:my_directory/example -> если в ресурсах несколько
   * фич с именем example.feature - путь то фичи нужно уточнить;
   *
   * <p>2. только по тегу сценария - тег указывается символом "тильда" -> '~' (в таком случае этот
   * тег должен быть уникальным для всех сценариев всех фич в проекте):
   * - @PreCondition:~KKKPLN-20585-after -> будет запущен сценарий, на котором есть тег
   * '@KKKPLN-20585-after' (если фича с таким тегом на сценарии будет найдена и такой сценарий будет
   * только 1);
   *
   * <p>3. по имени фичи и тегу сценария вместе - тогда имя фичи указывается как в п.1, сама фича
   * может содержать много сценариев, на сценариях должны висеть теги, уникальные только в рамках
   * этой одной фичи: - @PreCondition:example~test -> будет запущен сценарий с тегом '@test' из фичи
   * example.feature; - @PreCondition:my_directory/example~test -> то же самое, с уточнённым путём
   * до фичи
   *
   * <p>Порядок запуска постусловий, если их несколько: В тег можно добавить число по
   * шаблону @PreCondition:N:<featureNameAndTag> - приоритет запуска постусловия - тогда постусловия
   * будут запущены в соответствии с указанным порядком. Если приоритет не проставлен - по дефолту
   * он равен 0.
   * Пример: @PreCondition:3:example123~last @PreCondition:example123~test22 @PreCondition:-10:example123~last
   */
  @Before(order = 20)
  public void runPreconditionFeature(Scenario scenario) {

    String issueKeyTag = scenario.getSourceTagNames().stream().findFirst().orElse(null);
    var featureNameAndTag = CucumberHelper.getConditionTag(scenario, PRE_CONDITION_TAG);

    if (issueKeyTag == null || featureNameAndTag.isEmpty()) {
      return;
    }

    if (!PRECONDITION_STORAGE.containsKey(issueKeyTag)) {

      akitaScenario.log("FEATURE-ФАЙЛЫ / ТЕГИ СЦЕНАРИЯ ДЛЯ ЗАПУСКА: " + featureNameAndTag);
      featureNameAndTag = featureNameAndTag.stream().map(e -> e.split(":")[1]).toList();
      runFeatures(featureNameAndTag);

      Map<String, Object> preconditionVars =
          akitaScenario.getVars().getVariables().entrySet().stream()
              .filter(e -> !systemVariables.contains(e.getKey()))
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

      PRECONDITION_STORAGE.put(issueKeyTag, new HashMap<>(preconditionVars));
      return;
    }

    PRECONDITION_STORAGE.get(issueKeyTag).forEach((key, value) -> akitaScenario.setVar(key, value));
  }
}
