package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.header;

import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.SavedDoc;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.MatchingError;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.last.MatchCondition;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MatchingResult;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MessageMatcher;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.header.HeaderValidator;

public class CustomHeaderMatcher implements MessageMatcher {
  private final List<MatchingError> errors;

  public CustomHeaderMatcher() {
    this.errors = new ArrayList<>();
  }

  @Override
  public <T extends MatchCondition> MatchingResult matches(
      ConsumerRecord<?, ?> record, List<T> conditions) {
    boolean matched = false;
    for (var condition : conditions) {
      var header = record.headers().lastHeader(condition.getField());
      if (header == null) {
        if (condition.isRequired()) {
          errors.add(MatchingError.missingHeader(condition.getField()));
        }
        continue;
      }
      String headerValue = new String(header.value());
      boolean allConditionsMatched = true;
      boolean hasAutoCondition =
          condition.getConditions().stream().anyMatch(c -> "auto".equals(c.getType()));
      if (hasAutoCondition) {
        HeaderValidator.Validator validator = HeaderValidator.getValidator(condition.getField());
        if (validator != null) {
          if (!validator.validate(headerValue, condition.getField(), errors)) {
            allConditionsMatched = false;
            continue;
          }
        }
      }
      for (MatchCondition.Condition subCondition : condition.getConditions()) {
        if ("auto".equals(subCondition.getType()) || "required".equals(subCondition.getType())) {
          continue;
        }
        if (!condition.checkCondition(headerValue, subCondition)) {
          errors.add(
              MatchingError.headerMismatch(
                  condition.getField(),
                  subCondition.getType() + ": " + subCondition.getValue(),
                  headerValue));
          allConditionsMatched = false;
        }
      }
      if (allConditionsMatched) {
        matched = true;
      }
    }
    if (matched) {
      System.out.println(conditions.get(0).getField());
      String result = new String(record.headers().lastHeader(conditions.get(0).getField()).value());
      return MatchingResult.success(new SavedDoc(record, result));
    }
    return MatchingResult.withErrors(errors);
  }

  public List<MatchingError> getErrors() {
    return new ArrayList<>(errors);
  }
}
