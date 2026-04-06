package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.restassured.response.Response;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.CamundaIncident;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.requests.CamundaRequests;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class GatheringLogsHooks extends BaseMethods {
    public static final String CAMUNDA_TAG = "@Camunda";

    /**
     * Хук, выполняемый после тестов с тэгом @Camunda, если тест завершился неудачно. Хук собирает все переменные и инциденты Camunda. Всю собранную
     * информацию прикрепляет к результатам теста в виде файла.
     */
    @After(value = CAMUNDA_TAG, order = 980)
    public void getLogsFromCamunda() {
        Scenario scenario = akitaScenario.getScenario();
        if (scenario.isFailed()) {
            StringBuilder camundaLogs = new StringBuilder();

            //пытаемся найти, есть ли у нас instanceId
            Object instanceId = akitaScenario.tryGetVar("instanceId");
            if (instanceId != null) {
                camundaLogs
                        //получаем перменные камунда
                        .append("Переменные инстанса с id \"").append(instanceId).append("\":\n")
                        .append(CamundaRequests.getProcessInstanceVariables(instanceId.toString()).body().prettyPrint()).append("\n")
                        //получаем возможные инциденты
                        .append("Инциденты инстанса с id \"").append(instanceId).append("\":\n")
                        .append(getCamundaIncident(instanceId.toString()));
            }

            //прикрепляем полученные логи к сценарию
            scenario.attach(camundaLogs.toString(), "text/plain", "camundaLogs");
        }
    }

    /**
     * Метод получает из Camunda все инцеденты по instanceId. Если инциденты найдены - для каждого пытается получить stackTrace.
     *
     * @param instanceId инстанс ID токена Camunda.
     * @return строку, содержащую все инцеденты с детальным stackTrace.
     */
    private String getCamundaIncident(String instanceId) {
        StringBuilder result = new StringBuilder();
        CamundaIncident[] incidents = CamundaRequests.getIncidentsByProcessInstanceId(instanceId);
        for (CamundaIncident incident : incidents) {
            var externalTaskId = incident.getConfiguration();
            if (externalTaskId != null && !externalTaskId.isBlank()) {
                Response response = CamundaRequests.getExternalTaskErrorDetailsByExternalTaskId(externalTaskId);
                incident.setErrorDetails(response.body().asString());
                result.append(incident).append("\n");
            }
        }
        return result.length() > 0 ? result.toString() : "Нет инцидентов";
    }
}
