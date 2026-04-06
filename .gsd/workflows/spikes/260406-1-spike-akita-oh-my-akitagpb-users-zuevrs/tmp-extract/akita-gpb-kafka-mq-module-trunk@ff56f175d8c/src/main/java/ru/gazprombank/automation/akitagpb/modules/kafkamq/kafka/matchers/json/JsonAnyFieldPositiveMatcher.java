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

public class JsonAnyFieldPositiveMatcher implements MessageMatcher {
  private final List<MatchingError> errors = new ArrayList<>();

  @Override
  public <T extends MatchCondition> MatchingResult matches(
      ConsumerRecord<?, ?> record, List<T> conditions) {
    boolean anyMatched = false;
    for (var condition : conditions) {
      try {
        String fieldValue = JsonPath.read(record.value().toString(), condition.getField());
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

        if (allConditionsMatched) {
          anyMatched = true;
          return MatchingResult.success(new SavedDoc(record, fieldValue));
        }
      } catch (PathNotFoundException e) {
        if (condition.isRequired()) {
          errors.add(MatchingError.missingField(condition.getField()));
        }
      }
    }
    return anyMatched ? MatchingResult.success() : MatchingResult.withErrors(errors);
  }
}
