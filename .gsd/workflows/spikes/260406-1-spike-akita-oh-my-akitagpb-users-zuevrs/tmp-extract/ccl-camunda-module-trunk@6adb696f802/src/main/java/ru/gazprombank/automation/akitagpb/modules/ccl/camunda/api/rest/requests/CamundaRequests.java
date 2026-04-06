package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.requests;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import io.restassured.response.Response;
import io.restassured.specification.MultiPartSpecification;
import io.restassured.specification.RequestSpecification;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.ActivityInstanceStep;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.CamundaIncident;
import ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.api.rest.models.User;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.FileHelper.getResourcesFileContent;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.NumberHelper.parseDouble;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.getStringValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;
import static ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.api.rest.requests.KeycloakRequests.getKeycloakToken;

public class CamundaRequests {

    public static final String camundaBaseURL = ConfigLoader.getConfigValue("camunda.base.url");
    private static final Integer camundaIncidentsTimeout = ConfigLoader.getConfigValueOrDefault("camunda.incidents.timeout", 5);
    private static final Integer camundaPollInterval = ConfigLoader.getConfigValueOrDefault("camunda.activityInstance.pollInterval", 1);

    public static RequestSpecification getCamundaSpecification() {
        return given().baseUri(camundaBaseURL)
                .auth().oauth2(getKeycloakToken(new User("camundaRestUser")));
    }

    /**
     * Получить спискр всех активных инстансов (с сортировкой по увеличению вермени завершения), которые были пройдены, по ProcessInstanceId.
     *
     * @param processInstanceId instance id процесса.
     * @return список пройденных активных инстансов
     */
    public static List<ActivityInstanceStep> getSortedActivityInstanceHistoryByProcessInstanceId(String processInstanceId) {
        var historyArray = getCamundaSpecification()
                .queryParam("processInstanceId", processInstanceId)
                .queryParam("sortBy", "startTime")
                .queryParam("sortOrder", "asc")
                .when()
                .get("history/activity-instance")
                .then()
                .statusCode(200)
                .extract().as(ActivityInstanceStep[].class);
        return Arrays.asList(historyArray);
    }

    /**
     * Получение активных инстансов для процесса с требуемым instanceId.
     *
     * @param instanceId instanceId процесса.
     * @return Response (ответ на запрос process-instance/{instanceId}/activity-instances)
     */
    public static Response getActivityInstancesByProcessInstanceId(String instanceId) {
        return getCamundaSpecification()
                .when()
                .get("process-instance/{instanceId}/activity-instances", instanceId)
                .then()
                .statusCode(200)
                .extract().response();
    }

    /**
     * Получение массива processInstances - активных инстансов процессов по businessKey процесса.
     *
     * @param businessKey businessKey процесса.
     * @return Response запроса массива processInstances активных инстансов процессов по businessKey процесса
     */
    public static Response getProcessInstancesByBusinessKey(String businessKey) {
        return getCamundaSpecification()
                .queryParam("businessKey", businessKey)
                .when()
                .get("process-instance")
                .then()
                .extract().response();
    }

    /**
     * Удаляем заявку на процессе в Camunda.
     *
     * @param instanceId id инстанса процесса.
     */
    public static Response deleteProcessInstanceAndGetResponse(String instanceId) {
        return getCamundaSpecification()
                .queryParam("skipCustomListeners", "true")
                .queryParam("skipIoMappings", "true")
                .when()
                .delete("process-instance/{instanceId}", instanceId);
    }

    /**
     * Удаляем заявку на процессе в Camunda и проверяем статус ответа 204.
     *
     * @param instanceId id инстанса процесса.
     */
    public static void deleteProcessInstance(String instanceId) {
        deleteProcessInstanceAndGetResponse(instanceId)
                .then()
                .statusCode(204);
    }

    /**
     * Удаляем схему процесса или DMN в Camunda.
     *
     * @param deployId id деплоя схемы/DMN.
     */
    public static Response deleteCamundaDeploy(String deployId, boolean isCascade) {
        return getCamundaSpecification()
                .queryParam("cascade", String.valueOf(isCascade))
                .queryParam("skipCustomListeners", "true")
                .queryParam("skipIoMappings", "true")
                .when()
                .delete("deployment/{deployId}", deployId);
    }

    /**
     * Удаляем схему процесса или DMN в Camunda, адрес которой указан через параметр.
     *
     * @param deployId id деплоя схемы/DMN.
     */
    public static Response deleteCustomCamundaDeploy(String camundaBaseURL, String deployId) {
        return given().baseUri(camundaBaseURL)
                .queryParam("cascade", "true")
                .queryParam("skipCustomListeners", "true")
                .queryParam("skipIoMappings", "true")
                .when()
                .delete("deployment/{deployId}", deployId);
    }

