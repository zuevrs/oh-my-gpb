package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.filters;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.ProcessInstance;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.StartedProcessInstance;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.StartedProcessInstances;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.api.AkitaScenario;

import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.requests.CamundaRequests.camundaBaseURL;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.steps.CamundaSteps.SAVE_PROCESS_INSTANCE_TAG;

public class StartProcessInstanceFilter implements Filter {

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        final Response response = ctx.next(requestSpec, responseSpec);
        String requestPattern = camundaBaseURL + "/process-definition/.+/start";

        if (response.getStatusCode() == 200 && requestSpec.getURI().matches(requestPattern)) {
            var processInstance = response.as(ProcessInstance.class);
            var startedProcess = new StartedProcessInstance(
                    processInstance.getId(),
                    AkitaScenario.getInstance().getScenario().getSourceTagNames().contains(SAVE_PROCESS_INSTANCE_TAG)
            );
            StartedProcessInstances.getInstance().addProcessInstance(startedProcess);
        }

        return response;
    }
}
