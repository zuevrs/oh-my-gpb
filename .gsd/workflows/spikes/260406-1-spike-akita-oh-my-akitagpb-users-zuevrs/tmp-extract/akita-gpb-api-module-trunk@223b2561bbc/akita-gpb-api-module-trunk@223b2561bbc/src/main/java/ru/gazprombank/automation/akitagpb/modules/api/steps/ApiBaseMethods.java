package ru.gazprombank.automation.akitagpb.modules.api.steps;

import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.messageContainsRegexp;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables.resolveVars;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.config.SSLConfig;
import io.restassured.http.Method;
import io.restassured.internal.support.Prettifier;
import io.restassured.response.Response;
import io.restassured.specification.RequestSender;
import io.restassured.specification.RequestSpecification;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.conn.ssl.SSLSocketFactory;
import ru.gazprombank.automation.akitagpb.modules.api.rest.RequestParam;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.api.AkitaScenario;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.PropertyLoader;
import ru.gazprombank.automation.akitagpb.modules.core.hooks.statistics.StatisticsHooks;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

/** Общие методы, используемые в различных шагах */
@Slf4j
public class ApiBaseMethods extends BaseMethods {

  /**
   * Создание запроса
   *
   * @param paramsTable массив с параметрами
   * @return сформированный запрос
   */
  public static RequestSender createRequest(List<RequestParam> paramsTable) {
    String body;
    RequestSpecification request = given();

    var isContainSsl =
        paramsTable.stream().anyMatch(param -> param.getType().name().startsWith("SSL_"));
    if (isContainSsl) {
      // Добавляем ssl конфиг если в feature есть параметры начинающиеся с SSL_
      SSLConfig sslConfig = getSslConfig(paramsTable);
      if (sslConfig != null) {
        request.config(RestAssured.config().sslConfig(sslConfig));
      }
    }

    paramsTable.removeIf(param -> param.getType().name().startsWith("SSL_"));

    for (RequestParam requestParam : paramsTable) {
      String name = requestParam.getName();
      String value = requestParam.getValue();
      // в случае, когда для каждого multipart задается собственный content-type, например
      // "file;application/pdf"
      String[] nameWithContentType;
      switch (requestParam.getType()) {
        case PARAMETER:
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          request.queryParam(name, value);
          break;
        case HEADER:
          if (value == null || value.isEmpty()) {
            request.header(name, "");
          } else {
            value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
            value = resolveVars(value);
            request.header(name, value);
          }
          break;
        case COOKIE:
          if (value == null || value.isEmpty()) {
            request.cookie(name, "");
          } else {
            value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
            value = resolveVars(value);
            request.cookie(name, value);
          }
          break;
        case API_SPEC:
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          OpenApiInteractionValidator validator =
              OpenApiInteractionValidator.createForSpecificationUrl(value)
                  .withWhitelist(
                      ValidationErrorsWhitelist.create()
                          .withRule(
                              "actualTimestamp has true format",
                              messageContainsRegexp("actualTimestamp")))
                  .build();
          OpenApiValidationFilter validationFilter = new OpenApiValidationFilter(validator);
          request.filter(validationFilter);
          break;
        case NO_SSL_CHECK:
          request.relaxedHTTPSValidation();
          break;
        case VAR:
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          AkitaScenario.getInstance().setVar(name, value);
          break;
        case BODY:
          value = PropertyLoader.loadValueFromFileOrPropertyOrVariableOrDefault(resolveVars(value));
          body = resolveVars(value);
          request.body(body);
          break;
        case BODY_BYTE:
          byte[] bodyByte = (byte[]) AkitaScenario.getInstance().getVar(value);
          request.body(bodyByte);
          break;
        case MULTIPART:
          if (value == null || value.isEmpty()) {
            value = "";
          } else {
            value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
            value = resolveVars(value);
          }
          nameWithContentType = name.split(";");
          // если в имени multipart'а задан content-type, то добавляем его в запрос
          if (nameWithContentType.length > 1) {
            request.multiPart(nameWithContentType[0].trim(), value, nameWithContentType[1].trim());
          } else {
            request.multiPart(name, value);
          }
          break;
        case MULTIPART_UTF8:
          if (value == null || value.isEmpty()) {
            value = "";
          } else {
            value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
            value = resolveVars(value);
          }
          nameWithContentType = name.split(";");
          if (nameWithContentType.length > 1) {
            request.multiPart(
                new MultiPartSpecBuilder(value)
                    .controlName(nameWithContentType[0].trim())
                    .charset("UTF-8")
                    .mimeType(nameWithContentType[1].trim())
                    .emptyFileName()
                    .build());
          } else {
            request.multiPart(
                new MultiPartSpecBuilder(value)
                    .controlName(name)
                    .charset("UTF-8")
                    .emptyFileName()
                    .build());
          }
          break;
        case MULTIPART_FILE:
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          nameWithContentType = name.split(";");
          if (nameWithContentType.length > 1) {
            request.multiPart(
                nameWithContentType[0].trim(),
                new File(FilenameUtils.normalize(value)),
                nameWithContentType[1].trim());
          } else {
            request.multiPart(name, new File(FilenameUtils.normalize(value)));
          }
          break;
        case MULTIPART_FILE_CONTENT:
          // получаем резолвленный путь до файла
          String path = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          path = resolveVars(path);
          // получаем имя файла
          String fileName = Paths.get(path).getFileName().toString();
          // получаем содержимое файла и резолвим переменные в нём
          String content = PropertyLoader.loadValueFromFileOrPropertyOrVariableOrDefault(path);
          content = resolveVars(content);
          nameWithContentType = name.split(";");
          if (nameWithContentType.length > 1) {
            request.multiPart(
                nameWithContentType[0].trim(),
                fileName,
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                nameWithContentType[1].trim());
          } else {
            request.multiPart(
                name, fileName, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
          }
          break;
        case FORM_PARAM:
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          request.formParam(name, value);
          break;
        default:
          throw new IllegalArgumentException(
              String.format(
                  "Некорректно задан тип %s для параметра запроса %s ",
                  requestParam.getType(), name));
      }
    }
    return request;
  }

  /**
   * Метод возвращает ssl конфиг с полученными из таблицы параметрами ssl
   *
   * @param paramsTable параметры указанные в запросе
   * @return SSLConfig
   */
  private static SSLConfig getSslConfig(List<RequestParam> paramsTable) {

    String sslCertificatePath = "";
    String sslCertificatePassword = "";
    String sslCertificateType = "PKCS12";

    List<RequestParam> sslParams =
        paramsTable.stream()
            .filter(param -> param.getType().name().startsWith("SSL_"))
            .collect(Collectors.toList());
    for (RequestParam requestParam : sslParams) {
      String value = requestParam.getValue();
      value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
      value = resolveVars(value);
      switch (requestParam.getType()) {
        case SSL_CERTIFICATE_FILE:
          sslCertificatePath = value;
          break;
        case SSL_CERTIFICATE_PASSWORD:
          sslCertificatePassword = value;
          break;
        case SSL_CERTIFICATE_TYPE:
          sslCertificateType = value;
          break;
      }
    }

    if (sslCertificatePath.isEmpty()) {
      AkitaScenario.getInstance().log("Не задан путь до сертификата");
      return null;
    }

    try {
      KeyStore keyStore = KeyStore.getInstance(sslCertificateType);
      keyStore.load(
          new FileInputStream(
              Paths.get(FilenameUtils.separatorsToSystem(sslCertificatePath)).toFile()),
          sslCertificatePassword.toCharArray());

      SSLSocketFactory sslSocketFactory = new SSLSocketFactory(keyStore, sslCertificatePassword);
      sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
      SSLConfig sslConfig = new SSLConfig().sslSocketFactory(sslSocketFactory);
      return sslConfig;
    } catch (Exception e) {
      AkitaScenario.getInstance().log("Ошибка при загрузке ssl сертификата");
    }

    return null;
  }

  /**
   * Получает Response из ответа и сохраняет в переменную
   *
   * @param variableName имя переменной, в которую будет сохранен Response
   * @param response ответ от http запроса
   */
  public void getResponseAndSaveToVariable(String variableName, Response response) {
    akitaScenario.setVar(variableName, response);
  }

  /**
   * Получает body из ответа и сохраняет в переменную
   *
   * @param variableName имя переменной, в которую будет сохранен ответ
   * @param response ответ от http запроса
   */
  public void getBodyAndSaveToVariable(String variableName, Response response) {
    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      akitaScenario.setVar(variableName, response.getBody().asString());
      akitaScenario.log(
          "Тело ответа : \n" + new Prettifier().getPrettifiedBodyIfPossible(response, response));
    } else {
      fail(
          "Некорректный ответ на запрос: "
              + new Prettifier().getPrettifiedBodyIfPossible(response, response));
    }
  }

