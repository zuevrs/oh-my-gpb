package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StartedProcessInstance {

    private String id;
    private boolean doNotDelete;

}
