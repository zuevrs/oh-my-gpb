package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.steps;

import io.cucumber.java.ru.И;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.ActivityInstanceStep;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.CamundaIncident;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.requests.CamundaRequests;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.exceptions.CamundaException;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.assertions.MainAssert;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels.StringAssertableConditionParam;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.getActivityInstanceHistoryRecursive;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.getExternalTaskIdFromIncident;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.getVariableHistory;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;

public class CamundaCheckSteps extends BaseMethods {

    /**
     * Получаем список всех активных инстансов, которые были пройдены (включая все кубики в вызванных процессах), по ProcessInstanceId.<br>
     * Проверяем, что id или имя активити и порядок их следования совпадают с указанным в списке под шагом.
     *
     * @param nameOrId          чтобы проверять по имени кубиков, необходимо передать параметр "по их имени";
     *                          если передать "по их ID", или не передавать этот параметр - проверка будет происходить по ID кубиков.
     * @param processInstanceId instance id проверяемого процесса.
     * @param expectedChain     список имён или id кубиков в ожидаемом порядке.
     */
    @И("^проверить порядок прохождение кубиков( по их (?:имени|ID))? в процессе с instance id \"(.+)\"$")
    public void checkChainSteps(String nameOrId, String processInstanceId, List<String> expectedChain) {
        //Получаем список всех пройденных кубиков остортированных по увеличению вермени завершения
        processInstanceId = processValue(processInstanceId);
        var chain = getActivityInstanceHistoryRecursive(processInstanceId);

        //проверяем, что порядок выполнения кубиков совпадает
        //могут быть не указанные в expectedChain кубики
        Function<ActivityInstanceStep, String> nameOrIdExtractor =
                nameOrId != null && nameOrId.contains("имени") ? ActivityInstanceStep::getActivityName : ActivityInstanceStep::getActivityId;
        Assertions.assertThat(chain).as("Ожидаемые кубики не найдены в истории или порядок их выполнения отличается от ожидаемого.")
                .extracting(nameOrIdExtractor).containsSubsequence(expectedChain);
    }

    /**
     * Получаем список всех активных инстансов, которые были пройдены (включая все кубики в вызванных процессах), по ProcessInstanceId.<br>
     * Проверяем, что id или имя активити отсутствуют среди пройденных на процессе и вложенных в процесс.
     *
     * @param nameOrId          чтобы проверять по имени кубиков, необходимо передать параметр "по их имени";
     *                          если передать "по их ID", или не передавать этот параметр - проверка будет происходить по ID кубиков.
     * @param processInstanceId instance id проверяемого процесса.
     * @param unexpectedChain   список имён или id кубиков, которых не должно быть среди пройденных.
     */
    @И("^проверить отсутствие кубиков среди пройденных( по их (?:имени|ID))? в процессе с instance id \"(.+)\"$")
    public void notCheckChainSteps(String nameOrId, String processInstanceId, List<String> unexpectedChain) {
        //Получаем список всех пройденных кубиков остортированных по увеличению времени завершения
        processInstanceId = processValue(processInstanceId);
        var chain = getActivityInstanceHistoryRecursive(processInstanceId);

        //проверяем, что среди пройденных кубиков отсутствуют указанные
        Function<ActivityInstanceStep, String> nameOrIdExtractor =
                nameOrId != null && nameOrId.contains("имени") ? ActivityInstanceStep::getActivityName : ActivityInstanceStep::getActivityId;
        Assertions.assertThat(chain).as("Ожидаемые кубики найдены в истории.").extracting(nameOrIdExtractor)
                .doesNotContainAnyElementsOf(unexpectedChain);
    }

    /**
     * Данный шаг проверяет на процессе наличие переменных и их значения (информация будет браться из истории инстанса или из текущего состояния).
     * Необходимо указать структуру в виде таблицы:
     * | variable1 | value1 |
     * | variable2 |        |
     * | variable3 | value3 |
     * <p>
     * Можно не указывать значение ожидаемой переменной (пример: variable2), тогда оно проверяться не будет.
     * Шаг выгрузит все переменные из процесса, и проверит что их действительно только 3, и это variable1 = value1, variable2, variable3 = value3.
     * В ином случае, шаг покажет несоответстие фактического результата, ожидаемому (какие переменные отсутствуют или имеют неверные значения).
     *
     * @param historyFlag       строгая или нет проверка
     * @param processInstanceId id инстанса процесса.
     * @param expectedVariables таблица с именами переменных и их значениями (опционально)
     */
    @И("^проверить (переменные|все переменные|переменные из истории|все переменные из истории)? Camunda с instanceId \"(.+)\"$")
    public void checkExistenceVariables(String historyFlag, String processInstanceId, Map<String, String> expectedVariables) {
        SoftAssertions softAssertions = new SoftAssertions();
        String instanceId = processValue(processInstanceId);

        Map<String, String> responseVariables =
                historyFlag.contains("из истории") ? getVariableHistory(instanceId) : CamundaRequests.getProcessInstanceVariablesAsMap(instanceId);

        if (historyFlag.contains("все")) {
            softAssertions.assertThat(expectedVariables.keySet()).hasSize(responseVariables.keySet().size())
                    .hasSameElementsAs(responseVariables.keySet());
        }

        for (Map.Entry<String, String> entry : expectedVariables.entrySet()) {
            if (entry.getValue() != null) {
                String name = entry.getKey();
                String value = processValue(entry.getValue());

                if (value.equals("not_null")) {
                    softAssertions.assertThat(responseVariables.get(name))
                            .as("Переменная %s. Фактическое значение: \"%s\"\n Ожидаемое значение : \"%s\"", name, responseVariables.get(name), value)
                            .isNotNull();
                    continue;
                }

                softAssertions.assertThat(responseVariables.get(name))
                        .as("Переменная %s. Фактическое значение: \"%s\"\n Ожидаемое значение : \"%s\"", name, responseVariables.get(name), value)
                        .isEqualTo(value);
            }
        }

        softAssertions.assertAll();
    }

