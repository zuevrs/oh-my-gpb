package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.processors;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/** Общий интерфейс для обработки сообщений Kafka. */
@FunctionalInterface
public interface Processor {

  /**
   * Обрабатывает сообщение Kafka.
   *
   * @param record Сообщение Kafka
   * @return true, если сообщение прошло все проверки (валидация и матчинг), false — иначе
   */
  boolean process(ConsumerRecord<?, ?> record);
}
