package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators;

public interface MessageValidator {
  void validate(String message) throws ValidationException;
}
