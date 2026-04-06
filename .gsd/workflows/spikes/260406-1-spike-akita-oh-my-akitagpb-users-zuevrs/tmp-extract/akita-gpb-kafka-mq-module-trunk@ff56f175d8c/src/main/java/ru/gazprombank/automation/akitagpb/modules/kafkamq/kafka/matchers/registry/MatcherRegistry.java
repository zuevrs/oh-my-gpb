package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.MessageType;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MatchStrategy;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MessageMatcher;

public class MatcherRegistry {
  // Хранилище матчеров: MessageType -> MatchStrategy -> MessageMatcher
  private static final Map<MessageType, Map<MatchStrategy, Supplier<MessageMatcher>>> matchers =
      new HashMap<>();

  /**
   * Регистрирует матчер для комбинации типа сообщения и стратегии.
   *
   * @param type Тип сообщения (JSON, XML, AVRO, etc.)
   * @param strategy Стратегия матчинга (POSITIVE_ALL, POSITIVE_ANY, etc.)
   * @param matcherSupplier Поставщик экземпляра матчера
   */
  public static void register(
      MessageType type, MatchStrategy strategy, Supplier<MessageMatcher> matcherSupplier) {
    matchers.computeIfAbsent(type, k -> new HashMap<>()).put(strategy, matcherSupplier);
  }

  /**
   * Получает матчер для указанного типа сообщения и стратегии.
   *
   * @param type Тип сообщения
   * @param strategy Стратегия матчинга
   * @return Экземпляр MessageMatcher
   * @throws IllegalArgumentException если матчер не найден
   */
  public static MessageMatcher getMatcher(MessageType type, MatchStrategy strategy) {
    Map<MatchStrategy, Supplier<MessageMatcher>> strategyMap = matchers.get(type);

    if (strategyMap == null) {
      throw new IllegalArgumentException("No matchers registered for type: " + type);
    }
    Supplier<MessageMatcher> matcherSupplier = strategyMap.get(strategy);
    if (matcherSupplier == null) {
      throw new IllegalArgumentException(
          "No matcher registered for type: " + type + ", strategy: " + strategy);
    }
    return matcherSupplier.get();
  }

  /** Очищает реестр (для тестов или переинициализации). */
  public static void clear() {
    matchers.clear();
  }
}
