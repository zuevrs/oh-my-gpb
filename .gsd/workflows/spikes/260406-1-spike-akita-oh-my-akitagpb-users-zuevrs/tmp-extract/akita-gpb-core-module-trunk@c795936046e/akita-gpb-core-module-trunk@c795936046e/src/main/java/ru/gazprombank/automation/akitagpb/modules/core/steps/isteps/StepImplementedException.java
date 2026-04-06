package ru.gazprombank.automation.akitagpb.modules.core.steps.isteps;

public class StepImplementedException extends RuntimeException {

  public static final String ERROR_MESSAGE = "Требуется реализация данного шага";

  public StepImplementedException(String message) {
    super(message);
  }
}
