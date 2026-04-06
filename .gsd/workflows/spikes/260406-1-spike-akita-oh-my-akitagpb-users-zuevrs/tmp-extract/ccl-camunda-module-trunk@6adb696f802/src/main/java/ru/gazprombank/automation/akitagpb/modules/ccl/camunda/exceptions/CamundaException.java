package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.exceptions;

public class CamundaException extends RuntimeException {

    public CamundaException(String message, Object... params) {
        super(String.format(message, params));
    }

    public CamundaException(String message) {
        super(message);
    }
}