    /**
     * Шаг для проверки сообщения инцидента в Камунде.
     * - сначала выполняется запрос GET incident/processInstanceId={processInstanceId}
     * - получаем externalTaskId: если configuration != null, то externalTaskId = configuration
     * - если в первом запросе configuration == null, отправляем запросы GET incident/{id}, чтобы получить инцидент-причину,
     * имеющий configuration != null
     * - затем выполняется запрос external-task/{externalTaskId}/errorDetails для получения сообщения инцидента
     * - проверка сообщения как строки на соответствие условиям
     * <p>
     * Таблица параметров шага содержит 2 колонки: "Условие" (равно, не равно, пустая, не пустая, содержит, не содержит, начинается с,
     * заканчивается на, регулярное выражение) и "Значение" (ожидаемое значение).
     * Пример 1:
     * И сообщение инцидента на процессе с instanceId "instanceId" соответствует условиям:
     * | Условие              | Значение                                                     |
     * | регулярное выражение | [\s\S]*Получены ошибки при получении в АС Олимп: 4203[\s\S]* |
     * | содержит             | Получены ошибки при получении в АС Олимп: 4203               |
     * | начинается с         | ru.gpb.ccl.limitmanager.exception.LimitManagerException      |
     * Пример 2:
     * И сообщение инцидента на процессе с instanceId "instanceId", полученное в течение 10 секунд, соответствует условиям:
     * | Условие              | Значение                                                     |
     * | регулярное выражение | [\s\S]*Получены ошибки при получении в АС Олимп: 4203[\s\S]* |
     *
     * @param processInstanceId id инстанса процесса
     * @param timeout           время ожидания инцидента
     * @param conditions        условия проверки
     */
    @И("^сообщение инцидента на процессе с instanceId \"(.*)\"(, полученное в течение (\\d+) секунд,)? соответствует условиям:?$")
    public void assertCamundaIncidentErrorDetails(String processInstanceId, Integer timeout, List<StringAssertableConditionParam> conditions) {
        processInstanceId = processValue(processInstanceId);
        CamundaIncident[] incidents = CamundaRequests.waitAndGetIncidentsByProcessInstanceId(processInstanceId, timeout);
        if (incidents.length == 0) {
            throw new RuntimeException("На данном процессе нет инцидентов.");
        } else if (incidents.length > 1) {
            throw new RuntimeException(
                    "На процессе больше 1 инцидента. Необходима доработка данного шага по дополнительным условиям.\n" + Arrays.toString(incidents));
        }

        var externalTaskId = getExternalTaskIdFromIncident(incidents[0]);
        var actualErrorDetails = CamundaRequests.getExternalTaskErrorDetailsByExternalTaskId(externalTaskId).then().statusCode(200).extract().body()
                .asPrettyString();

        MainAssert mainAssert = new MainAssert();
        conditions.forEach(e -> mainAssert.getAssertion(e.getType()).accept(e, actualErrorDetails));
        mainAssert.assertAll();
    }

    /**
     * Метод проверяет завершение инстанса на процессе
     * Запрашивается информация по инстансу. Если ответ != 200 , значит инстанс удален
     *
     * @param processInstanceId идентификатор инстанса в Camunda.
     */
    //TODO: переписать на метод по запросу информации инстанса из истории и проверять его статус
    @И("^проверить завершение процесса с instanceId \"(.*)\"$")
    public void checkProcessIsComplete(String processInstanceId) {
        String instanceId = processValue(processInstanceId);
        try {
            Awaitility.await().pollInSameThread().timeout(120, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                    .until(() -> CamundaRequests.getProcessInstance(instanceId).getStatusCode() != 200);
        } catch (ConditionTimeoutException e) {
            throw new CamundaException("Время ожидания истекло. \n%s", e.getMessage());
        }
    }
}
