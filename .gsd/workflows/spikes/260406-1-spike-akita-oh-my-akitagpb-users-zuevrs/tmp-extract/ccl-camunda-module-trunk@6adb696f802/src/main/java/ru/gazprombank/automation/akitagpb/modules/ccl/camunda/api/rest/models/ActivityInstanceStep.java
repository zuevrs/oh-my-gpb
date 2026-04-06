package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ActivityInstanceStep {

    private String id;
    private String parentActivityInstanceId;
    private String activityId;
    private String activityName;
    private String activityType;
    private String processDefinitionKey;
    private String processDefinitionId;
    private String processInstanceId;
    private String executionId;
    private String taskId;
    private String calledProcessInstanceId;
    private String calledCaseInstanceId;
    private String assignee;
    private String startTime;
    private String endTime;
    private String durationInMillis;
    private String canceled;
    private String completeScope;
    private String tenantId;
    private String removalTime;
    private String rootProcessInstanceId;
}
