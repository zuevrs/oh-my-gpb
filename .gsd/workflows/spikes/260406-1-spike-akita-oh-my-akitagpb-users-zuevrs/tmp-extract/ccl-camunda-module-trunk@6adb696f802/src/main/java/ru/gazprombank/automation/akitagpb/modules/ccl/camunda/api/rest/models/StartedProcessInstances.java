package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models;

import java.util.ArrayList;
import java.util.List;

public class StartedProcessInstances {

    private static final StartedProcessInstances instance = new StartedProcessInstances();
    private final List<StartedProcessInstance> processInstances = new ArrayList<>();

    private StartedProcessInstances() {
    }

    public static StartedProcessInstances getInstance() {
        return instance;
    }

    public List<StartedProcessInstance> getProcessInstances() {
        return processInstances;
    }

    public void addProcessInstance(StartedProcessInstance processInstance) {
        this.processInstances.add(processInstance);
    }

    public boolean isEmpty() {
        return processInstances.isEmpty();
    }

}
