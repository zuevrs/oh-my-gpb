package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators;

import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.MessageType;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.json.JsonMessageValidator;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.text.TextMessageValidator;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.xml.XmlMessageValidator;

public class ValidatorFactory {
  public static MessageValidator getValidator(MessageType messageType) {
    return switch (messageType) {
      case JSON -> new JsonMessageValidator();
      case XML -> new XmlMessageValidator();
      case TEXT -> new TextMessageValidator();
      case HEADER -> null;
    };
  }
}
