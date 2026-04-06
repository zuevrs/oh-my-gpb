package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.registry;

import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.MessageType;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MatchStrategy;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.header.CustomHeaderMatcher;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.header.HeaderAnyPositiveMatcher;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.json.*;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.xml.*;

public class MatcherRegistryInitializer {
  public static void initialize() {
    // JSON матчеры
    MatcherRegistry.register(
        MessageType.JSON, MatchStrategy.POSITIVE_ANY, JsonAnyFieldPositiveMatcher::new);
    MatcherRegistry.register(
        MessageType.JSON, MatchStrategy.POSITIVE_ALL, JsonAllFieldsPositiveMatcher::new);
    MatcherRegistry.register(
        MessageType.JSON, MatchStrategy.NEGATIVE_ANY, JsonAnyFieldNegativeMatcher::new);
    MatcherRegistry.register(
        MessageType.JSON, MatchStrategy.NEGATIVE_ALL, JsonAllFieldsNegativeMatcher::new);

    // XML матчеры
    MatcherRegistry.register(
        MessageType.XML, MatchStrategy.POSITIVE_ANY, XmlAnyFieldPositiveMatcher::new);
    MatcherRegistry.register(
        MessageType.XML, MatchStrategy.POSITIVE_ALL, XmlAllFieldsPositiveMatcher::new);
    MatcherRegistry.register(
        MessageType.XML, MatchStrategy.NEGATIVE_ANY, XmlAnyFieldNegativeMatcher::new);
    MatcherRegistry.register(
        MessageType.XML, MatchStrategy.NEGATIVE_ALL, XmlAllFieldsNegativeMatcher::new);

    // Header матчеры
    MatcherRegistry.register(
        MessageType.HEADER, MatchStrategy.POSITIVE_ANY, HeaderAnyPositiveMatcher::new);
    MatcherRegistry.register(
        MessageType.HEADER, MatchStrategy.CUSTOM_HEADER, CustomHeaderMatcher::new);
  }
}
