package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Модельный класс для описания объекта ChildTransitionInstance, массив которых приходит в ответе на запрос
 * GET /process-instance/{processInstanceId}/activity-instances в поле "childTransitionInstances": []
 */
@Setter
@Getter
@NoArgsConstructor
public class ChildTransitionInstance {

    private String id;
    private String parentActivityInstanceId;
    private String activityId;
    private String activityType;
    private String processInstanceId;
    private String processDefinitionId;
    private String executionId;
    private String activityName;
    private List<String> incidentIds;
    private List<Object> incidents;
    private String targetActivityId;

}