    /**
     * Получить все переменные инстанса процесса по processInstanceId
     *
     * @param processInstanceId id инстанса процесса.
     * @return Response (ответ на запрос process-instance/{instanceId}/variables).
     */
    public static Response getProcessInstanceVariables(String processInstanceId) {
        return getCamundaSpecification()
                .when()
                .get("process-instance/{instanceId}/variables", processInstanceId)
                .then()
                .statusCode(200)
                .extract().response();
    }

    /**
     * Получить все переменные инстанса процесса по processInstanceId в виде {@link Map}.
     *
     * @param processInstanceId id инстанса процесса.
     * @return {@link Map} с набором переменных вида: ключ - имя пересенной, значение - {@link Map} содержщий type и value.
     */
    public static Map<String, String> getProcessInstanceVariablesAsMap(String processInstanceId) {
        Map<String, String> result = new HashMap<>();
        Map<String, Map<String, Object>> response = getCamundaSpecification()
                .queryParam("deserializeValues", false)
                .when()
                .get("process-instance/{instanceId}/variables", processInstanceId)
                .then()
                .statusCode(200)
                .extract().jsonPath(new JsonPathConfig(NumberReturnType.DOUBLE)).getMap("$");
        response.forEach((key, value) -> {
            Object rawValue = value.get("value");
            try {
                if (rawValue == null) {
                    result.put(key, "null");
                } else {
                    //получаем именно то, что написано в JSON
                    String stringValue = getStringValue(rawValue);
                    //если тип Double, то проверяем, то обрабатываем записб видо 0.00Е3 в обычный вид
                    if (value.get("type").equals("Double")) {
                        stringValue = parseDouble(stringValue);
                    }
                    result.put(key, stringValue);
                }
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(String.format("Не удалось преобразовать значение %s в текст!", rawValue));
            }
        });
        return result;
    }

    /**
     * Получить массив всех инцидентов по ProcessInstanceId.
     *
     * @param processInstanceId instance id процесса.
     * @return массив инцидентов.
     */
    public static CamundaIncident[] getIncidentsByProcessInstanceId(String processInstanceId) {
        return getCamundaSpecification()
                .queryParam("processInstanceId", processInstanceId)
                .when()
                .get("incident")
                .then()
                .statusCode(200)
                .extract().as(CamundaIncident[].class);
    }

    /**
     * Получить массив всех инцидентов по ProcessInstanceId с ожиданием указанного количества секунд.
     *
     * @param processInstanceId instance id процесса
     * @param timeout           время ожидания инцидента на процессе
     * @return массив инцидентов
     */
    public static CamundaIncident[] waitAndGetIncidentsByProcessInstanceId(String processInstanceId, Integer timeout) {
        timeout = timeout != null ? timeout : camundaIncidentsTimeout;
        AtomicReference<CamundaIncident[]> result = new AtomicReference<>();
        try {
            Awaitility.await()
                    .pollInSameThread()
                    .timeout(timeout, TimeUnit.SECONDS)
                    .pollInterval(camundaPollInterval, TimeUnit.SECONDS)
                    .until(() -> {
                        result.set(getIncidentsByProcessInstanceId(processInstanceId));
                        return result.get().length > 0;
                    });
        } catch (ConditionTimeoutException ignore) {
        }
        return result.get();
    }

    /**
     * Получить инцидент по его Id.
     *
     * @param id id инцидента.
     * @return инцидент.
     */
    public static CamundaIncident getIncidentById(String id) {
        return getCamundaSpecification()
                .pathParam("id", id)
                .when()
                .get("incident/{id}")
                .then()
                .statusCode(200)
                .extract().as(CamundaIncident.class);
    }

    /**
     * Получить stack-trace ошибки задачи external-task по externalTaskId.
     *
     * @param externalTaskId id external-task'и.
     * @return Response (ответ на запрос external-task/{externalTaskId}/errorDetails).
     */
    public static Response getExternalTaskErrorDetailsByExternalTaskId(String externalTaskId) {
        return getCamundaSpecification()
                .when()
                .get("external-task/{externalTaskId}/errorDetails", externalTaskId)
                .then()
                .statusCode(200)
                .extract().response();
    }

