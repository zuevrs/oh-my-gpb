package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.processors;

import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.SoftAssertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.api.AkitaScenario;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.MessageType;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.ErrorReporter;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.KafkaErrorReporter;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors.MatchingError;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.last.MatchCondition;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MatchStrategy;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MatchingResult;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.registry.MatcherRegistry;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.MessageValidator;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.ValidationException;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.ValidatorFactory;

/**
 * Интерфейс для обработки сообщений Kafka. Реализации этого интерфейса могут выполнять валидацию,
 * матчинг и другие операции над сообщением.
 */
@FunctionalInterface
public interface MessageProcessor extends Processor {

  /**
   * Обрабатывает одно сообщение Kafka.
   *
   * @param record Сообщение Kafka
   * @return true, если сообщение прошло все проверки (валидация и матчинг успешны), false — иначе
   * @throws RuntimeException если для NEGATIVE_ALL или NEGATIVE_ANY обнаружен FIELD_MATCH или
   *     ALL_FIELDS_MATCH и failFastOnNegativeMatch=true
   */
  boolean process(ConsumerRecord<?, ?> record);

  /**
   * Фабричный метод для создания процессора с валидацией и матчингом.
   *
   * @param messageType Тип сообщения (JSON, XML, TEXT)
   * @param ignoreValidationErrors Если true, ошибки валидации только логируются, но не добавляются
   *     в SoftAssertions
   * @param softly SoftAssertions для сбора ошибок (опционально, может быть null для жестких
   *     проверок)
   * @param matchParams Параметры для матчинга (Map<fieldPath, expectedValue>). Если null, матчинг
   *     пропускается
   * @param matchStrategy Стратегия матчинга (POSITIVE_ALL, POSITIVE_ANY, NEGATIVE_ALL,
   *     NEGATIVE_ANY). Если null, матчинг пропускается
   * @param failFastOnNegativeMatch Если true, при NEGATIVE_ALL или NEGATIVE_ANY и
   *     FIELD_MATCH/ALL_FIELDS_MATCH бросается исключение
   * @return MessageProcessor
   */
  static MessageProcessor validatingAndMatchingProcessor(
      MessageType messageType,
      boolean ignoreValidationErrors,
      SoftAssertions softly,
      List<? extends MatchCondition> matchParams,
      MatchStrategy matchStrategy,
      boolean failFastOnNegativeMatch,
      String varName) {

    Logger log = LoggerFactory.getLogger(MessageProcessor.class);
    MessageValidator validator = ValidatorFactory.getValidator(messageType);
    ErrorReporter reporter = softly != null ? new KafkaErrorReporter(softly) : null;

    return record -> {
      var scenario = AkitaScenario.getInstance();
      // 1. Валидация
      boolean isValid = true;
      try {
        validator.validate(record.value().toString());
        scenario.log(
            String.format(
                "Сообщение валидно: offset=%s, partition=%s", record.offset(), record.partition()));
      } catch (ValidationException e) {
        isValid = false;
        String errorMessage =
            String.format("Ошибка валидации сообщения: offset=%s", record.offset());
        scenario.log(errorMessage);
        if (!ignoreValidationErrors) {
          if (reporter != null) {
            reporter.reportValidationError(record, messageType.name(), errorMessage);
          } else {
            throw new RuntimeException(errorMessage, e);
          }
        }
        return isValid; // Пропускаем матчинг, если валидация провалилась
      }

      // 2. Матчинг (только если валидация прошла и параметры заданы)
      boolean isMatched = false;
      if (matchParams != null && matchStrategy != null) {
        MatchingResult result =
            MatcherRegistry.getMatcher(messageType, matchStrategy).matches(record, matchParams);
        if (result.isSuccess()) {
          isMatched = true;
          // Для POSITIVE_ALL и POSITIVE_ANY не репортим как ошибку
          if ((matchStrategy == MatchStrategy.POSITIVE_ALL
                  || matchStrategy == MatchStrategy.POSITIVE_ANY)
              && reporter != null) {
            scenario.log(
                String.format(
                    "Матчинг успешен для offset=%s: strategy=%s", record.offset(), matchStrategy));
            scenario.log(String.format("Все или часть полей совпали: %s", matchParams));
            if (varName != null) {
              scenario.log("Завершаем шаг и сохраняем заголовок в переменную");
              scenario.setVar(varName, record.value());
            }
          }
          if ((matchStrategy == MatchStrategy.NEGATIVE_ALL
                  || matchStrategy == MatchStrategy.NEGATIVE_ANY)
              && reporter != null) {
            scenario.log(
                String.format(
                    "Матчинг не успешен для offset=%s: strategy=%s",
                    record.offset(), matchStrategy));
            return false;
          }
        } else {
          if (reporter != null) {
            for (MatchingError error : result.getErrors()) {
              switch (error.getType()) {
                case MISSING_FIELD:
                  reporter.reportMissingField(record, error.getField(), error.getMessage());
                  break;
                case FIELD_MISMATCH:
                  reporter.reportMismatchField(
                      record,
                      error.getField(),
                      error.getActual(),
                      error.getExpected(),
                      error.getMessage());
                  break;
                case FIELD_MATCH:
                  // Репортируем FIELD_MATCH только для NEGATIVE_ALL и NEGATIVE_ANY
                  if (matchStrategy == MatchStrategy.NEGATIVE_ALL
                      || matchStrategy == MatchStrategy.NEGATIVE_ANY) {
                    reporter.reportMatchField(
                        record,
                        error.getField(),
                        error.getActual(),
                        error.getExpected(),
                        error.getMessage());
                    if (failFastOnNegativeMatch) {
                      throw new RuntimeException(
                          "Нежелательное совпадение поля: " + error.getMessage());
                    }
                  }
                  break;
                //                                    ALL_FIELDS_MATCH пока не реализован
                case ALL_FIELDS_MATCH:
                  // Репортируем ALL_FIELDS_MATCH только для NEGATIVE_ALL и NEGATIVE_ANY
                  if (matchStrategy == MatchStrategy.NEGATIVE_ALL
                      || matchStrategy == MatchStrategy.NEGATIVE_ANY) {
                    reporter.reportMatchField(
                        record, null, error.getFields(), error.getFields(), error.getMessage());
                    if (failFastOnNegativeMatch) {
                      throw new RuntimeException(
                          "Нежелательное совпадение всех полей: " + error.getMessage());
                    }
                  }
                  break;
                case FORMAT_ERROR:
                  reporter.reportValidationError(record, messageType.name(), error.getMessage());
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
      return isValid && isMatched;
    };
  }
}
