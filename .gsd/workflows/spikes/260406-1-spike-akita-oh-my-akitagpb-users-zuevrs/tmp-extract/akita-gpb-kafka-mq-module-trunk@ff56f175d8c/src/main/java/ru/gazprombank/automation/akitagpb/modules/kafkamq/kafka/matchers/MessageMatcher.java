package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers;

import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.last.MatchCondition;

public interface MessageMatcher {

  <T extends MatchCondition> MatchingResult matches(
      ConsumerRecord<?, ?> record, List<T> conditions);
}
