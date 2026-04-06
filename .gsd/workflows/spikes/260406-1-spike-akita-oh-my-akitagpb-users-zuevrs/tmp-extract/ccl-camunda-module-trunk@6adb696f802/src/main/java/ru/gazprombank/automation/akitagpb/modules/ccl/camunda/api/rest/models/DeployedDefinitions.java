package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models;

import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.XMLHelper;
import ru.gazprombank.automation.akitagpb.modules.core.steps.isteps.IBaseMethods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DeployedDefinitions {

    private static final DeployedDefinitions instance = new DeployedDefinitions();
    private final List<DeployedDefinition> deployedDefinitions = new ArrayList<>();
    private final Map<String, String> lastDeployedFiles = new HashMap<>();

    private DeployedDefinitions() {
    }

    public static DeployedDefinitions getInstance() {
        return instance;
    }

    public boolean isDeployed(String filePath) {
        String key = getIdFromCamundaFile(filePath);
        if (lastDeployedFiles.containsKey(key)) {
            return lastDeployedFiles.get(key).equals(filePath);
        } else {
            return false;
        }
    }

    public void addDeployedDefinition(DeployedDefinition deployedDefinition) {
        deployedDefinitions.add(deployedDefinition);
        lastDeployedFiles.put(deployedDefinition.getKey(), deployedDefinition.getFilePath());
    }

    public DeployedDefinition getLastDeployedDefinitionByKey(String filePath) {
        String key = getIdFromCamundaFile(filePath);
        return deployedDefinitions.stream().filter(d -> d.getKey().equals(key)).reduce((first, second) -> second)
                .orElseThrow(() -> new RuntimeException("Не удалось найти deployedDefinition для файла " + filePath));
    }

    public boolean isEmpty() {
        return deployedDefinitions.isEmpty();
    }

    public List<DeployedDefinition> getDeployedDefinitions() {
        return deployedDefinitions;
    }

    private String getIdFromCamundaFile(String filePath) {
        String tagName = filePath.endsWith("bpmn") ? "bpmn:process" : "decision";
        return XMLHelper.getXmlElementAttributeValue(XMLHelper.parseXml(filePath), tagName, "id");
    }

    public void deleteDeployedDefinition(String deployedDefinitionId) {
        Optional<DeployedDefinition> deployedDefinition = deployedDefinitions.stream()
                .filter(dd -> dd.getDeploymentId().equals(deployedDefinitionId)).findFirst();
        deployedDefinition.ifPresent(dd -> {
            deployedDefinitions.remove(dd);
            lastDeployedFiles.remove(dd.getKey());
        });
    }
}
