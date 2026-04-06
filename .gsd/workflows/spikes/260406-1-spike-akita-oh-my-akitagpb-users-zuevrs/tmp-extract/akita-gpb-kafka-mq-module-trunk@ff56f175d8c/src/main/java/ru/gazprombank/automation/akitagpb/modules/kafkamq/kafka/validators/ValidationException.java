package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators;

public class ValidationException extends Exception {
  private final ValidationErrorType errorType;

  public ValidationException(ValidationErrorType errorType) {
    this.errorType = errorType;
  }

  public enum ValidationErrorType {
    TECHNICAL, // Невалидный JSON, XML и т.д.
    BUSINESS // Отсутствуют обязательные поля
  }
}
