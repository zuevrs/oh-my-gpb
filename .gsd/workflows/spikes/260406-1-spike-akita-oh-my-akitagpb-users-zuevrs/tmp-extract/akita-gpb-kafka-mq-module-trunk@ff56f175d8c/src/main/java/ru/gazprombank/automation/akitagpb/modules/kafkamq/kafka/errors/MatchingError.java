package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors;

import java.util.Map;
import lombok.Getter;

@Getter
public class MatchingError {
  public enum MatchingErrorType {
    MISSING_FIELD,
    FIELD_MISMATCH,
    FIELD_MATCH,
    ALL_FIELDS_MATCH,
    FORMAT_ERROR,
    BUSINESS_RULE_VIOLATION,
    MISSING_HEADER,
    HEADER_MISMATCH,
  }

  // Геттеры
  private final MatchingErrorType type;
  private String field;
  private Map<Object, Object> fields;
  private final String message;
  private final Object expected;
  private final Object actual;

  private MatchingError(
      MatchingErrorType type, String field, String message, Object expected, Object actual) {
    this.type = type;
    this.field = field;
    this.message = message;
    this.expected = expected;
    this.actual = actual;
  }

  private MatchingError(
      MatchingErrorType type,
      Map<Object, Object> fields,
      String message,
      Object expected,
      Object actual) {
    this.type = type;
    this.fields = fields;
    this.message = message;
    this.expected = expected;
    this.actual = actual;
  }

  // Статические конструкторы
  public static MatchingError missingField(String field) {
    return new MatchingError(
        MatchingErrorType.MISSING_FIELD, field, "Required field is missing", null, null);
  }

  public static MatchingError fieldMismatch(String field, Object expected, Object actual) {
    return new MatchingError(
        MatchingErrorType.FIELD_MISMATCH,
        field,
        String.format("Field value mismatch. Expected: %s, Actual: %s", expected, actual),
        expected,
        actual);
  }

  public static MatchingError missingHeader(String field) {
    return new MatchingError(
        MatchingErrorType.MISSING_HEADER, field, "Required header is missing", null, null);
  }

  public static MatchingError headerMismatch(String field, Object expected, Object actual) {
    return new MatchingError(
        MatchingErrorType.HEADER_MISMATCH,
        field,
        String.format("Header value mismatch. Expected: %s, Actual: %s", expected, actual),
        expected,
        actual);
  }

  public static MatchingError fieldMatch(String field, Object expected, Object actual) {
    return new MatchingError(
        MatchingErrorType.FIELD_MATCH,
        field,
        String.format("Field value match. Expected: %s, Actual: %s", expected, actual),
        expected,
        actual);
  }

  public static MatchingError allFieldsMatch(
      Map<Object, Object> fields, Object expected, Object actual) {
    return new MatchingError(
        MatchingErrorType.ALL_FIELDS_MATCH,
        fields,
        String.format("Field value match. Expected: %s, Actual: %s", expected, actual),
        expected,
        actual);
  }
  //    public static MatchingError businessRuleViolation(String rule, String details) {
  //        return new MatchingError(
  //                MatchingErrorType.BUSINESS_RULE_VIOLATION,
  //                null,
  //                String.format("Business rule violation: %s. Details: %s", rule, details),
  //                null,
  //                null
  //        );
  //    }
}
