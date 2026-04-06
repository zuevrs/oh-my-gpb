package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.json;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.MatchingError;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.last.MatchCondition;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MatchingResult;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MessageMatcher;

public class JsonAnyFieldNegativeMatcher implements MessageMatcher {
  private final List<MatchingError> errors = new ArrayList<>();

  @Override
  public <T extends MatchCondition> MatchingResult matches(
      ConsumerRecord<?, ?> record, List<T> conditions) {
    int countOfMatches = 0;

    for (var condition : conditions) {
      try {
        String fieldValue = JsonPath.read(record.value().toString(), condition.getField());
        boolean anyConditionMatched = false;

        for (MatchCondition.Condition subCondition : condition.getConditions()) {
          if (condition.checkCondition(fieldValue, subCondition)) {
            errors.add(
                MatchingError.fieldMatch(
                    condition.getField(),
                    subCondition.getType() + ": " + subCondition.getValue(),
                    fieldValue));
            anyConditionMatched = true;
          }
        }

        if (anyConditionMatched) {
          countOfMatches++;
        }
      } catch (PathNotFoundException e) {
        if (!condition.isRequired()) {
          countOfMatches++; // Отсутствие поля считается совпадением для negative
        }
      }
    }

    if (countOfMatches > 0) {
      return MatchingResult.withErrors(errors);
    }
    return MatchingResult.success();
  }
}