  /**
   * Сравнение кода http ответа с ожидаемым
   *
   * @param response ответ от сервиса
   * @param expectedStatusCode ожидаемый http статус код
   * @return возвращает true или false в зависимости от ожидаемого и полученного http кодов
   */
  public boolean checkStatusCode(Response response, int expectedStatusCode) {
    int statusCode = response.getStatusCode();
    if (statusCode != expectedStatusCode) {
      akitaScenario.log(
          "Получен неверный статус код ответа "
              + statusCode
              + ". Ожидаемый статус код "
              + expectedStatusCode);
      if (!response.body().asString().isBlank()) {
        akitaScenario.log("Тело ответа: " + response.body().asString());
      }
    }
    return statusCode == expectedStatusCode;
  }

  /**
   * Отправка http запроса
   *
   * @param method тип http запроса
   * @param address url, на который будет направлен запроc
   * @param paramsTable список параметров для http запроса
   */
  public static Response sendRequest(
      String method, String address, List<RequestParam> paramsTable) {
    address = PropertyLoader.loadProperty(address, resolveVars(address));
    StatisticsHooks.getInstance().addEndpoint(address);
    RequestSender request = createRequest(paramsTable);
    return request.request(Method.valueOf(method), address);
  }

  /**
   * Отправка http GET запроса(Server Sent Events)
   *
   * @param address url, на который будет направлен запроc
   */
  @SneakyThrows
  public String sendGetSseRequest(String address) {
    address = PropertyLoader.loadProperty(address, resolveVars(address));
    StatisticsHooks.getInstance().addEndpoint(address);
    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder(new URI(address)).GET().build();
    return client
        .send(request, HttpResponse.BodyHandlers.ofLines())
        .body()
        .findFirst()
        .map(Objects::toString)
        .orElse("");
  }

  protected Configuration createJsonPathConfiguration() {
    return new Configuration.ConfigurationBuilder()
        .jsonProvider(new GsonJsonProvider())
        .mappingProvider(new GsonMappingProvider())
        .build();
  }
}
