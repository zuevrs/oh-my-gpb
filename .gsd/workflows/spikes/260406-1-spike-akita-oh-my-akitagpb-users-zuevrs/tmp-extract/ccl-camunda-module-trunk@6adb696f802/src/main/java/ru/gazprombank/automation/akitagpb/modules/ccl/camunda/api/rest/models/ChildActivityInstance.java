package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Модельный класс для описания объекта ChildActivityInstance, массив которых приходит в ответе на запрос
 * GET /process-instance/{processInstanceId}/activity-instances в поле "childActivityInstances": []
 */
@Setter
@Getter
public class ChildActivityInstance {

    private String id;
    private String parentActivityInstanceId;
    private String activityId;
    private String activityType;
    private String processInstanceId;
    private String processDefinitionId;
    private List<ChildActivityInstance> childActivityInstances;
    private List<ChildTransitionInstance> childTransitionInstances;
    private List<String> executionIds;
    private String activityName;
    private List<String> incidentIds;
    private List<Object> incidents;
    private String name;

}
