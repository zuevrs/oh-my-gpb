package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.steps;

import io.cucumber.java.ru.И;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.ActivityInstanceStep;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.requests.CamundaRequests;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.dataTableModels.CamundaActivityWaitingParam;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.dataTableModels.CamundaVariableUpdatingParam;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.exceptions.CamundaException;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.FileHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.XMLHelper;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.camundaActivityInstanceTimeout;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.getActivityInstanceHistoryRecursive;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.waitActiveProcessByNameOrIdAndGetInstanceId;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.waitActiveProcessOrSubprocessInstanceWithActivityId;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.waitCamunda;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

/**
 * Класс содержащий Cucumber шаги для операций с Camunda клиентом.
 */
public class CamundaSteps extends BaseMethods {

    public static final String SAVE_PROCESS_INSTANCE_TAG = "@saveProcessInstance";
    public static final String DELETE_DEPLOYMENT_CASCADE_TAG = "@deleteDeploymentCascade";

    /**
     * Шаг Cucumber для ожидания перехода инстанса на указанный кубик.
     *
     * @param activityId ID кубика (активности), на который должен перейти инстанс.
     * @param instanceId instance Id процесса.
     * @throws CamundaException возникает при ошибках исполнения запроса в Camunda.
     */
    @И("^ждём перехода на кубик \"(.+)\" процесса с instanceId равным \"(.+)\"$")
    public void waitActiveInstance(String activityId, String instanceId) {
        waitActiveProcessOrSubprocessInstanceWithActivityId(activityId, instanceId, false, null);
    }

    /**
     * Шаг Cucumber, аналогичный шагу {@link #waitActiveInstance(String, String)}, но для ожидания перехода инстанса на указанный кубик SubProcess'а -
     * т.е. ПОДпроцесса, находящегося на схеме основного процесса (на любом уровне вложенности).
     *
     * @param activityId     ID кубика (активности) SubProcess'а (подпроцесса), на который должен перейти инстанс.
     * @param subprocessName имя схемы SubProcess'а (опциональный параметр, нужен, если на схеме несколько подпроцессов)
     * @param instanceId     instance Id основного процесса.
     * @throws CamundaException возникает при ошибках исполнения запроса в Camunda.
     */
    @И("^ждём перехода на кубик \"([^\"]+)\" SubProcess'а( \"(.+)\")? со схемы основного процесса с instanceId равным \"([^\"]+)\"$")
    public void waitActiveSubprocessInstance(String activityId, String subprocessName, String instanceId) {
        waitActiveProcessOrSubprocessInstanceWithActivityId(activityId, instanceId, true, subprocessName);
    }

    /**
     * Шаг Cucumber для ожидания перехода инстанса на указанный кубик процесса или SubProcess'а (подпроцесса) по имени или ID процесса и
     * его businessKey (и для сохранения instanceId этого процесса в заданную переменную). При этом не имеет значения, на схеме какого процесса
     * находится ожидаемый кубик - основного или вызванного (и на каком уровне вызова, т.е. процесс с нужным кубиком может быть вызван из вызванного
     * процесса и т.д.).
     * Необходмо указать (через таблицу параметров шага):
     * - ID ожидаемого кубика(параметр 'ID кубика');
     * - ИМЯ (название) или ID процесса, на схеме которого находится этот кубик (один из параметров - 'Имя процесса' или 'ID процесса'. Если будут
     * заданы оба вместе - поиск будет по имени процесса, параметр 'ID процесса' будет игнорирован);
     * - находится ли кубик на схеме этого процесса или на его SubProcess'е (отдельной подсхеме этой же .bpmn-схемы) (параметр 'Кубик SubProcess'а со
     * схемы процесса?', НЕобязательный, принимает значения "да", "нет", "true" и"false" (без кавычек, в любомрегистре).) Если кубик на процессе -
     * параметр можно не указывать (удалить из таблицы));
     * - имя (название схемы) SubProcess'а - НЕобязательный, указывается для уточнения SubProcess'а, если на процессе их запущено несколько;
     * - businessKey процесса (он сквозной и для основного, и для вызванных процессов)(параметр 'businessKey').
     * <p>
     * Также шаг включает в себя функционал шагов {@link #waitActiveInstance(String, String)} и {@link #waitActiveSubprocessInstance(String, String,
     * String)}, но ожидание кубика идёт не по instanceId, а по названию процесса и его businessKey
     * <p>
     * Принцип работы: с помощью запроса GET /process-instance?businessKey={businessKey} получается список id инстансов процессов, активных по
     * этому businessKey. По каждому id запросом GET /process-instance/{id}/activity-instances определяется название (имя) процесса. Если название
     * совпадает с указанным именемпроцесса - instanceId этого процессасохраняется. Если ни для одного id название не совпало - значит, до данного
     * процесса выполнение ещё не дошло, и запрос GET/process-instance?businessKey={businessKey}повторяется. Далее, после получения instanceId
     * нужного
     * процесса, логика аналогична шагам {@link #waitActiveInstance(String, String)} и {@link #waitActiveSubprocessInstance(String, String, String)}.
     *
     * @param instanceIdVar переменная для сохранения в неё instanceId процесса, на котором находится искомый кубик.
     * @param param         объект класса {@link CamundaActivityWaitingParam}, представление кукумбер-таблицы параметров.
     *                      Возможные параметры таблицы и значения описаны в {@link CamundaActivityWaitingParam.TableParameters}
     * @throws CamundaException возникает при ошибках исполнения запроса в Camunda.
     */
    @И("^ждём перехода на кубик процесса с параметрами из таблицы, сохранив instanceId этого процесса в переменную \"([^\"]+)\"$")
    public void waitActiveProcessOrSubprocessInstanceByProcessNameAndBusinessKey(String instanceIdVar, CamundaActivityWaitingParam param) {
        String processInstanceId = waitActiveProcessByNameOrIdAndGetInstanceId(param.getProcessName(), param.getProcessId(), param.getBusinessKey());
        waitActiveProcessOrSubprocessInstanceWithActivityId(param.getActivityId(), processInstanceId, param.isSubprocess(),
                                                            param.getSubprocessName());
        akitaScenario.setVar(instanceIdVar, processInstanceId);
        akitaScenario.log(String.format("Значение %s: %s", instanceIdVar, processInstanceId));
    }

