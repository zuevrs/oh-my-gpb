package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.json;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.SavedDoc;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.MatchingError;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.last.MatchCondition;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MatchingResult;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MessageMatcher;

public class JsonAllFieldsPositiveMatcher implements MessageMatcher {
  private final List<MatchingError> errors = new ArrayList<>();

  @Override
  public <T extends MatchCondition> MatchingResult matches(
      ConsumerRecord<?, ?> record, List<T> conditions) {
    int countOfMismatches = 0;
    String lastFieldValue = null;

    for (var condition : conditions) {
      try {
        String fieldValue = JsonPath.read(record.value().toString(), condition.getField());
        lastFieldValue = fieldValue;
        boolean allConditionsMatched = true;

        for (MatchCondition.Condition subCondition : condition.getConditions()) {
          if (!condition.checkCondition(fieldValue, subCondition)) {
            errors.add(
                MatchingError.fieldMismatch(
                    condition.getField(),
                    subCondition.getType() + ": " + subCondition.getValue(),
                    fieldValue));
            allConditionsMatched = false;
          }
        }

        if (!allConditionsMatched) {
          countOfMismatches++;
        }
      } catch (PathNotFoundException e) {
        if (condition.isRequired()) {
          errors.add(MatchingError.missingField(condition.getField()));
          countOfMismatches++;
        }
      }
    }

    if (countOfMismatches == 0) {
      return MatchingResult.success(new SavedDoc(record, lastFieldValue));
    }
    return MatchingResult.withErrors(errors);
  }
}
