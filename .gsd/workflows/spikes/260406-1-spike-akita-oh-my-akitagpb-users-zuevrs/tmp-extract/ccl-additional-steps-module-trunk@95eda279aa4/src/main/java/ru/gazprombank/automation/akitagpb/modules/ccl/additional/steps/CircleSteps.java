package ru.gazprombank.automation.akitagpb.modules.ccl.additional.steps;

import static ru.gazprombank.automation.akitagpb.modules.api.steps.ApiBaseMethods.sendRequest;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.ru.И;
import io.restassured.response.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import ru.gazprombank.automation.akitagpb.modules.api.rest.RequestParam;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class CircleSteps extends BaseMethods {

  private static final int CIRCLE_TIMEOUT = 60;
  private static final int POLL_INTERVAL = 2;

  /**
   * Метод для многократной отправки запроса до получения нужного значения по jsonPath
   *
   * @param method Метод запроса
   * @param address url адрес
   * @param jsonPath путь в ответе
   * @param valueByJsonPath ожадаемое значение в ответе
   * @param paramsTable парметры запроса
   */
  @И(
      "^выполнен (GET|POST|PUT|DELETE|PATCH) запрос на URL \"(.*)\" с headers и parameters из таблицы. В ответе ожидаем что jsonPath \"(.*)\" равен \"(.*)\"$")
  public void sendHttpRequestSaveResponse2(
      String method,
      String address,
      String jsonPath,
      String valueByJsonPath,
      List<RequestParam> paramsTable) {

    String jsonPathVar = processValue(jsonPath);
    String value = processValue(valueByJsonPath);

    Awaitility.await()
        .pollInSameThread()
        .timeout(CIRCLE_TIMEOUT, TimeUnit.SECONDS)
        .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
        .until(
            () -> {
              Response response = sendRequest(method, address, paramsTable);
              akitaScenario.log(response.body().prettyPrint());
              Object object = JsonPath.read(response.body().prettyPrint(), jsonPathVar);
              String jsonElementValue = StringHelper.objectToJsonString(object);

              return value.equals(jsonElementValue);
            });
  }
}
