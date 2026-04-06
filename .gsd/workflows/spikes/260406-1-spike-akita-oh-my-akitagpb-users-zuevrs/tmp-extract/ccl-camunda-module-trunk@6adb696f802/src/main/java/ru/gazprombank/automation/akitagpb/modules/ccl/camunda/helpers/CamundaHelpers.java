package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.awaitility.core.ConditionTimeoutException;
import org.awaitility.pollinterval.IterativePollInterval;
import org.awaitility.pollinterval.PollInterval;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.gazprombank.automation.akitagpb.modules.api.rest.RequestParam;
import ru.gazprombank.automation.akitagpb.modules.api.rest.RequestParamType;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.ActivityInstanceStep;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.CamundaIncident;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.ChildActivityInstance;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.ChildTransitionInstance;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.ProcessInstance;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.requests.CamundaRequests;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.exceptions.CamundaException;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.FileHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.NumberHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.XMLHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.api.rest.models.User;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.XMLHelper.getElementByString;
import static ru.gazprombank.automation.akitagpb.modules.ccl.keycloak.api.rest.requests.KeycloakRequests.getKeycloakToken;
import static ru.gazprombank.automation.akitagpb.modules.core.steps.isteps.IBaseMethods.akitaScenario;

public class CamundaHelpers {

    public static final Integer camundaActivityInstanceTimeout = ConfigLoader.getConfigValueOrDefault("camunda.activityInstance.timeout", 600);
    public static final Integer camundaPollInterval = ConfigLoader.getConfigValueOrDefault("camunda.activityInstance.pollInterval", 1);
    public static final Integer camundaPollIntervalPause = ConfigLoader.getConfigValueOrDefault("camunda.activityInstance.pause", 10);
    public static final Integer camundaSameActivityInstanceRepeatCount = ConfigLoader.getConfigValueOrDefault(
            "camunda.sameActivityInstance.repeatCount", 30);
    public static final Integer camundaSameProcessInstanceRepeatCount = ConfigLoader.getConfigValueOrDefault(
            "camunda.sameProcessInstance.repeatCount", 200);
    private static List<String> alreadyCheckedInstanceIds;

    public static List<RequestParam> addCamundaAccessToken(List<RequestParam> sourceParams) {
        List<RequestParam> paramTable = new ArrayList<>(sourceParams);
        if (paramTable.stream()
                .noneMatch(param -> param.getType().equals(RequestParamType.HEADER) && param.getName().equalsIgnoreCase("Authorization"))) {
            String accessToken = getKeycloakToken(new User("camundaRestUser"));
            paramTable.add(new RequestParam(RequestParamType.HEADER, "Authorization", "Bearer " + accessToken));
        }
        return paramTable;
    }

    /**
     * Метод парсит DefinitionId bpmn-процесса или dmn-таблицы, из полученного ответа деплоя в формате json.
     *
     * @param response ответ на запрос деплоя в Camunda.
     * @return значение первого ключа из списка deployedDecisionDefinitions.
     */
    public static String extractDefinitionId(Response response) {
        return response.path("deployedProcessDefinitions") == null ? response.path("deployedDecisionDefinitions.keySet().getAt(0)") :
                response.path("deployedProcessDefinitions.keySet().getAt(0)");
    }

    public static Map<String, Object> getDeployedDefinitionInfo(Response response) {
        return response.path("deployedProcessDefinitions") == null ?
                response.path("deployedDecisionDefinitions.'%s'", extractDefinitionId(response)) :
                response.path("deployedProcessDefinitions.'%s'", extractDefinitionId(response));
    }


    /**
     * Получаем список всех пройденных кубиков (историю инстанса) рекурсивно по ProcessInstanceId.
     * Т.е. в результирующий список будут рекурсивно добавлены и все списки кубиков для вызванных процессов, если такие есть.
     *
     * @param processInstanceId instance id проеряемого процесса.
     */
    public static List<ActivityInstanceStep> getActivityInstanceHistoryRecursive(String processInstanceId) {
        var stepList = CamundaRequests.getSortedActivityInstanceHistoryByProcessInstanceId(processInstanceId);
        var resultList = new ArrayList<>(stepList);
        for (int i = stepList.size() - 1; i >= 0; i--) {
            if (stepList.get(i).getCalledProcessInstanceId() != null) {
                resultList.addAll(i + 1, getActivityInstanceHistoryRecursive(stepList.get(i).getCalledProcessInstanceId()));
            }
        }
        return resultList;
    }

