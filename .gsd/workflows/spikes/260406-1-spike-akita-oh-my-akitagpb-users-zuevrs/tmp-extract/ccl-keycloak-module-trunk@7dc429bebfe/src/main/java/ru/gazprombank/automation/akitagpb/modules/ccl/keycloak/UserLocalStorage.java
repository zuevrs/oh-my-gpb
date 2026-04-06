package ru.gazprombank.automation.akitagpb.modules.ccl.keycloak;

import java.util.HashMap;
import java.util.Map;
import ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.api.rest.models.User;

public class UserLocalStorage {

  private static UserLocalStorage instance;
  private final Map<String, User> users = new HashMap<>();

  private UserLocalStorage() {}

  public static UserLocalStorage getInstance() {
    if (instance == null) {
      instance = new UserLocalStorage();
    }
    return instance;
  }

  public void addUser(User user) {
    users.put(user.getLogin() + "_" + user.getClientId(), user);
  }

  public User getUser(String login, String clientId) {
    return users.get(login + "_" + clientId);
  }

  public void removeUser(String login, String clientId) {
    users.remove(login + "_" + clientId);
  }

  public void clear() {
    users.clear();
  }
}
