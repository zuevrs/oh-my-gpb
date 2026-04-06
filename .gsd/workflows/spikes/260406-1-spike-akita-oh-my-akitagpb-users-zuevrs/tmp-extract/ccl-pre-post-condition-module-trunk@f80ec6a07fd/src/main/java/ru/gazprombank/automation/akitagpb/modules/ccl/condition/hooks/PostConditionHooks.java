package ru.gazprombank.automation.akitagpb.modules.ccl.condition.hooks;

import static ru.gazprombank.automation.akitagpb.modules.ccl.condition.cucumber.RunFeatureHelper.runFeatures;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import java.util.Comparator;
import java.util.stream.Collectors;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class PostConditionHooks extends BaseMethods {

  public static final String POST_CONDITION_TAG = "@PostCondition";

  /**
   * After-хук для сценариев с тегом/тегами @PostCondition:<featureNameAndTag> Запускает все фичи
   * (шаги в этих фичах) с именами фич и/или тегами сценария <featureNameAndTag> (фичи должны
   * находиться в ресурсах проекта (main или test, в любой директории)). Запускаемые фичи
   * <featureNameAndTag> на своём сценарии должны иметь тег @NoInitBefore (чтобы этот сценарий
   * получил доступ к переменным основного теста).
   *
   * <p>Примеры: 1. только по имени feature-файла (в такой фиче должен быть только один сценарий):
   * - @PostCondition:example -> будет запущен сценарий из фичи example.feature (если она будет
   * найдена в ресурсах проекта); - @PostCondition:my_directory/example -> если в ресурсах несколько
   * фич с именем example.feature - путь то фичи нужно уточнить;
   *
   * <p>2. только по тегу сценария - тег указывается символом "тильда" -> '~' (в таком случае этот
   * тег должен быть уникальным для всех сценариев всех фич в проекте):
   * - @PostCondition:~KKKPLN-20585-after -> будет запущен сценарий, на котором есть тег
   * '@KKKPLN-20585-after' (если фича с таким тегом на сценарии будет найдена и такой сценарий будет
   * только 1);
   *
   * <p>3. по имени фичи и тегу сценария вместе - тогда имя фичи указывается как в п.1, сама фича
   * может содержать много сценариев, на сценариях должны висеть теги, уникальные только в рамках
   * этой одной фичи: - @PostCondition:example~test -> будет запущен сценарий с тегом '@test' из
   * фичи example.feature; - @PostCondition:my_directory/example~test -> то же самое, с уточнённым
   * путём до фичи
   *
   * <p>Порядок запуска постусловий, если их несколько: В тег можно добавить число по
   * шаблону @PostCondition:N:<featureNameAndTag> - приоритет запуска постусловия - тогда
   * постусловия будут запущены в соответствии с указанным порядком. Если приоритет не проставлен -
   * по дефолту он равен 0.
   * Пример: @PostCondition:3:example123~last @PostCondition:example123~test22 @PostCondition:-10:example123~last
   *
   * @param scenario текущий сценарий
   */
  @After(order = 2000)
  public void runFeatureInFeature(Scenario scenario) {
    var featureNamesAndTags =
        scenario.getSourceTagNames().stream()
            .filter(e -> e.startsWith(POST_CONDITION_TAG))
            .map(e -> e.substring(POST_CONDITION_TAG.length() + 1))
            .map(e -> e.matches("^-?\\d+:.+") ? e : "0:" + e)
            .sorted(Comparator.comparingInt(o -> Integer.parseInt(o.split(":")[0])))
            .collect(Collectors.toList());

    if (!featureNamesAndTags.isEmpty()) {
      akitaScenario.log("FEATURE-ФАЙЛЫ / ТЕГИ СЦЕНАРИЯ ДЛЯ ЗАПУСКА: " + featureNamesAndTags);
      featureNamesAndTags = featureNamesAndTags.stream().map(e -> e.split(":")[1]).toList();
      runFeatures(featureNamesAndTags);
    }
  }
}
