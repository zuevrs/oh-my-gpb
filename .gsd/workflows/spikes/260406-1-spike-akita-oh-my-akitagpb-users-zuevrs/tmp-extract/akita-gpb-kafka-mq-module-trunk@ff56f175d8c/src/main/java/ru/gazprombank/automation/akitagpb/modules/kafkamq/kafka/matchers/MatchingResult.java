package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.SavedDoc;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.MatchingError;

public class MatchingResult {
  private final List<MatchingError> errors;
  private final boolean isSuccess;
  private SavedDoc savedDoc;

  private MatchingResult(boolean isSuccess, List<MatchingError> errors) {
    this.isSuccess = isSuccess;
    this.errors = Collections.unmodifiableList(errors);
  }

  private MatchingResult(boolean isSuccess, List<MatchingError> errors, SavedDoc savedDoc) {
    this.isSuccess = isSuccess;
    this.errors = Collections.unmodifiableList(errors);
    this.savedDoc = savedDoc;
  }

  // Статические фабричные методы
  public static MatchingResult success() {
    return new MatchingResult(true, Collections.emptyList());
  }

  public static MatchingResult success(SavedDoc savedDoc) {
    return new MatchingResult(true, Collections.emptyList(), savedDoc);
  }

  public static MatchingResult withErrors(List<MatchingError> errors) {
    return new MatchingResult(false, new ArrayList<>(errors));
  }

  public static MatchingResult withError(MatchingError error) {
    return new MatchingResult(false, Collections.singletonList(error));
  }

  // Геттеры
  public boolean isSuccess() {
    return isSuccess;
  }

  // Геттеры
  public SavedDoc getSavedDoc() {
    return savedDoc;
  }

  public List<MatchingError> getErrors() {
    return errors;
  }

  // Дополнительные методы
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public boolean hasErrorOfType(MatchingError.MatchingErrorType type) {
    return errors.stream().anyMatch(e -> e.getType() == type);
  }
}
