package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.text;

import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.MessageValidator;

public class TextMessageValidator implements MessageValidator {
  @Override
  public void validate(String message) {
    if (message == null || message.trim().isEmpty()) {
      throw new AssertionError("Message is empty or null");
    }
    // Можно добавить дополнительные проверки для текста
  }
}
