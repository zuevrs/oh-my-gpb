package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.xml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.XMLHelper;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.SavedDoc;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.MatchingError;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.last.MatchCondition;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MatchingResult;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MessageMatcher;

public class XmlAllFieldsPositiveMatcher implements MessageMatcher {
  private final List<MatchingError> errors = new ArrayList<>();

  @Override
  public <T extends MatchCondition> MatchingResult matches(
      ConsumerRecord<?, ?> record, List<T> conditions) {
    var document =
        XMLHelper.parseXml(
            new ByteArrayInputStream(record.value().toString().getBytes(StandardCharsets.UTF_8)));
    boolean allConditionsMatched = true;

    for (var condition : conditions) {
      try {
        String fieldValue =
            XMLHelper.findNodeByXpath(document, condition.getField())
                .getFirstChild()
                .getNodeValue();
        boolean conditionMatched = true;

        for (MatchCondition.Condition subCondition : condition.getConditions()) {
          if (!condition.checkCondition(fieldValue, subCondition)) {
            errors.add(
                MatchingError.fieldMismatch(
                    condition.getField(),
                    subCondition.getType() + ": " + subCondition.getValue(),
                    fieldValue));
            conditionMatched = false;
          }
        }

        if (!conditionMatched) {
          allConditionsMatched = false;
        }
      } catch (Exception e) {
        if (condition.isRequired()) {
          errors.add(MatchingError.missingField(condition.getField()));
          allConditionsMatched = false;
        }
      }
    }

    if (allConditionsMatched) {
      return MatchingResult.success(new SavedDoc(record, null));
    }
    return MatchingResult.withErrors(errors);
  }
}