    /**
     * Задеплоить BPMN-схему или DMN-таблицу в Camunda c указанием deployment-name.
     *
     * @param filePath путь до файла.
     * @return Response (ответ на запрос /deployment/create).
     */
    public static Response deployCamundaFiles(String filePath, String name) {
        return getCamundaSpecification()
                .given()
                .header("Content-Type", "multipart/form-data")
                .multiPart("upload", new File(filePath))
                .multiPart("deployment-name", name)
                .when()
                .post("/deployment/create")
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    /**
     * Задеплоить BPMN-схему или DMN-таблицу в Camunda.
     *
     * @param filePath путь до файла.
     * @return Response (ответ на запрос /deployment/create).
     */
    public static Response deployCamundaFiles(String filePath) {
        return getCamundaSpecification()
                .given()
                .header("Content-Type", "multipart/form-data")
                .multiPart("upload", new File(filePath))
                .when()
                .post("/deployment/create")
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    /**
     * Задеплоить BPMN-схему или DMN-таблицу в Camunda.
     *
     * @param contentBody содержимое файла файла.
     * @return Response (ответ на запрос /deployment/create).
     */
    public static Response deployCamundaContentBodyFile(String contentBody) {

        MultiPartSpecification multiPartSpecification = new MultiPartSpecBuilder(contentBody.getBytes())
                .fileName("file.bpmn")
                .controlName("file")
                .build();

        return getCamundaSpecification()
                .given()
                .header("Content-Type", "multipart/form-data")
                .multiPart(multiPartSpecification)
                .when()
                .post("/deployment/create")
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    /**
     * Получить список всех переменных из истории.
     *
     * @param processInstanceId instance id процесса.
     * @return Response (массив переменных).
     */
    public static Response variableInstanceHistory(String processInstanceId) {
        return getCamundaSpecification()
                .queryParam("executionIdIn", processInstanceId)
                .queryParam("deserializeValues", false)
                .when()
                .get("history/variable-instance")
                .then()
                .statusCode(200)
                .extract().response();
    }

    /**
     * Обновить переменные процесса.
     *
     * @param processInstanceId instance id процесса
     * @param requestBody       тело запроса
     */
    public static void updateProcessVariables(String processInstanceId, String requestBody) {
        getCamundaSpecification()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("process-instance/{instanceId}/variables", processInstanceId)
                .then()
                .statusCode(204);
    }

    /**
     * Выполнить запрос POST external-task/fetchAndLock
     *
     * @param requestBody тело запроса
     * @return ответ на запрос
     */
    public static Response fetchAndLock(String requestBody) {
        return getCamundaSpecification()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("external-task/fetchAndLock");
    }

    /**
     * Получить external-task'и инстанса процесса запросом POST external-task
     *
     * @return ответ на запрос
     */
    public static Response getExternalTaskFromProcessId() {
        return getCamundaSpecification()
                .contentType(ContentType.JSON)
                .body(resolveVariables(getResourcesFileContent(processValue("${test.data.base.path}/common/camunda/externalTaskFromProcessId.json"))))
                .when()
                .post("/external-task")
                .then()
                .statusCode(200)
                .extract().response();
    }

    /**
     * Выполнить таску запросом POST /external-task/{taskId}/complete
     *
     * @param taskId id таски
     * @return ответ на запрос
     */
    public static Response completeTaskByExternalTaskId(String taskId) {
        return getCamundaSpecification()
                .contentType(ContentType.JSON)
                .body(getResourcesFileContent(processValue("${test.data.base.path}/common/camunda/complete.json")))
                .when()
                .post("/external-task/{taskId}/complete", taskId);
    }

    /**
     * Выполнить запрос GET process-instance/
     *
     * @param processInstanceId instanceId процесса.
     * @return Response (ответ на запрос process-instance/{instanceId})
     */
    public static Response getProcessInstance(String processInstanceId) {
        return getCamundaSpecification()
                .when()
                .get("process-instance/{processInstanceId}", processInstanceId)
                .then()
                .extract().response();
    }

    /**
     * Получить информацию о последнем Definition по Definition Key
     *
     * @param key Definition Key процесса.
     * @return Response (ответ на запрос process-definition)
     */
    public static Response getLatestDefinitionByKey(String key) {
        return getCamundaSpecification()
                .queryParam("key", key)
                .queryParam("latestVersion", true)
                .when()
                .get("process-definition")
                .then()
                .statusCode(200)
                .extract().response();
    }

    /**
     * Получить список всех instanceId по businessKey
     *
     * @param businessKey переменная businessKey.
     * @return Response (ответ на запрос process-instance)
     */
    public static Response getProcessInstanceByBusinessKey(String businessKey) {
        return getCamundaSpecification()
                .when()
                .queryParam("businessKey", businessKey)
                .get("process-instance/")
                .then()
                .statusCode(200)
                .extract().response();
    }

    /**
     * Запускаем новый токен на процессе с указанным processDefinitionId и возвращаем его instanceId.
     *
     * @param processDefinitionId id нужного процесса.
     * @param body содержимое тела запроса. Должно осдержать все необхидимые данные для создания нового токена.
     * @return instanceId в формате {@link String}
     */
    public static String startCamundaInstance(String processDefinitionId, String body) {
        return getCamundaSpecification()
                .body(body)
                .contentType("application/json")
                .when()
                .post("process-definition/{processDefinitionId}/start", processDefinitionId)
                .then()
                .statusCode(200)
                .extract().response().jsonPath().get("id");
    }
}