    /**
     * Получаем id Сервис таски по её имени из файла процеса Camunda (.bpmn).
     *
     * @param bpmnFileName    путь до файла процесса Camunda. Можно указывать через переменные.
     * @param serviceTaskName имя ServiceTask кубика.
     * @param varName         имя переменной Akita, в которой сохранится результат.
     */
    @И("^из файла процесса Camunda \"(.+)\" получить ServiceTask id по имени \"(.+)\" и сохранить в переменную \"(.+)\"$")
    public void getServiceTaskIdByName(String bpmnFileName, String serviceTaskName, String varName) {
        String filePath = resolveVariables(bpmnFileName);
        Document document = XMLHelper.parseXml(filePath);
        Node node = XMLHelper.getNodeByTagAndAttributes(document, "bpmn:serviceTask", "name=" + serviceTaskName);
        Assertions.assertThat(node).as("В файле процесса Camunda нет ServiceTask с именем: " + serviceTaskName).isNotNull();
        akitaScenario.setVar(varName, node.getAttributes().getNamedItem("id").getNodeValue());
    }

    /**
     * Ожидаем, пока в истории не появится запись о кубике. Проверка выполняется каждую секунду, в течении указанного таймаута в секундах.
     *
     * @param nameOrIdFlag      ищем кубик по имени или по ID
     * @param nameOrIdVar       название или ID кубика
     * @param processInstanceId instance id проверяемого процесса.
     * @param timeout           максимальное время ожидания появления кубика в истории в секундах
     */
    @И("^ждём выполнения кубика( по (?:имени|ID))? \"(.+)\" в процессе с instance id \"(.+)\" в течение \"(.+)\" секунд$")
    public void waitForStepInHistory(String nameOrIdFlag, String nameOrIdVar, String processInstanceId, Integer timeout) {
        //Получаем список всех пройденных кубиков остортированных по увеличению времени завершения
        processInstanceId = processValue(processInstanceId);
        String nameOrId = processValue(nameOrIdVar);

        Function<ActivityInstanceStep, String> nameOrIdExtractor =
                nameOrIdFlag != null && nameOrIdFlag.contains("имени") ? ActivityInstanceStep::getActivityName : ActivityInstanceStep::getActivityId;

        String finalProcessInstanceId = processInstanceId;
        Awaitility
                .await("Ожидание появления кубика в истории")
                .pollInSameThread()
                .timeout(timeout, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> getActivityInstanceHistoryRecursive(finalProcessInstanceId)
                        .stream()
                        .anyMatch(step -> {
                            var nameOrIdCurrent = nameOrIdExtractor.apply(step);
                            return nameOrIdCurrent != null && nameOrIdCurrent.equals(nameOrId) && step.getEndTime() != null;
                        })
                );
    }

    /**
     * Шаг для выполнения complete задачи:
     * - сначала выполняется fetchAndLock в цикле, т.к. может быть для одного businessKey несколько подзадач
     * - далее вытаскивается taskId
     * - затем выполняется complete, ожидаем код 204
     *
     * @param instanceId  id процесса
     * @param businessKey businessKey процесса
     */
    @И("^выполняем complete текущей задачи для процесса с instanceId равным \"([^\"]+)\" и businessKey равным \"(.*)\"$")
    public void circleFetchWithBusinessKey(String instanceId, String businessKey) {
        businessKey = processValue(businessKey);
        akitaScenario.setVar("businessKey", businessKey);
        circleFetch(instanceId);
    }

