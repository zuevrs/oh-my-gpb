package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.filters;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.MultiPartSpecification;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.DeployedDefinition;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.DeployedDefinitions;
import ru.gazprombank.automation.akitagpb.modules.core.steps.isteps.IBaseMethods;

import java.io.File;
import java.util.List;
import java.util.Map;

import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.requests.CamundaRequests.camundaBaseURL;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.getDeployedDefinitionInfo;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.steps.CamundaSteps.DELETE_DEPLOYMENT_CASCADE_TAG;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.steps.CamundaSteps.SAVE_PROCESS_INSTANCE_TAG;

public class AddDeployedDefinitionFilter implements Filter {

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        final Response response = ctx.next(requestSpec, responseSpec);
        String requestPattern = camundaBaseURL + "/deployment/create";

        if (response.getStatusCode() == 200 && requestSpec.getURI().matches(requestPattern)) {

            DeployedDefinition deployedDefinition = getDeployedDefinitions(response);

            List<MultiPartSpecification> multiPartList = requestSpec.getMultiPartParams();
            MultiPartSpecification multiPart = multiPartList.get(0);

            if (multiPart.getContent() instanceof File) {
                String filePath = ((File) multiPart.getContent()).getPath();
                deployedDefinition.setFilePath(filePath);
            }

            DeployedDefinitions.getInstance().addDeployedDefinition(deployedDefinition);
        }

        return response;
    }

    /**
     * Метод получает объект deployedDefinitions bpmn-процесса или dmn-таблицы, из полученного ответа деплоя в формате json.
     *
     * @param response ответ на запрос деплоя в Camunda.
     * @return значение первого ключа из списка deployedDecisionDefinitions.
     */
    private DeployedDefinition getDeployedDefinitions(Response response) {
        Map<String, Object> deployedDefinitionInfo = getDeployedDefinitionInfo(response);
        var scenarioTags = IBaseMethods.akitaScenario.getScenario().getSourceTagNames();
        boolean doNotDelete = scenarioTags.contains(SAVE_PROCESS_INSTANCE_TAG);
        boolean needDeleteCascade = scenarioTags.contains(DELETE_DEPLOYMENT_CASCADE_TAG);
        return new DeployedDefinition("", deployedDefinitionInfo, doNotDelete, needDeleteCascade);
    }

}
