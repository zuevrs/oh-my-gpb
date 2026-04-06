package ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.api.rest.requests;

import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Date;
import ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.UserLocalStorage;
import ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.api.rest.models.User;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;

/** Клас содержит запросы к Keycloak. */
public class KeycloakRequests {

  private static final String keycloakURL = ConfigLoader.getConfigValue("hosts.keycloak.url");
  private static final UserLocalStorage storage = UserLocalStorage.getInstance();

  public static RequestSpecification getKeycloakSpecification() {
    return given().baseUri(keycloakURL);
  }

  /**
   * Метод для получения токена Keycloak для конкретного пользователя по логину, паролю и client ID.
   *
   * @param clientId client ID пользователя.
   * @param login логин пользователя.
   * @param password пароль пользователя.
   * @return {@link String} Access token пользователя для использования в других запросах.
   */
  public static String getKeycloakToken(String clientId, String login, String password) {
    User user = storage.getUser(login, clientId);
    if (user != null && user.getAccessToken() != null && user.tokenNotExpired()) {
      return user.getAccessToken();
    } else {
      Response response =
          getKeycloakSpecification()
              .contentType("application/x-www-form-urlencoded; charset=UTF-8")
              .body(
                  String.format(
                      "grant_type=password&client_id=%s&username=%s&password=%s",
                      clientId, login, password))
              .post("auth/realms/ccl/protocol/openid-connect/token")
              .then()
              .statusCode(200)
              .extract()
              .response();

      String accessToken = response.jsonPath().getString("access_token");
      int expiredIn = response.jsonPath().getInt("expires_in");
      Date expiredAfter = new Date();
      expiredAfter.setTime(expiredAfter.getTime() + expiredIn * 1000L);

      user =
          User.builder()
              .clientId(clientId)
              .login(login)
              .password(password)
              .accessToken(accessToken)
              .expiresAfter(expiredAfter)
              .build();
      storage.addUser(user);

      return accessToken;
    }
  }

  /**
   * Метод для получения токена Keycloak для конкретного пользователя по логину, паролю,
   * client_secret и client ID.
   *
   * @param clientId client ID пользователя.
   * @param login логин пользователя.
   * @param password пароль пользователя.
   * @return {@link String} Access token пользователя для использования в других запросах.
   */
  public static String getKeycloakToken(
      String clientId, String login, String password, String secret) {
    User user = storage.getUser(login, clientId);
    if (user != null && user.getAccessToken() != null && user.tokenNotExpired()) {
      return user.getAccessToken();
    } else {
      Response response =
          getKeycloakSpecification()
              .contentType("application/x-www-form-urlencoded; charset=UTF-8")
              .body(
                  String.format(
                      "grant_type=password&client_id=%s&username=%s&password=%s&client_secret=%s",
                      clientId, login, password, secret))
              .post("auth/realms/ccl/protocol/openid-connect/token")
              .then()
              .statusCode(200)
              .extract()
              .response();

      String accessToken = response.jsonPath().getString("access_token");
      int expiredIn = response.jsonPath().getInt("expires_in");
      Date expiredAfter = new Date();
      expiredAfter.setTime(expiredAfter.getTime() + expiredIn * 1000L);

      user =
          User.builder()
              .clientId(clientId)
              .login(login)
              .password(password)
              .clientSecret(secret)
              .accessToken(accessToken)
              .expiresAfter(expiredAfter)
              .build();
      storage.addUser(user);

      return accessToken;
    }
  }

  /**
   * Метод для получения токена Keycloak для конкретного пользователя по сущности {@link User}.
   *
   * @param user объект типа {@link User}, содержащий логин, пароль и clientId.
   * @return {@link String} Access token пользователя для использования в других запросах.
   */
  public static String getKeycloakToken(User user) {
    if (user.haveSecret()) {
      return getKeycloakToken(
          user.getClientId(), user.getLogin(), user.getPassword(), user.getClientSecret());
    } else {
      return getKeycloakToken(user.getClientId(), user.getLogin(), user.getPassword());
    }
  }

  /**
   * Метод для получения токена Keycloak для пользователя по умолчанию.
   *
   * @return {@link String} Access token пользователя для использования в других запросах.
   */
  public static String getKeycloakToken() {
    return getKeycloakToken(new User("keycloakUser"));
  }

  public static String getKeycloakUserFullName(String username) {
    Response response =
        getKeycloakSpecification()
            .auth()
            .oauth2(getKeycloakToken())
            .queryParam("username", username)
            .get("auth/admin/realms/ccl/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
    if (response.path("attributes.fullName[0][0]") != null) {
      return response.path("attributes.fullName[0][0]");
    } else {
      return response.path("lastName[0]")
          + " "
          + response.path("firstName[0]")
          + " "
          + response.path("attributes.middleName[0][0]");
    }
  }
}