    /**
     * Шаг для выполнения complete задачи:
     * - сначала выполняется fetchAndLock в цикле, т.к. может быть для одного businessKey несколько подзадач
     * - далее вытаскивается taskId
     * - затем выполняется complete, ожидаем код 204
     *
     * @param instanceId id процесса
     */
    @И("^выполняем complete текущей задачи для процесса с instanceId равным \"([^\"]+)\"$")
    public void circleFetch(String instanceId) {
        String processInstanceId = processValue(instanceId);
        akitaScenario.setVar("processInstanceId", processInstanceId);

        try {
            String fetchAndLockBody = resolveVariables(
                    FileHelper.getResourcesFileContent(processValue("${test.data.base.path}/common/camunda/fetchAndLock.json")));
            waitCamunda().until(() -> CamundaRequests.fetchAndLock(fetchAndLockBody).getStatusCode() == 200);

            String taskId = CamundaRequests.getExternalTaskFromProcessId().getBody().path("id[0]");
            akitaScenario.setVar("taskId", taskId);

            waitCamunda().until(() -> CamundaRequests.completeTaskByExternalTaskId(taskId).getStatusCode() == 204);
        } catch (ConditionTimeoutException e) {
            throw new CamundaException("Общее время ожидания в %d секунд истекло. \n%s", camundaActivityInstanceTimeout, e.getMessage());
        }
    }

    /**
     * Обновить (изменить/добавить/удалить) переменные процесса Camunda запросом POST process-instance/{instanceId}/variables.
     * Таблица параметров шага имеет 4 колонки:
     * - Действие - изменить, добавить или удалить переменную
     * - name - имя переменной (существующей - для удаления или изменения, новой - для добавления)
     * - type - тип переменной (для удаления переменной можно не указывать,
     * при изменении переменной можно также изменить и её тип, а можно только значение)
     * - value - новое значение переменной (для удаления переменной не указывается).
     * <p>
     * Пример:
     * И обновить переменные процесса Camunda с instanceId "instanceId" в соответствии с таблицей:
     * | Действие | name               | type    | value        |
     * | изменить | eskMcMngr          | String  | ${newValue1} |
     * | добавить | ${newVar1}         | Double  | 222.333      |
     * | удалить  | sparkFinanceReport |         |              |
     *
     * @param processInstanceId id инстанса процесса
     * @param params            параметры обновления переменных
     */
    @И("^обновить переменные процесса Camunda с instanceId \"(.*)\" в соответствии с таблицей:?$")
    public void updateProcessVariables(String processInstanceId, List<CamundaVariableUpdatingParam> params) {
        processInstanceId = processValue(processInstanceId);
        JSONObject body = new JSONObject(FileHelper.getResourcesFileContent(processValue("${test.data.base.path}/common/camunda/modifyVariables.json")));
        params.forEach(e -> {
            if (e.getAction().equalsIgnoreCase("удалить")) {
                body.getJSONArray("deletions").put(e.getName());
            } else {
                JSONObject object = new JSONObject();
                object.put("type", e.getType());
                object.put("value", e.getValue());
                body.getJSONObject("modifications").put(e.getName(), object);
            }
        });
        CamundaRequests.updateProcessVariables(processInstanceId, body.toString(5));
    }

    /**
     * Метод запрашивает последнюю версию задеплоеного процесса по входному параметру Definition Key
     * Затем сохраняет значение Definition Id из ответа в необходимую переменную
     *
     * @param definitionKey идентификатор Key процесса в Camunda.
     * @param definitionId  идентификатор ID процесса в Camunda.
     */
    @И("^получить Definition Id последнего процесса по Definition Key \"(.*)\" и сохранить в переменную \"(.*)\"")
    public void getLatestDefinitionIdByKey(String definitionKey, String definitionId) {
        akitaScenario.setVar(definitionId, CamundaRequests.getLatestDefinitionByKey(definitionKey).path("id[0]"));
    }

    /**
     * Метод удаляет инстанс в Camunda по ID
     *
     * @param processInstanceId идентификатор инстанса в Camunda.
     */
    @И("^удалить Process Instance по Instance ID \"(.*)\"")
    public void deleteProcessInstance(String processInstanceId) {
        String instanceId = processValue(processInstanceId);
        CamundaRequests.deleteProcessInstance(instanceId);
    }

    @И("^найти instanceId по businessKey \"(.*)\", definitionId \"(.*)\" и сохранить в переменную \"(.*)\"$")
    public void getProcessInstanceByBusinessKeyAndDefinitionId(String businessKey, String definitionId, String processInstanceId) {
        businessKey = processValue(businessKey);
        definitionId = processValue(definitionId);
        akitaScenario.setVar(processInstanceId,
                             CamundaRequests.getProcessInstanceByBusinessKey(businessKey).path("find { it.definitionId == '%s' }.id", definitionId));
    }
}
