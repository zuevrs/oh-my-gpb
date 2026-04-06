package ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.steps;

import static ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.api.rest.requests.KeycloakRequests.getKeycloakToken;

import io.cucumber.java.ru.И;
import ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.api.rest.models.User;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class KeycloakSteps extends BaseMethods {

  /**
   * Шаг выполняет REST запрос, для получения access token из Keycloak, сохраняет его в локальное
   * хранилище и в переменную сценария.
   *
   * @param userName имя пользователя из Config файла.
   * @param varName имя переменной, в которую будет сохранён access token.
   */
  @И(
      "^получить access token из Keycloak для пользователя с именем \"(.+)\" и сохранить в переменную \"(.+)\"$")
  public void getAccessToken(String userName, String varName) {
    User user = new User(userName);
    String accessToken = getKeycloakToken(user);
    akitaScenario.setVar(varName, accessToken);
    akitaScenario.log(String.format("Значение %s: %s", varName, accessToken));
  }
}
