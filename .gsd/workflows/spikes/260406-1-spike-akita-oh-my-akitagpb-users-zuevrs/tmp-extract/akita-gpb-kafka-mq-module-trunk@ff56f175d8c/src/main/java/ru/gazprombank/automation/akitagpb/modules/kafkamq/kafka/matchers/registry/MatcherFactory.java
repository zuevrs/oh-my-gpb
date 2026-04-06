package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.registry;

import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.MessageType;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MatchStrategy;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MessageMatcher;

public class MatcherFactory {
  public static MessageMatcher getMatcher(MessageType messageType, MatchStrategy strategy) {
    return MatcherRegistry.getMatcher(messageType, strategy);
  }

  public static MessageMatcher getHeaderMatcher(MatchStrategy strategy) {
    return MatcherRegistry.getMatcher(MessageType.HEADER, strategy);
  }
}
