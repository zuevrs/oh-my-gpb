package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.steps;

import io.cucumber.java.ru.И;
import ru.gazprombank.automation.akitagpb.modules.api.rest.RequestParam;
import ru.gazprombank.automation.akitagpb.modules.api.steps.ApiSteps;
import ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.UserLocalStorage;
import ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.api.rest.models.User;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

import java.util.ArrayList;
import java.util.List;

import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.addCamundaAccessToken;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.api.rest.requests.KeycloakRequests.getKeycloakToken;

public class CamundaSecureRequestsSteps extends BaseMethods {
    private static final ApiSteps apiSteps = new ApiSteps();

    /**
     * Шаг обёртка для REST запросов в Camunda. Нужен для добавления заголовка с авторизацией через Bearer token, если пользователь сам не добавил
     * этот заголовок.</br>
     * Все параметры соответствуют оригинальному шагу.
     */
    @И("^выполнен (GET|POST|PUT|DELETE|PATCH) запрос в Camunda на URL \"(.*)\". Полученный ответ сохранен в переменную \"(.*)\"$")
    public void sendCamundaRequest(String method, String address, String variableName) {
        apiSteps.sendHttpRequestSaveResponse(method, address, variableName, addCamundaAccessToken(new ArrayList<>()));
    }

    /**
     * Шаг обёртка для REST запросов в Camunda. Нужен для добавления заголовка с авторизацией через Bearer token, если пользователь сам не добавил
     * этот заголовок.</br>
     * Все параметры соответствуют оригинальному шагу.
     */
    @И("^выполнен (GET|POST|PUT|DELETE|PATCH) запрос в Camunda на URL \"(.*)\" с headers и parameters из таблицы. Полученный ответ сохранен в переменную \"(.*)\"")
    public void sendCamundaRequest(String method, String address, String variableName, List<RequestParam> paramsTable) {
        apiSteps.sendHttpRequestSaveResponse(method, address, variableName, addCamundaAccessToken(paramsTable));
    }

    /**
     * Шаг обёртка для REST запросов в Camunda. Нужен для добавления заголовка с авторизацией через Bearer token, если пользователь сам не добавил
     * этот заголовок.</br>
     * Все параметры соответствуют оригинальному шагу.
     */
    @И("^выполнен (GET|POST|PUT|DELETE|PATCH) запрос в Camunda на URL \"(.*)\". Ожидается код ответа: (\\d+)")
    public void sendCamundaRequest(String method, String address, int expectedStatusCode) {
        apiSteps.checkResponseCode(method, address, expectedStatusCode, addCamundaAccessToken(new ArrayList<>()));
    }

    /**
     * Шаг обёртка для REST запросов в Camunda. Нужен для добавления заголовка с авторизацией через Bearer token, если пользователь сам не добавил
     * этот заголовок.</br>
     * Все параметры соответствуют оригинальному шагу.
     */
    @И("^выполнен (GET|POST|PUT|DELETE|PATCH) запрос в Camunda на URL \"(.*)\" с headers и parameters из таблицы. Ожидается код ответа: (\\d+)")
    public void sendCamundaRequest(String method, String address, int expectedStatusCode, List<RequestParam> paramsTable) {
        apiSteps.checkResponseCode(method, address, expectedStatusCode, addCamundaAccessToken(paramsTable));
    }

    /**
     * Шаг запрашивает новый access token из Keycloak, который будет использоваться для всех запросов в Camunda.
     */
    @И("^обновить access token для всех Camunda шагов$")
    public void updateCamundaToken() {
        User camundaUser = new User("camundaRestUser");
        UserLocalStorage.getInstance().removeUser(camundaUser.getLogin(), camundaUser.getClientId());
        getKeycloakToken(camundaUser);
    }

    /**
     * Шаг устанавливает переданный access token в Качестве токена для всех запросов в Camunda.
     *
     * @param accessTokenVar имя переменной в которой хранится access token или само значение токена.
     */
    @И("^установить access token \"(.+)\" для всех Camunda шагов$")
    public void setCamundaAccessToken(String accessTokenVar) {
        User camundaUser = new User("camundaRestUser");
        camundaUser.setAccessToken(processValue(accessTokenVar));
        camundaUser.setUnlimited(true);
        UserLocalStorage.getInstance().addUser(camundaUser);
    }
}
