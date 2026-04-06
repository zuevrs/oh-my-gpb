package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.header;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.MatchingError;

public abstract class HeaderValidator {
  @FunctionalInterface
  public interface Validator {
    boolean validate(String headerValue, String headerKey, List<MatchingError> errors);
  }

  // Реестр валидаторов
  private static final Map<String, Validator> VALIDATORS = new HashMap<>();

  public static void registerValidator(String keyword, Validator validator) {
    VALIDATORS.put(keyword.toLowerCase(), validator);
  }

  public static Validator getValidator(String headerKey) {
    String keyLower = headerKey.toLowerCase();
    if (keyLower.contains("uuid")) {
      return VALIDATORS.get("uuid");
    } else if (keyLower.contains("timestamp")) {
      return VALIDATORS.get("timestamp");
    } else if (keyLower.contains("traceid") || keyLower.contains("trace_id")) {
      return VALIDATORS.get("traceid");
    }
    return null;
  }

  // Валидаторы
  private static final Validator UUID_VALIDATOR =
      (headerValue, headerKey, errors) -> {
        try {
          UUID.fromString(headerValue);
          return true;
        } catch (IllegalArgumentException e) {
          errors.add(MatchingError.headerMismatch(headerKey, "Valid UUID", headerValue));
          return false;
        }
      };

  private static final Validator TIMESTAMP_VALIDATOR =
      (headerValue, headerKey, errors) -> {
        try {
          ZonedDateTime.parse(headerValue, DateTimeFormatter.ISO_ZONED_DATE_TIME);
          return true;
        } catch (DateTimeParseException e) {
          errors.add(MatchingError.headerMismatch(headerKey, "ISO 8601 timestamp", headerValue));
          return false;
        }
      };

  private static final Validator TRACE_ID_VALIDATOR =
      (headerValue, headerKey, errors) -> {
        if (headerValue != null && headerValue.matches("[a-f0-9]{32}")) {
          return true;
        }
        errors.add(
            MatchingError.headerMismatch(
                headerKey, "32-character hexadecimal trace ID", headerValue));
        return false;
      };

  // Инициализация валидаторов
  static {
    registerValidator("uuid", UUID_VALIDATOR);
    registerValidator("timestamp", TIMESTAMP_VALIDATOR);
    registerValidator("traceid", TRACE_ID_VALIDATOR);
  }
}
