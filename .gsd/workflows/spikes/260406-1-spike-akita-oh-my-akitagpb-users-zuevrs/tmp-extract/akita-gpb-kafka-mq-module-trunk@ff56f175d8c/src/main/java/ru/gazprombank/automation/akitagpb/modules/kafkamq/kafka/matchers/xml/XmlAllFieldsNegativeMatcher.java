package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.xml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.XMLHelper;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.MatchingError;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.last.MatchCondition;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MatchingResult;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MessageMatcher;

public class XmlAllFieldsNegativeMatcher implements MessageMatcher {
  private final List<MatchingError> errors = new ArrayList<>();

  @Override
  public <T extends MatchCondition> MatchingResult matches(
      ConsumerRecord<?, ?> record, List<T> conditions) {
    int countOfMatches = 0;
    var document =
        XMLHelper.parseXml(
            new ByteArrayInputStream(record.value().toString().getBytes(StandardCharsets.UTF_8)));

    for (var condition : conditions) {
      try {
        String fieldValue =
            XMLHelper.findNodeByXpath(document, condition.getField())
                .getFirstChild()
                .getNodeValue();
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
      } catch (Exception e) {
        if (!condition.isRequired()) {
          countOfMatches++;
        } else {
          errors.add(MatchingError.missingField(condition.getField()));
        }
      }
    }

    if (countOfMatches == conditions.size()) {
      return MatchingResult.withErrors(errors);
    }
    return MatchingResult.success();
  }
}
