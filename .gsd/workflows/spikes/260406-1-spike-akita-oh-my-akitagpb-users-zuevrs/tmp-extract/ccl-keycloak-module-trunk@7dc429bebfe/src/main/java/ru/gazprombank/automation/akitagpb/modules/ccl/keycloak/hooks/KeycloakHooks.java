package ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.hooks;

import io.cucumber.java.Before;
import ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.UserLocalStorage;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class KeycloakHooks extends BaseMethods {

  /** Очистка хранилища токенов перед каждым тестом. */
  @Before
  public void cleanLocalStorage() {
    UserLocalStorage.getInstance().clear();
  }
}