    /**
     * Метод для Cucumber-шагов ожидания перехода инстанса на указанный кубик процесса (если isSubprocess = false) или SubProcess'а (ПОДпроцесса на
     * основной схеме) любого уровня вложенности (если isSubprocess = true) Выполняет http-запрос к Camunda
     * "process-instance/{instanceId}/activity-instances",
     * пока ID активного кубика не станет равным ID ожидаемого кубика.
     *
     * @param activityIdVar  ID кубика (активности) процесса/подпроцесса, на который должен перейти инстанс.
     * @param instanceIdVar  instance Id основного процесса.
     * @param isSubprocess   флаг для обозначения "места" поиска кубика - на SubProcess'е (подпроцессе) (true) или на процессе (false)
     * @param subprocessName имя (название схемы) SubProcess'а
     * @throws CamundaException возникает при ошибках исполнения запроса в Camunda.
     */
    public static void waitActiveProcessOrSubprocessInstanceWithActivityId(String activityIdVar, String instanceIdVar, boolean isSubprocess,
                                                                           String subprocessName) {
        String instanceId = processValue(instanceIdVar);
        String activityId = processValue(activityIdVar);
        AtomicReference<List<String>> actualActivityIds = new AtomicReference<>();
        AtomicReference<List<String>> bufferActivityIds = new AtomicReference<>(List.of());
        AtomicInteger repeatCount = new AtomicInteger(0);

        //Вариант 3
        IterativePollInterval interval = new IterativePollInterval(duration -> {
            if (repeatCount.get() % 4 == 0) {
                return Duration.ofSeconds(camundaPollIntervalPause);
            } else {
                return Duration.ofSeconds(camundaPollInterval);
            }
        }, Duration.ofSeconds(camundaPollInterval));

        try {
            waitCamunda(interval).until(() -> {
                actualActivityIds.set(getActivityInstances(instanceId, isSubprocess, subprocessName, activityId));
                if (actualActivityIds.get() != null && !actualActivityIds.get().equals(bufferActivityIds.get())) {
                    akitaScenario.log("Совершен переход на ActivityIds: " + actualActivityIds.get());
                    bufferActivityIds.set(actualActivityIds.get());
                    repeatCount.set(0);
                }

                //Workaround для поддержания соединения с ReportPortal
                if (repeatCount.get() % 30 == 0) {
                    akitaScenario.log("Находимся на ActivityIds: " + actualActivityIds.get());
                }

                return actualActivityIds.get() != null && actualActivityIds.get().contains(activityId)
                        || repeatCount.incrementAndGet() == camundaSameActivityInstanceRepeatCount;
            });
            if (actualActivityIds.get() == null || !actualActivityIds.get().contains(activityId)) {
                throw new CamundaException("Ожидание было прервано, т.к. токен находился на одном и том же кубике '%s' "
                                                   + "или Камунда не отвечала в течение %d запросов", actualActivityIds.get(),
                                           camundaSameActivityInstanceRepeatCount);
            }
        } catch (ConditionTimeoutException e) {
            actualActivityIds.set(getActivityInstances(instanceId, isSubprocess, subprocessName, activityId));
            if (actualActivityIds.get() == null || !actualActivityIds.get().contains(activityId)) {
                throw new CamundaException("Общее время ожидания в %d секунд истекло. \n%s", camundaActivityInstanceTimeout, e.getMessage());
            }
        }
    }

    /**
     * Метод для ожидания активности процесса с заданным именем (или ID) и получения его processInstanceId. С помощью запроса GET
     * /process-instance?businessKey={businessKey} получается массив инстансов процессов, активных по этому businessKey. Если по ID процесса - то
     * нужный processInstance ищется по полю "definitionId" (полное совпадение этого поля с передаваемым параметром processId ИЛИ совпадение
     * processId с definitionKey из definitionId - см. {@link #getDefinitionKeyFromDefinitionId(String)}) Если по имени процесса - то по каждому id
     * инстанса запросом GET /process-instance/{id}/activity-instances определяется название (имя) процесса. Если название (ID) совпадает с указанным
     * именем (ID) процесса - метод возвращает instanceId этого процесса. Если совпадений нет (и processInstanceId == null) - значит, до данного
     * процесса выполнение ещё не дошло, и запрос GET /process-instance?businessKey={businessKey} повторяется.
     *
     * @param processName имя процесса, на схеме которого находится ожидаемый кубик.
     * @param processId   ID процесса, на схеме которого находится ожидаемый кубик - может быть равен как просто definitionKey процесса (например,
     *                    order-execution для процесса "Обработка кредитной заявки" - можно взять в Camunda Modeler'е в поле "ID" или в
     *                    Camunda Cockpit'е в поле "Definition Key"), так и полному definitionId процесса (например,
     *                    order-execution:143:9da57dff-42f6-11ed-bc41-02426003a31a для "Обработка кредитной заявки", или
     *                    9acd4dc3-4566-11ed-86d8-02426003a31a (просто UUID) для процесса "АТ: Проверка создания заявки и клиента" - значение брать по
     *                    пути jsonPath = deployedProcessDefinitions.keySet().getAt(0) ответа на POST /deployment/create)
     * @param businessKey businessKey процесса.
     * @return processInstanceId инстанса процесса с заданным именем.
     * @throws CamundaException возникает при ошибках исполнения запроса в Camunda.
     */
    public static String waitActiveProcessByNameOrIdAndGetInstanceId(String processName, String processId, String businessKey) {
        alreadyCheckedInstanceIds = new ArrayList<>();
        AtomicReference<String> processInstanceId = new AtomicReference<>();
        AtomicInteger bufferCheckedIdsSize = new AtomicInteger(0);
        AtomicInteger repeatCount = new AtomicInteger(0);
        try {
            waitCamunda().until(() -> {
                processInstanceId.set(getProcessInstanceIdFromProcessInstancesByProcessNameOrProcessId(processName, processId,
                                                                                                       CamundaRequests.getProcessInstancesByBusinessKey(
                                                                                                               businessKey)));
                if (bufferCheckedIdsSize.get() != alreadyCheckedInstanceIds.size()) {
                    bufferCheckedIdsSize.set(alreadyCheckedInstanceIds.size());
                    repeatCount.set(0);
                }

                //Workaround для поддержания соединения с ReportPortal
                if (repeatCount.get() % 30 == 0) {
                    akitaScenario.log("Ожидаем активности процесса с заданным именем (или ID)");
                }

                return processInstanceId.get() != null || repeatCount.incrementAndGet() == camundaSameProcessInstanceRepeatCount;
            });
            if (processInstanceId.get() == null) {
                throw new CamundaException(
                        "Ожидание было прервано, т.к. токен находился на одном и том же процессе или Камунда не отвечала в течение %d запросов.",
                        camundaSameProcessInstanceRepeatCount);
            }
            return processInstanceId.get();
        } catch (ConditionTimeoutException e) {
            throw new CamundaException("Общее время ожидания в %d секунд истекло. \n%s", camundaActivityInstanceTimeout, e.getMessage());
        }
    }

