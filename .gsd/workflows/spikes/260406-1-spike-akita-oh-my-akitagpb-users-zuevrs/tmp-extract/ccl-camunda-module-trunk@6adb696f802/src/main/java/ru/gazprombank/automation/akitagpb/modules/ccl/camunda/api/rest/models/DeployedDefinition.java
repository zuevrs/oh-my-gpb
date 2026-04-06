package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
@AllArgsConstructor
public class DeployedDefinition {

    private String filePath;
    private Map<String, Object> deployedDefinitionInfo;
    private boolean doNotDelete;
    private boolean needDeleteCascade;

    public String getDeploymentId() {
        return (String) deployedDefinitionInfo.get("deploymentId");
    }

    public String getResource() {
        return (String) deployedDefinitionInfo.get("resource");
    }

    public String getKey() {
        return (String) deployedDefinitionInfo.get("key");
    }
}
