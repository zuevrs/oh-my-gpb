package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.header;

import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.MatchingError;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.last.MatchCondition;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MatchingResult;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MessageMatcher;

public class HeaderAnyPositiveMatcher implements MessageMatcher {
  List<MatchingError> errors = new ArrayList<>();

  @Override
  public <T extends MatchCondition> MatchingResult matches(
      ConsumerRecord<?, ?> record, List<T> conditions) {
    return null;
  }
}
