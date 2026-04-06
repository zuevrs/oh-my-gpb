package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface ErrorReporter {
  void reportMissingField(ConsumerRecord<?, ?> record, String fieldPath, String errorMessage);

  void reportMismatchField(
      ConsumerRecord<?, ?> record,
      String fieldPath,
      Object actual,
      Object expected,
      String errorMessage);

  void reportMatchField(
      ConsumerRecord<?, ?> record,
      String fieldPath,
      Object actual,
      Object expected,
      String errorMessage);

  void reportValidationError(ConsumerRecord<?, ?> record, String format, String errorMessage);
}
