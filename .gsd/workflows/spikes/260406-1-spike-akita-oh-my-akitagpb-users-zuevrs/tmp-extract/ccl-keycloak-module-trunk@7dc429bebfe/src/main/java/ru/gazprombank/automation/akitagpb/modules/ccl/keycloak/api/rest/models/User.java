package ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.api.rest.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;

/** Класс-модель для хранения и использовании информации о пользователе. */
@Setter
@Getter
@Builder
@AllArgsConstructor
public class User {

  private String clientId;
  private String login;
  private String password;
  private String clientSecret;
  private String accessToken;
  private Date expiresAfter;
  private Boolean unlimited;

  /**
   * Конструктор через имя пользователя, информация о котором хранится в файле конфигурации.
   *
   * @param userName имя пользователя.
   */
  public User(String userName) {
    Map<String, String> userData = ConfigLoader.getConfigValue("users." + userName);
    assertThat(userData).as("Не найден пользователь с именем " + userName).isNotNull();
    this.clientId = userData.get("client_id");
    this.login = userData.get("login");
    this.password = userData.get("password");
    this.clientSecret = userData.get("client_secret");
  }

  public boolean haveSecret() {
    return clientSecret != null;
  }

  public boolean tokenNotExpired() {
    if (unlimited == null) {
      unlimited = false;
    }
    if (expiresAfter == null) {
      return false;
    }
    Date now = new Date();
    return unlimited || now.before(expiresAfter);
  }

  public void setTokenExpiresAfter(int seconds) {
    if (expiresAfter == null) {
      expiresAfter = new Date();
    }
    expiresAfter.setTime(new Date().getTime() + seconds * 1000L);
  }
}
