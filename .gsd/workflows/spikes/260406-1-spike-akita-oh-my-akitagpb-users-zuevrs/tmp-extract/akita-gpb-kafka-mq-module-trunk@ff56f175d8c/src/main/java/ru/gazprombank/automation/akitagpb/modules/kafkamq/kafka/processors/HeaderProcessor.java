package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.processors;

import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.SoftAssertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.api.AkitaScenario;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.MessageType;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.SaveType;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.ErrorReporter;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.KafkaErrorReporter;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.MatchingError;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.last.MatchCondition;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MatchStrategy;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.registry.MatcherRegistry;

/** Интерфейс для обработки заголовков сообщения Kafka. */
@FunctionalInterface
public interface HeaderProcessor extends Processor {

  /**
   * Обрабатывает заголовки сообщения Kafka.
   *
   * @param record Сообщение Kafka
   * @return true, если заголовки прошли матчинг, false — иначе
   */
  boolean process(ConsumerRecord<?, ?> record);

  /**
   * Фабричный метод для создания процессора заголовков с матчингом.
   *
   * @param matchParams Параметры для матчинга (Map<fieldPath, expectedValue>) с префиксом header.
   * @param matchStrategy Стратегия матчинга (POSITIVE_ALL, POSITIVE_ANY, NEGATIVE_ALL,
   *     NEGATIVE_ANY)
   * @param varName Имя переменной для сохранения в AkitaScenario
   * @return HeaderProcessor
   */
  static HeaderProcessor matchingProcessor(
      MessageType messageType,
      List<MatchCondition> matchParams,
      SoftAssertions softly,
      MatchStrategy matchStrategy,
      SaveType saveType,
      String varName) {

    Logger log = LoggerFactory.getLogger(HeaderProcessor.class);
    ErrorReporter reporter = softly != null ? new KafkaErrorReporter(softly) : null;

    return record -> {
      var scenario = AkitaScenario.getInstance();
      // 2. Матчинг
      boolean isMatched = false;
      if (matchParams != null && matchStrategy != null) {
        var result =
            MatcherRegistry.getMatcher(messageType, matchStrategy).matches(record, matchParams);
        //             MatchingResult result =
        // MatcherFactory.getHeaderMatcher(matchStrategy).matches(record, matchParams);
        if (result.isSuccess()) {
          isMatched = true;
          // Для POSITIVE_ALL и POSITIVE_ANY не репортим как ошибку
          if ((matchStrategy == MatchStrategy.POSITIVE_ALL
                  || matchStrategy == MatchStrategy.POSITIVE_ANY
                  || matchStrategy == MatchStrategy.CUSTOM_HEADER)
              && reporter != null) {
            scenario.log(
                String.format(
                    "Матчинг успешен для offset=%s: strategy=%s", record.offset(), matchStrategy));
            scenario.log(String.format("Все или часть полей совпали: %s", matchParams));
            if (varName != null) {
              scenario.log("Завершаем шаг и сохраняем заголовок в переменную");
              if (saveType == SaveType.HEADER) {
                System.out.println();
                scenario.setVar(varName, result.getSavedDoc().lastHeader());
                scenario.log(result.getSavedDoc().lastHeader());
              } else if (saveType == SaveType.BODY) {
                scenario.setVar(varName, result.getSavedDoc().record().value());
                scenario.log(result.getSavedDoc().record().value());
              } else if (saveType == SaveType.HEADER_UUID) {
                var uuidHeader =
                    UUID.nameUUIDFromBytes(result.getSavedDoc().lastHeader().getBytes());
                scenario.setVar(varName, uuidHeader);
                scenario.log(uuidHeader);
              }
            }
          }
        } else {
          if (reporter != null) {
            for (MatchingError error : result.getErrors()) {
              switch (error.getType()) {
                case MISSING_HEADER:
                  reporter.reportMissingField(record, error.getField(), error.getMessage());
                  break;
                case HEADER_MISMATCH:
                  reporter.reportMismatchField(
                      record,
                      error.getField(),
                      error.getActual(),
                      error.getExpected(),
                      error.getMessage());
                  break;
                default:
                  log.warn("Необработанный тип ошибки: {}", error.getType());
              }
            }
          }
          log.warn("Матчинг провален для offset={}: strategy={}", record.offset(), matchStrategy);
        }
      }

      // Возвращаем true только если валидация прошла (или игнорируется) и матчинг успешен
      return isMatched;
    };
  }
}
