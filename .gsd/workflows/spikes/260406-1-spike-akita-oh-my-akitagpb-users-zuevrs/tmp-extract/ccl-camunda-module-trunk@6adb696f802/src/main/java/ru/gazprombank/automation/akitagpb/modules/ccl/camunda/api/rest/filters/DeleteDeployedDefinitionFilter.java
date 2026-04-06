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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.requests.CamundaRequests.camundaBaseURL;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.getDeployedDefinitionInfo;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.steps.CamundaSteps.DELETE_DEPLOYMENT_CASCADE_TAG;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.steps.CamundaSteps.SAVE_PROCESS_INSTANCE_TAG;

public class DeleteDeployedDefinitionFilter implements Filter {

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        final Response response = ctx.next(requestSpec, responseSpec);
        String requestPattern = camundaBaseURL + "/deployment/(.+)\\?.+";

        if (
                requestSpec.getMethod().equalsIgnoreCase("DELETE")
                && response.getStatusCode() == 204
                && requestSpec.getURI().matches(requestPattern)
        ) {
            Pattern p = Pattern.compile(requestPattern);
            Matcher m = p.matcher(requestSpec.getURI());
            if(m.find()){
                String deployedDefinitionId = m.group(1);
                DeployedDefinitions.getInstance().deleteDeployedDefinition(deployedDefinitionId);
            }
        }

        return response;
    }
}