    /**
     * Метод для получения id инстанса процесса из переданного массива processInstances по имени процесса или по ID процесса. Если по ID процесса -
     * тонужный processInstance ищетсяпо полю "definitionId" (полное совпадение этого поля с передаваемым параметром processId ИЛИ совпадение
     * processId с definitionKey из definitionId - см. {@link #getDefinitionKeyFromDefinitionId(String)}) Если по имени процесса - то для каждого id
     * инстанса процесса метод отправляет запрос GET/process-instance/{id}/activity-instances для определения имени процесса (если по этому id запрос
     * уже отправлялся - он помещается в список alreadyCheckedInstanceIds, иповторно по нему запрос отправляться не будет (кэширование)). Если имя
     * (или ID) процесса совпадёт с требуемым именем (ID) процесса - методвернёт processInstanceId этогопроцесса. Если в массиве нет инстанса процесса
     * с требуемым именем (ID) - метод вернёт null.
     *
     * @param processName              имя процесса, на схеме которого находится ожидаемый кубик.
     * @param processId                ID процесса, на схеме которого находится ожидаемый кубик - может быть равен как просто definitionKey процесса
     *                                 (например, order-execution для процесса "Обработка кредитной заявки" - можно взять в CamundaModeler'е в поле
     *                                 "ID" или в CamundaCockpit'е в поле "Definition Key"), так и полному definitionId процесса (например,
     *                                 order-execution:143:9da57dff-42f6-11ed-bc41-02426003a31a для "Обработка кредитной заявки", или
     *                                 9acd4dc3-4566-11ed-86d8-02426003a31a (просто UUID) для процесса "АТ: Проверка создания заявки и клиента" -
     *                                 значение брать по пути jsonPath = deployedProcessDefinitions.keySet().getAt(0) ответа на POST
     *                                 /deployment/create)
     * @param processInstancesResponse ответ на запрос GET /process-instance.
     * @return processInstanceId инстанса процесса с заданным именем (ID) или null, если такого процесса не найдено.
     */
    private static String getProcessInstanceIdFromProcessInstancesByProcessNameOrProcessId(String processName, String processId,
                                                                                           Response processInstancesResponse) {
        if (processInstancesResponse.statusCode() != 200) {
            return null;
        }
        ProcessInstance[] processInstances = processInstancesResponse.as(ProcessInstance[].class);
        return Arrays.stream(processInstances).filter(instance -> {
            if (alreadyCheckedInstanceIds.contains(instance.getId())) {
                return false;
            }
            alreadyCheckedInstanceIds.add(instance.getId());
            if (processName != null) {
                return CamundaRequests.getActivityInstancesByProcessInstanceId(instance.getId()).path("name").equals(processName);
            } else {
                return getDefinitionKeyFromDefinitionId(processId) == null ?
                        processId.equalsIgnoreCase(instance.getDefinitionId()) || processId.equalsIgnoreCase(
                                getDefinitionKeyFromDefinitionId(instance.getDefinitionId())) :
                        processId.equalsIgnoreCase(instance.getDefinitionId());
            }
        }).findFirst().orElseGet(ProcessInstance::new).getId();
    }

