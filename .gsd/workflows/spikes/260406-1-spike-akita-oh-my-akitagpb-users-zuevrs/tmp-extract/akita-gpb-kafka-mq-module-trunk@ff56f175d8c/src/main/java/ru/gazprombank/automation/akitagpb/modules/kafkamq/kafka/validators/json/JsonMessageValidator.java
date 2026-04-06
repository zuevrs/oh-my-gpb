package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.json;

import org.json.JSONException;
import org.json.JSONObject;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.MessageValidator;

public class JsonMessageValidator implements MessageValidator {
  @Override
  public void validate(String message) {
    try {
      new JSONObject(message); // Просто проверяем, что это валидный JSON
    } catch (JSONException e) {
      throw new AssertionError("Message is not valid JSON: " + message, e);
    }
  }
}
