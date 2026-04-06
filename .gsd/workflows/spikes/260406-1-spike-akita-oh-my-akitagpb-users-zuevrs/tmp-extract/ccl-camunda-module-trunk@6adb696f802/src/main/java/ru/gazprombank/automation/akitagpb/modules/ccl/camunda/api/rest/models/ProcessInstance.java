package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ProcessInstance {

    private List<Object> links;
    private String id;
    private String definitionId;
    private String businessKey;
    private String caseInstanceId;
    private Boolean ended;
    private Boolean suspended;
    private String tenantId;
}