    /**
     * Поле definitionId (из запроса GET /process-instance) инстанса процесса обычно имеет формат definitionKey:definitionVersion:deploymentId
     * (например, order-execution:143:9da57dff-42f6-11ed-bc41-02426003a31a для инстанса процесса "Обработка кредитной заявки"). Но, если definitionKey
     * процесса слишком длинный (по мнению Камунды), формат поля definitionId будет просто UUID. Если первый формат - метод вернёт из переданного
     * definitionId definitionKey (т.е. order-execution для "Обработки кредитной заявки" из примера выше). Если второй формат - метод вернёт null.
     *
     * @param definitionId definitionId инстанса процесса.
     * @return definitionKey инстанса процесса или null, если формат definitionId - UUID.
     */
    private static String getDefinitionKeyFromDefinitionId(String definitionId) {
        Pattern pattern = Pattern.compile("(.+):\\d+:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
        Matcher matcher = pattern.matcher(definitionId);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Получаем ID текущих активных кубиков на процессе Camunda (если isSubprocess = false), или на SubProcess'е (ПОДпроцессе на основной схеме)
     * любого уровня вложенности (если isSubprocess = true).
     *
     * @param instanceId     id инстанса процесса.
     * @param isSubprocess   флаг для обозначения "места" поиска кубика - на подпроцессе (true) или на процессе (false)
     * @param subprocessName имя (название схемы) SubProcess'а
     * @param activityId     id кубика - чтобы Exception кидался при наличии инцидентов и активном кубике, не равном ожидаемому
     * @return ID активных кубиков или null, если не получили ответ HTTP 200 от Камунды.
     */
    private static List<String> getActivityInstances(String instanceId, boolean isSubprocess, String subprocessName, String activityId) {
        Response response = CamundaRequests.getActivityInstancesByProcessInstanceId(instanceId);
        if (response.statusCode() != 200) {
            return null;
        }

        var incidents = new ArrayList<>();
        var activityInstancesIds = response.jsonPath().getList("childActivityInstances", ChildActivityInstance.class).stream()
                .filter(e -> isSubprocess == e.getActivityType().equals("subProcess") && (subprocessName == null || subprocessName.equals(
                        e.getName()))).flatMap(e -> {
                    if (isSubprocess) {
                        //получаем последние дочерние кубики с подпроцесса (их может быть несколько на одном подпроцессе, токен распараллеливается), на которых находятся токены
                        List<ChildActivityInstance> result = getAllLastChildActivityInstances(e);
                        return !result.isEmpty() ? result.stream() : Stream.of(e);
                    }
                    return Stream.of(e);
                }).peek(e -> {
                    if (!e.getIncidents().isEmpty()) {
                        incidents.addAll(e.getIncidents());
                    } else if (!e.getChildTransitionInstances().isEmpty() && !e.getChildTransitionInstances().get(0).getIncidents().isEmpty()) {
                        incidents.addAll(e.getChildTransitionInstances().get(0).getIncidents());
                    }
                }).map(ChildActivityInstance::getActivityId).collect(Collectors.toList());

        var transitionInstances = response.jsonPath().getList("childTransitionInstances", ChildTransitionInstance.class);
        if (!transitionInstances.isEmpty() && !transitionInstances.get(0).getIncidents().isEmpty()) {
            incidents.addAll(transitionInstances.get(0).getIncidents());
        }
        if (!incidents.isEmpty() && !activityInstancesIds.contains(activityId)) {
            throw new RuntimeException("На процессе найдены инциденты:\n" + incidents);
        }
        return activityInstancesIds;
    }

    /**
     * Получить список всех объектов ChildActivityInstance, находящихся на последнем уровне вложенности каждого из этого объекта.
     * Выполняется рекурсивно.
     *
     * @param root родительский ChildActivityInstance
     * @return список всех объектов ChildActivityInstance, находящихся на последнем уровне вложенности каждого из этого объекта
     */
    private static List<ChildActivityInstance> getAllLastChildActivityInstances(ChildActivityInstance root) {
        List<ChildActivityInstance> result = new ArrayList<>();
        var rootChilds = root.getChildActivityInstances();
        if (rootChilds == null || rootChilds.isEmpty()) {
            return result;
        }
        rootChilds.forEach(e -> {
            if (e.getChildActivityInstances() == null || e.getChildActivityInstances().isEmpty()) {
                result.add(e);
            } else {
                result.addAll(getAllLastChildActivityInstances(e));
            }
        });
        return result;
    }

    public static ConditionFactory waitCamunda() {
        return Awaitility
                .await()
                .pollInSameThread()
                .timeout(camundaActivityInstanceTimeout, TimeUnit.SECONDS)
                .pollInterval(camundaPollInterval, TimeUnit.SECONDS);
    }

    public static ConditionFactory waitCamunda(PollInterval interval) {
        return Awaitility
                .await()
                .pollInSameThread()
                .timeout(camundaActivityInstanceTimeout, TimeUnit.SECONDS)
                .pollInterval(interval)
                ;
    }

    /**
     * Вспомогательный метод для проверки и сохранение переменной из шага в колонке "Переменная для *"
     *
     * @param parametersFromStep     параметры из шага
     * @param deployedDefinitionInfo мапа с переменными из ответа от camunda
     */
    public static void saveVarFromCamundaResponse(Map<String, String> parametersFromStep, Map<String, Object> deployedDefinitionInfo) {
        parametersFromStep.entrySet().stream().filter(entry -> entry.getKey().startsWith("Переменная для ")).forEach(entry -> {
            String varName = entry.getKey().substring(15);
            assertThat(deployedDefinitionInfo.containsKey(varName)).as("Ответ от Camunda не содержит поля %s в разделе Deployed.", varName).isTrue();
            akitaScenario.setVar(entry.getValue(), deployedDefinitionInfo.get(varName));
            akitaScenario.log("В переменные сценария сохраняем " + entry.getValue() + " = " + deployedDefinitionInfo.get(varName));
        });
    }

    /**
     * Метод позволяет получить список переменных из истории и сохранить их как Map.<br>
     * Парсинг Json идёт через ObjectMapper для того, чтобы получать чилса в том виде, в котором они указаны в Camunda, а не в Java формате.
     *
     * @param processInstanceId instanceId процесса с переменными
     * @return {@link Map}, где ключ - это имя переменной ({@link String}), а значение - значение переменной ({@link String})
     */
    public static Map<String, String> getVariableHistory(String processInstanceId) {
        Response response = CamundaRequests.variableInstanceHistory(processInstanceId);
        Map<String, String> resultVariables = new HashMap<>();

        List<HashMap<Object, Object>> responseVariables = response.jsonPath(new JsonPathConfig(NumberReturnType.DOUBLE)).getList("$");

        for (HashMap<Object, Object> object : responseVariables) {
            String name = String.valueOf(object.get("name"));
            Object rawValue = object.get("value");
            try {
                if (rawValue == null) {
                    resultVariables.put(name, "null");
                } else {
                    //получаем именно то, что написано в JSON
                    String value = StringHelper.getStringValue(rawValue);
                    //если тип Double, то проверяем, то обрабатываем записб видо 0.00Е3 в обычный вид
                    if (object.get("type").toString().equals("Double")) {
                        value = NumberHelper.parseDouble(value);
                    }
                    resultVariables.put(name, value);
                }
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(String.format("Не удалось преобразовать значение %s в текст!", rawValue));
            }
        }

        return resultVariables;
    }

    /**
     * Вспомогательный метод для замены в тестовой схеме пустого кубика вызова подпроцесса без эскалации на указанный с процесса в Bitbucket
     *
     * @param bpmnFilePath          путь до bpmn-схемы, с которой берём кубик вызова подпроцесса для проверки
     * @param id                    идентификатор кубика вызова подпроцесса, который берём для проверки
     * @param xpathCallActivityTest xpath кубика с подпроцессом на схемах
     */
    public static Document prepareBpmnChangeTestFileWithCallActivity(String bpmnFilePath, String id, String xpathCallActivityTest) {
        String bpmnFilePathTest = "${camunda.files}/testing/deployCamundaTestFileWithCallActivity.bpmn";

        return bpmnChangeTestFileWithCallActivityMain(bpmnFilePath, bpmnFilePathTest, id, xpathCallActivityTest);
    }

    /**
     * Вспомогательный метод для изменения в bpmn-схеме топика на moderate у сервисной таски
     *
     * @param document документ bpmn-схемы
     * @param table    таблица с id кубиков в схеме, пример
     *                 | Id кубика        |
     *                 | Activity_13stxk3 |
     *                 | Activity_0mhgjom |
     */
    public static String prepareCamundaChangeToModerate(String typeTask, List<Map<String, String>> table, Document document) {
        for (Map<String, String> line : table) {
            String id = resolveVariables(line.get("Id кубика"));
            if (typeTask.equals("сервисную таску")) {
                prepareBpmnChangeServiceTask(document, id);
            } else if (typeTask.equals("кубики вызова подпроцессов")) {
                prepareBpmnChangeCallActivityTask(document, id);
            }
        }

        addFlagAutotest(document);
        return XMLHelper.getContentFromDocumentXml(document);
    }

    /**
     * Вспомогательный метод для изменения в bpmn-схеме топика на moderate у сервисной таски
     *
     * @param document документ bpmn-схемы
     * @param id       кубика, на котором менятся топик
     */
    public static void prepareBpmnChangeServiceTask(Document document, String id) {
        String taskXpath = String.format("//*[local-name()='serviceTask'][@id='%s']", id);
        Node taskElement = XMLHelper.findNodeByXpath(document, taskXpath);
        ((Element) taskElement).setAttribute("camunda:topic", "moderate");
        Node extensionElements = XMLHelper.findNodeByXpath(document, taskXpath + "/extensionElements");
        taskElement.removeChild(extensionElements);
    }

    /**
     * Вспомогательный метод для преобразования тегов и вставки скрипта в bpmn-файл
     *
     * @param document          Document по bpmn схеме
     * @param insertXmlFilePath путь до файла скрипта
     * @param id                кубика
     */
    @SneakyThrows
    public static void addEndScriptInBpmn(Document document, String insertXmlFilePath, String id, String orderNumber) {
        String groovyScript = FileHelper.getFileContent(new File(resolveVariables(insertXmlFilePath)));
        String insertText = String.format(
                "<camunda:executionListener event=\"end\"><camunda:script scriptFormat=\"groovy\">%s</camunda:script></camunda:executionListener>",
                groovyScript);

        Element insertElement = getElementByString(insertText);

        String taskXpath = String.format("//*[local-name()='serviceTask'][@id='%s']", id);
        String extensionElementsXpath = String.format("%s/*[local-name()='extensionElements']", taskXpath);
        Node extensionElementsNode = XMLHelper.findNodeByXpath(document, extensionElementsXpath);

        if (extensionElementsNode == null) {
            Node node = XMLHelper.findNodeByXpath(document, taskXpath);
            Node importNode = document.importNode(getElementByString(
                    "<bpmn:extensionElements><camunda:properties><camunda:property/></camunda:properties></bpmn:extensionElements>"), false);
            node.insertBefore(importNode, node.getFirstChild());
        }

        // Если не задан номер позиции для вставки скрипта, то вставляем в конец тега, иначе вставляем в определённую позицию
        if (orderNumber == null) {
            Node node = XMLHelper.findNodeByXpath(document, extensionElementsXpath);
            Node importNode = document.importNode(insertElement, true);
            node.appendChild(importNode);
        } else {
            int order = Integer.parseInt(orderNumber);
            assertThat(order < 1).withFailMessage("'Позиция endScript' не может быть меньше 1").isFalse();

            String xpath = String.format("%s//*[local-name()='executionListener']", taskXpath);
            NodeList nodeList = XMLHelper.findNodeListByXpath(document, xpath);

            // т.к. нумерация начинается с 1, то у индекса вычитаем 1
            order = order - 1;
            Node importNode = document.importNode(insertElement, true);

            //Если задали позицию больше чем скриптов в bpmn, то вставляем последним, иначе вставляем в нужную позицию
            if (order >= nodeList.getLength()) {
                nodeList.item(0).getParentNode().appendChild(importNode);
            } else {
                Node node = nodeList.item(order);
                node.getParentNode().insertBefore(importNode, node);
            }
        }
    }

    /**
     * Вспомогательный метод - изменяем атрибуты в bpmn (добавляем постфикс "_isAutotest"), чтобы не мешало ручному тестированию
     *
     * @param document по bpmn схеме
     */
    public static void addFlagAutotest(Document document) {
        //Изменяем атрибуты в bpmn (добавляем постфикс "_isAutotest"), чтобы не мешало ручному тестированию
        Element processNode = (Element) XMLHelper.findNodeByXpath(document, "//*[local-name()='process']");
        String idProcess = processNode.getAttribute("id");
        //Если флаг уже выставлен, то пропускаем
        if (idProcess.endsWith("_isAutotest")) {
            return;
        }
        processNode.setAttribute("id", idProcess + "_isAutotest");
        processNode.setAttribute("name", processNode.getAttribute("name") + " (isAutotest)");

        Element diagramNode = (Element) XMLHelper.findNodeByXpath(document,
                                                                  String.format("//*[local-name()='BPMNPlane'][@bpmnElement='%s']", idProcess));
        diagramNode.setAttribute("bpmnElement", idProcess + "_isAutotest");
    }

    /**
     * Вспомогательный метод для преобразования тегов и изменения типа топика на moderate
     *
     * @param xmlFilePath путь до файла bpmn
     * @param id          кубика
     * @return контент изменённого файла bpmn
     */
    public static String getPrepareBpmnChangeToModerate(String xmlFilePath, String id) {
        Document document = XMLHelper.parseXml(resolveVariables(xmlFilePath));

        String taskXpath = String.format("//*[local-name()='serviceTask'][@id='%s']", id);
        Element taskElement = (Element) XMLHelper.findNodeByXpath(document, taskXpath);
        taskElement.setAttribute("camunda:topic", "moderate");

        //Изменяем атрибуты в bpmn (добавляем постфикс "_isAutotest"), чтобы не мешало ручному тестированию
        Element processNode = (Element) XMLHelper.findNodeByXpath(document, "//*[local-name()='process']");
        String idProcess = processNode.getAttribute("id");
        processNode.setAttribute("id", idProcess + "_isAutotest");
        processNode.setAttribute("name", processNode.getAttribute("name") + " (isAutotest)");

        Element diagramNode = (Element) XMLHelper.findNodeByXpath(document,
                                                                  String.format("//*[local-name()='BPMNPlane'][@bpmnElement='%s']", idProcess));
        diagramNode.setAttribute("bpmnElement", idProcess + "_isAutotest");

        return XMLHelper.getContentFromDocumentXml(document);
    }

    /**
     * Вспомогательный метод для изменения в bpmn-схеме callActivity таски
     *
     * @param document документ bpmn-схемы
     * @param id       кубика, который менятся
     */
    public static void prepareBpmnChangeCallActivityTask(Document document, String id) {
        String taskXpath = String.format("//*[local-name()='callActivity'][@id='%s']", id);
        Node taskElement = XMLHelper.findNodeByXpath(document, taskXpath);
        ((Element) taskElement).setAttribute("camunda:topic", "moderate");
        ((Element) taskElement).setAttribute("camunda:type", "external");
        Node extensionElements = XMLHelper.findNodeByXpath(document, taskXpath + "/extensionElements");
        taskElement.removeChild(extensionElements);
        ((Element) taskElement).removeAttribute("calledElement");
        document.renameNode(taskElement, "", "bpmn:serviceTask");

        String boundaryEventXpath = String.format("//*[local-name()='boundaryEvent'][@attachedToRef='%s']", id);
        NodeList boundaryEventElementList = XMLHelper.findNodeListByXpath(document, boundaryEventXpath);
        for (int j = 0; j < boundaryEventElementList.getLength(); j++) {
            String idBoundary = ((Element) boundaryEventElementList.item(j)).getAttribute("id");
            String idOutgoing = XMLHelper.findNodeByXpath(document, boundaryEventXpath + "/*[local-name()='outgoing']").getFirstChild()
                    .getNodeValue();
            NodeList removeListElement = XMLHelper.findNodeListByXpath(document,
                                                                       String.format("//*[text() = '%s' or @* = '%s' or @* = '%s']", idOutgoing,
                                                                                     idOutgoing, idBoundary));
            for (int i = 0; i < removeListElement.getLength(); i++) {
                removeListElement.item(i).getParentNode().removeChild(removeListElement.item(i));
            }
        }
    }

    /**
     * Вспомогательный метод для замены в тестовой схеме пустого кубика вызова подпроцесса с эскалацией на указанный с процесса в Bitbucket
     *
     * @param bpmnFilePath          путь до bpmn-схемы, с которой берём кубик вызова подпроцесса для проверки
     * @param id                    идентификатор кубика вызова подпроцесса, который берём для проверки
     * @param escalation            идентификатор эскалации с кубика вызова подпроцесса
     * @param xpathCallActivityTest xpath кубика с подпроцессом на схемах
     */
    public static Document prepareBpmnChangeTestFileWithCallActivityEscalation(String bpmnFilePath, String id, String escalation,
                                                                               String xpathCallActivityTest) {
        String bpmnFilePathTest = "${camunda.files}/testing/deployCamundaTestFileWithCallActivityEscalation.bpmn";
        String escalationXpathTest = "//*[local-name()='escalation'][@id='EscalationCheck']";

        Document testDocument = bpmnChangeTestFileWithCallActivityMain(bpmnFilePath, bpmnFilePathTest, id, xpathCallActivityTest);

        Document document = XMLHelper.parseXml(resolveVariables(bpmnFilePath));
        String xpath = String.format("//*[local-name()='boundaryEvent'][@id='%s']/*[local-name()='escalationEventDefinition']", escalation);
        Element escalationEventNode = (Element) XMLHelper.findNodeByXpath(document, xpath);
        String escalationRef = escalationEventNode.getAttribute("escalationRef");

        String escalationXpath = String.format("//*[local-name()='escalation'][@id='%s']", escalationRef);
        Element escalationNode = (Element) XMLHelper.findNodeByXpath(document, escalationXpath);
        String escalationName = escalationNode.getAttribute("name");
        String escalationCode = escalationNode.getAttribute("escalationCode");

        Element escalationNodeTest = (Element) XMLHelper.findNodeByXpath(testDocument, escalationXpathTest);
        escalationNodeTest.setAttribute("name", escalationName);
        escalationNodeTest.setAttribute("escalationCode", escalationCode);

        return testDocument;
    }

    /**
     * Основной метод замены в тестовом методе пустого кубика вызова подпроцесса с и без эскалации на указанный с процесса в Bitbucket
     *
     * @param bpmnFilePath          путь до bpmn-схемы, с которой берём кубик вызова подпроцесса для проверки
     * @param bpmnFilePathTest      путь до тестовой bpmn-схемы, на которой устанавливаем кубик вызова подпроцесса для проверки
     * @param id                    идентификатор эскалации с кубика вызова подпроцесса
     * @param xpathCallActivityTest xpath кубика с подпроцессом на схемах
     */
    public static Document bpmnChangeTestFileWithCallActivityMain(String bpmnFilePath, String bpmnFilePathTest, String id,
                                                                  String xpathCallActivityTest) {

        Document document = XMLHelper.parseXml(resolveVariables(bpmnFilePath));
        String xpath = String.format("//*[local-name()='callActivity'][@id='%s']", id);
        Element callActivityNode = (Element) XMLHelper.findNodeByXpath(document, xpath);
        String callActivityName = callActivityNode.getAttribute("name");
        String calledElement = callActivityNode.getAttribute("calledElement");
        String asyncBefore = callActivityNode.getAttribute("camunda:asyncBefore");
        Node extensionElements = callActivityNode.getElementsByTagName("bpmn:extensionElements").item(0);

        Document testDocument = XMLHelper.parseXml(resolveVariables(bpmnFilePathTest));
        Element callActivityNodeTest = (Element) XMLHelper.findNodeByXpath(testDocument, xpathCallActivityTest);
        callActivityNodeTest.setAttribute("name", callActivityName + " (isAutotest)");
        callActivityNodeTest.setAttribute("calledElement", calledElement + "_isAutotest");
        if (!asyncBefore.isEmpty()) {
            callActivityNodeTest.setAttribute("camunda:asyncBefore", asyncBefore);
        }
        Node importElement = testDocument.importNode(extensionElements, true);
        callActivityNodeTest.insertBefore(importElement, callActivityNodeTest.getFirstChild());

        Element processNode = (Element) XMLHelper.findNodeByXpath(testDocument, "//*[local-name()='process']");
        processNode.setAttribute("name", processNode.getAttribute("name") + " (" + calledElement + ")");

        return testDocument;
    }

    /**
     * Вспомогательный метод для замены в тестовом методе пустой сервисной таски на указанную сервисную таску с процесса в Bitbucket
     *
     * @param bpmnFilePath путь до bpmn-схемы, с которой берём сервисный кубик для проверки
     * @param id           идентификатор сервисного кубика, который берём для проверки
     */
    public static String prepareBpmnChangeTestFileWithServiceTask(String bpmnFilePath, String id) {
        String bpmnFilePathTest = "${camunda.files}/testing/deployCamundaTestFileWithServiceTask.bpmn";
        String xpathTest = "//*[local-name()='serviceTask'][@id='checkActivity']";
        String xpathDocument = "//*[local-name()='serviceTask'][@id='%s']";
        return prepareBpmnChangeTest(bpmnFilePath, id, bpmnFilePathTest, xpathTest, xpathDocument);
    }

    /**
     * Вспомогательный метод для замены в тестовом методе пустой сервисной таски на указанную сервисную таску с процесса в Bitbucket
     *
     * @param bpmnFilePath путь до bpmn-схемы, с которой берём сервисный кубик для проверки
     * @param id           идентификатор сервисного кубика, который берём для проверки
     */
    public static String prepareBpmnChangeTestFileWithSendTask(String bpmnFilePath, String id) {
        String bpmnFilePathTest = "${camunda.files}/testing/deployCamundaTestFileWithSendTask.bpmn";
        String xpathTest = "//*[local-name()='sendTask'][@id='checkActivity']";
        String xpathDocument = "//*[local-name()='sendTask'][@id='%s']";
        return prepareBpmnChangeTest(bpmnFilePath, id, bpmnFilePathTest, xpathTest, xpathDocument);
    }

    /**
     * Вспомогательный метод для замены в тестовом методе пустого intermediateCatchEvent на указанный intermediateCatchEvent с процесса в Bitbucket
     *
     * @param bpmnFilePath путь до bpmn-схемы, с которой берём сервисный кубик для проверки
     * @param id           идентификатор сервисного кубика, который берём для проверки
     */
    public static String prepareBpmnChangeTestFileWithIntermediateCatchEvent(String bpmnFilePath, String id) {
        String bpmnFilePathTest = "${camunda.files}/testing/deployCamundaTestFileWithIntermediateCatchEvent.bpmn";
        String xpathTest = "//*[local-name()='intermediateCatchEvent'][@id='checkActivity']";

        Document document = XMLHelper.parseXml(resolveVariables(bpmnFilePath));
        Document testDocument = XMLHelper.parseXml(resolveVariables(bpmnFilePathTest));
        Element processNode = (Element) XMLHelper.findNodeByXpath(testDocument, "//*[local-name()='process']");

        String xpath = String.format("//*[local-name()='intermediateCatchEvent'][@id='%s']", id);
        Element userTaskNode = (Element) XMLHelper.findNodeByXpath(document, xpath);

        String eventName = userTaskNode.getAttribute("name");
        String asyncBefore = userTaskNode.getAttribute("camunda:asyncBefore");

        Node extensionElements = userTaskNode.getElementsByTagName("bpmn:extensionElements").item(0);
        Node message = userTaskNode.getElementsByTagName("bpmn:messageEventDefinition").item(0);

        Element catchEventNodeTest = (Element) XMLHelper.findNodeByXpath(testDocument, xpathTest);
        catchEventNodeTest.setAttribute("name", eventName);
        catchEventNodeTest.setAttribute("camunda:asyncBefore", asyncBefore);

        if (extensionElements != null) {
            Node importElement = testDocument.importNode(extensionElements, true);
            catchEventNodeTest.insertBefore(importElement, catchEventNodeTest.getFirstChild());
        }

        if (message != null) {
            String messageRef = ((Element)message).getAttribute("messageRef");
            String messageXpath = String.format("//*[local-name()='message'][@id='%s']", messageRef);
            Element messageNode = (Element) XMLHelper.findNodeByXpath(document, messageXpath);

            Node importElement = testDocument.importNode(messageNode, true);
            testDocument.getDocumentElement().insertBefore(importElement, XMLHelper.findNodeByXpath(testDocument, "//*[local-name()='BPMNDiagram']"));

            importElement = testDocument.importNode(message, true);
            catchEventNodeTest.appendChild(importElement);
        }

        processNode.setAttribute("name", processNode.getAttribute("name") + " (" + eventName + ")");

        String bpmnContent = XMLHelper.getContentFromDocumentXml(testDocument);
        akitaScenario.log(bpmnContent);

        return bpmnContent;
    }

    /**
     * Общий метод для подстановки данных из реальногог BPMN в тестовый для кубиков тапа Service task и Send task.
     *
     * @param bpmnFilePath     путь до BPMN файла
     * @param id               id кубика, который надо подставить.
     * @param bpmnFilePathTest путь до тестовго BPMN файла.
     * @param xpathTest        XPath до тестового кубика, куда будем подставлять данные.
     * @param xpathDocument    XPath до текущего кубика, откуда будем брать данные.
     */
    private static String prepareBpmnChangeTest(String bpmnFilePath, String id, String bpmnFilePathTest, String xpathTest, String xpathDocument) {
        String xpathMethodInInput = xpathDocument + "//*[local-name()='inputParameter'][@name='method']";
        String xpathMethodInList = xpathMethodInInput + "//*[local-name()='value']";

        Document document = XMLHelper.parseXml(resolveVariables(bpmnFilePath));
        xpathDocument = String.format(xpathDocument, id);
        Element serviceTaskNode = (Element) XMLHelper.findNodeByXpath(document, xpathDocument);
        String taskName = serviceTaskNode.getAttribute("name");
        String camundaTopic = serviceTaskNode.getAttribute("camunda:topic");
        Node extensionElements = serviceTaskNode.getElementsByTagName("bpmn:extensionElements").item(0);

        Element methodNodeInput = (Element) XMLHelper.findNodeByXpath(document, String.format(xpathMethodInInput, id));
        Element methodNodeList = (Element) XMLHelper.findNodeByXpath(document, String.format(xpathMethodInList, id));
        String methodName = (methodNodeList == null)
                ? methodNodeInput.getFirstChild().getNodeValue() : methodNodeList.getFirstChild().getNodeValue();

        Document testDocument = XMLHelper.parseXml(resolveVariables(bpmnFilePathTest));
        Element serviceTaskNodeTest = (Element) XMLHelper.findNodeByXpath(testDocument, xpathTest);
        serviceTaskNodeTest.setAttribute("name", taskName);
        serviceTaskNodeTest.setAttribute("camunda:type", "external");
        serviceTaskNodeTest.setAttribute("camunda:topic", camundaTopic);
        Node importElement = testDocument.importNode(extensionElements, true);
        serviceTaskNodeTest.insertBefore(importElement, serviceTaskNodeTest.getFirstChild());

        Element processNode = (Element) XMLHelper.findNodeByXpath(testDocument, "//*[local-name()='process']");
        processNode.setAttribute("name", processNode.getAttribute("name") + " (" + methodName + ")");

        String bpmnContent = XMLHelper.getContentFromDocumentXml(testDocument);
        akitaScenario.log(bpmnContent);

        return bpmnContent;
    }

    /**
     * Вспомогательный метод для замены в тестовом методе пустой пользовательской таски на указанную пользовательскую таску с процесса в Bitbucket
     *
     * @param bpmnFilePath путь до bpmn-схемы, с которой берём пользовательский кубик для проверки
     * @param id           идентификатор пользовательского кубика, который берём для проверки
     */
    public static String prepareBpmnChangeTestFileWithUserTask(String bpmnFilePath, String id) {
        String bpmnFilePathTest = "${camunda.files}/testing/deployCamundaTestFileWithUserTask.bpmn";
        String xpathTest = "//*[local-name()='userTask'][@id='checkActivity']";

        Document document = XMLHelper.parseXml(resolveVariables(bpmnFilePath));
        String xpath = String.format("//*[local-name()='userTask'][@id='%s']", id);
        Element userTaskNode = (Element) XMLHelper.findNodeByXpath(document, xpath);
        String taskName = userTaskNode.getAttribute("name");
        String camundaFormKey = userTaskNode.getAttribute("camunda:formKey");
        String camundaAssignee = userTaskNode.getAttribute("camunda:assignee");
        String camundaCandidateGroups = userTaskNode.getAttribute("camunda:candidateGroups");
        Node extensionElements = userTaskNode.getElementsByTagName("bpmn:extensionElements").item(0);
        Node documentation = userTaskNode.getElementsByTagName("bpmn:documentation").item(0);

        Document testDocument = XMLHelper.parseXml(resolveVariables(bpmnFilePathTest));
        Element userTaskNodeTest = (Element) XMLHelper.findNodeByXpath(testDocument, xpathTest);
        userTaskNodeTest.setAttribute("name", taskName);
        userTaskNodeTest.setAttribute("camunda:formKey", camundaFormKey);
        userTaskNodeTest.setAttribute("camunda:candidateGroups", camundaCandidateGroups);
        if (!camundaAssignee.isEmpty()) {
            userTaskNodeTest.setAttribute("camunda:assignee", camundaAssignee);
        }
        if (extensionElements != null) {
            Node importElement = testDocument.importNode(extensionElements, true);
            userTaskNodeTest.insertBefore(importElement, userTaskNodeTest.getFirstChild());
        }
        if (documentation != null) {
            Node importElement = testDocument.importNode(documentation, true);
            userTaskNodeTest.insertBefore(importElement, userTaskNodeTest.getFirstChild());
        }

        Element processNode = (Element) XMLHelper.findNodeByXpath(testDocument, "//*[local-name()='process']");
        processNode.setAttribute("name", processNode.getAttribute("name") + " (" + camundaFormKey + ")");

        String bpmnContent = XMLHelper.getContentFromDocumentXml(testDocument);
        akitaScenario.log(bpmnContent);

        return bpmnContent;
    }

    /**
     * Вспомогательный метод, который получает bpmn-схему из хранилища переменных или по определённому пути
     *
     * @param bpmnFilePath путь до bpmn-схемы или переменная содержащая схему
     * @return Document
     */
    public static Document getDocumentFromVarNameOrPath(String bpmnFilePath) {
        Document document;
        String bpmnContent;
        if (akitaScenario.tryGetVar(bpmnFilePath) != null) {
            bpmnContent = (String) akitaScenario.getVar(bpmnFilePath);
            ByteArrayInputStream bpmnInputStream = new ByteArrayInputStream(bpmnContent.getBytes());
            document = XMLHelper.parseXml(bpmnInputStream);
        } else {
            document = XMLHelper.parseXml(resolveVariables(bpmnFilePath));
        }
        return document;
    }

    /**
     * Получить externalTaskId инцидента на процессе
     * - если configuration != null, то externalTaskId = configuration
     * - если в первом запросе configuration == null, отправляем запросы GET incident/{id}, чтобы получить инцидент-причину,
     * имеющий configuration != null
     *
     * @param incident объект инцидента на процессе
     * @return externalTaskId
     */
    public static String getExternalTaskIdFromIncident(CamundaIncident incident) {
        while (incident.getConfiguration() == null && incident.getCauseIncidentId() != null) {
            incident = CamundaRequests.getIncidentById(incident.getCauseIncidentId());
        }
        return Objects.requireNonNull(incident.getConfiguration(), "ExternalTaskId инцидента не найден.");
    }
}
