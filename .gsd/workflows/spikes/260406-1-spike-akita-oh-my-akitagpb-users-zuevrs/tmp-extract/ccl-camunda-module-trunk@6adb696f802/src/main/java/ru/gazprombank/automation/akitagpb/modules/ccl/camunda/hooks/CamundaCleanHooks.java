package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.hooks;

import io.cucumber.java.AfterAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.DeployedDefinition;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.DeployedDefinitions;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.StartedProcessInstance;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.StartedProcessInstances;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.requests.CamundaRequests;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

import java.util.List;
import java.util.stream.Collectors;


public class CamundaCleanHooks extends BaseMethods {

    private static final Logger logger = LoggerFactory.getLogger(CamundaCleanHooks.class);

    /**
     * Удаляем инстанс, BPMN-процессы и DMN-таблицы (если что-то было загружена) в Camunda после завершения всех тестов.
     */
    @AfterAll
    public static void deleteCamundaDeployments() {
        //Если за время выполнения был хотя бы одни деплоймент - выполняем параллельное удаление
        if (!DeployedDefinitions.getInstance().isEmpty()) {
            logger.debug("Старт удаления деплойментов");
            List<DeployedDefinition> deployedDefinitions = DeployedDefinitions.getInstance().getDeployedDefinitions();
            logger.debug(String.format("Всего было сделано %d деплойментов\n", deployedDefinitions.size()));
            deployedDefinitions = deployedDefinitions.stream().filter(e -> !e.isDoNotDelete()).collect(Collectors.toList());
            logger.debug(String.format("Из них будет удалено %d деплойментов\n", deployedDefinitions.size()));
            deployedDefinitions.forEach(e -> {
                var response = CamundaRequests.deleteCamundaDeploy(e.getDeploymentId(), e.isNeedDeleteCascade());
                if (response.getStatusCode() != 204) {
                    logger.debug(String.format("Деплоймент удалить не получилось:\n%s\n\n", response.prettyPrint()));
                }
            });
            logger.debug("Конец удаления деплойментов");
        }
    }

    /**
     * Удаляем инстансы процессов в Camunda, созданных в тестах, после завершения всех тестов.
     */
    @AfterAll(order = 10001)
    public static void deleteStartedProcessInstances() {
        if (!StartedProcessInstances.getInstance().isEmpty()) {
            logger.debug("Старт удаления инстансов процессаов");
            List<StartedProcessInstance> processInstances = StartedProcessInstances.getInstance().getProcessInstances();
            logger.debug(String.format("Всего было создано %d инстансов процессов\n", processInstances.size()));
            processInstances = processInstances.stream().filter(e -> !e.isDoNotDelete()).collect(Collectors.toList());
            logger.debug(String.format("Из них будет удалено %d инстансов процессов\n", processInstances.size()));
            processInstances.forEach(e -> {
                var response = CamundaRequests.deleteProcessInstanceAndGetResponse(e.getId());
                if (response.getStatusCode() != 204) {
                    logger.debug(String.format("Инстанс процесса удалить не получилось:\n%s\n\n", response.prettyPrint()));
                }
            });
            logger.debug("Конец удаления инстансов процессаов");
        }
    }
}
