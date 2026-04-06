package ru.gazprombank.automation.akitagpb.modules.ccl.condition.cucumber;

import io.cucumber.core.feature.FeatureParser;
import io.cucumber.core.gherkin.Pickle;
import java.net.URISyntaxException;
import java.util.UUID;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.api.AkitaScenario;

/**
 * Класс для получения объекта Сценария из фичи (в кукумбере такими объектами являются {@link
 * Pickle})
 */
public class PickleSupplier {

  private static FeatureParser featureParser;

  /**
   * Получить объект Сценария (Pickle) из фичи по имени feature-файла, тегу сценария или по имени
   * фичи + тегу сценария вместе. Имя feature-файла + тег сценария вместе: 'featureName~scenarioTag'
   * -> разделены тильдой '~'. Тег сценария: '~scenarioTag' -> строка начинается с тильды '~'. Имя
   * feature-файла: 'featureName' -> просто строка без тильды.
   *
   * @param featureNameAndTag - имя feature-файла, тег сценария или имя feature-файла + тег сценария
   *     вместе.
   * @return Pickle
   */
  public static Pickle getPickle(String featureNameAndTag) throws URISyntaxException {
    if (featureParser == null) {
      featureParser = new FeatureParser(UUID::randomUUID);
    }
    var featureResource = new FeatureResource(featureNameAndTag);
    var feature = featureParser.parseResource(featureResource).orElse(null);
    if (feature == null) {
      var message = "Ошибка при чтении фичи '" + featureResource.getUri() + "'";
      AkitaScenario.getInstance().log(message);
      throw new RuntimeException(message);
    }
    return featureResource.getScenarioTag() == null
        ? feature.getPickles().get(0)
        : feature.getPickles().stream()
            .filter(e -> e.getTags().contains(featureResource.getScenarioTag()))
            .findFirst()
            .orElseThrow();
  }
}
