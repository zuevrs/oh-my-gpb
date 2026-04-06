package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CamundaIncident {

    private String id;
    private String processDefinitionId;
    private String processInstanceId;
    private String executionId;
    private String incidentTimestamp;
    private String incidentType;
    private String activityId;
    private String failedActivityId;
    private String causeIncidentId;
    private String rootCauseIncidentId;
    private String configuration;
    private String incidentMessage;
    private String tenantId;
    private String jobDefinitionId;
    private String annotation;
    private String errorDetails;

    @Override
    public String toString() {
        return "Инцидент с ID " + id + "\n" +
                "processDefinitionId: " + processDefinitionId + "\n" +
                "processInstanceId: " + processInstanceId + "\n" +
                "executionId: " + executionId + "\n" +
                "incidentTimestamp: " + incidentTimestamp + "\n" +
                "incidentType: " + incidentType + "\n" +
                "activityId: " + activityId + "\n" +
                "failedActivityId: " + failedActivityId + "\n" +
                "causeIncidentId: " + causeIncidentId + "\n" +
                "rootCauseIncidentId: " + rootCauseIncidentId + "\n" +
                "configuration: " + configuration + "\n" +
                "incidentMessage: " + incidentMessage + "\n" +
                "tenantId: " + tenantId + "\n" +
                "jobDefinitionId: " + jobDefinitionId + "\n" +
                "annotation: " + annotation + "\n" +
                "errorDetails: " + errorDetails + "\n";
    }
}
