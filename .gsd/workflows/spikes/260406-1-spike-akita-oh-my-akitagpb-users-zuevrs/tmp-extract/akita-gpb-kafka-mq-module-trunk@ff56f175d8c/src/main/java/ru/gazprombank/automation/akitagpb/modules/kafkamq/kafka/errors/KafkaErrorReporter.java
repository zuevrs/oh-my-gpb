package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.errors;

import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.SoftAssertions;

public class KafkaErrorReporter implements ErrorReporter {
  @Getter private final SoftAssertions softly;

  //    public KafkaErrorReporter(ConsumerRecord<?, ?> record, SoftAssertions softly) {
  //        this.currentRecord = record;
  //        this.softly = softly;
  //    }
  public KafkaErrorReporter(SoftAssertions softly) {
    this.softly = softly;
  }

  @Override
  public void reportMissingField(
      ConsumerRecord<?, ?> record, String fieldPath, String errorMessage) {
    var missingFieldError =
        String.format(
            "feature поле %s у сообщения с офсетом %d отсутствует в сообщении kafka",
            fieldPath, record.offset());
    softly.collectAssertionError(
        new AssertionError(
            String.format(
                "Missing field [offset:%d, partition:%d]%nField: %s%nОшибка: %s%nMessage content:%n%s",
                record.offset(),
                record.partition(),
                fieldPath,
                missingFieldError,
                record.value())));
  }

  @Override
  public void reportMismatchField(
      ConsumerRecord<?, ?> record,
      String fieldPath,
      Object actual,
      Object expected,
      String errorMessage) {
    var mismatchFieldError =
        String.format(
            "значение feature поля %s сообщения с офсетом %d не равно значению поля kafka",
            fieldPath, record.offset());
    softly.collectAssertionError(
        new AssertionError(
            String.format(
                "Mismatch field [Офсет:%d, Партиция:%d]%nFeature поле: %s%nАктуальное значение kafka поля: %s%n"
                    + "Ожидаемое значение feature поля: %s%nОшибка: %s%nMessage content:%n%s",
                record.offset(),
                record.partition(),
                fieldPath,
                actual,
                expected,
                mismatchFieldError,
                record.value())));
  }

  @Override
  public void reportMatchField(
      ConsumerRecord<?, ?> record,
      String fieldPath,
      Object actual,
      Object expected,
      String errorMessage) {
    var matchFieldError =
        String.format(
            "значение feature поля %s сообщения с офсетом %d равно значению поля kafka",
            fieldPath, record.offset());
    softly.collectAssertionError(
        new AssertionError(
            String.format(
                "Match field [Офсет:%d, Партиция:%d]%nFeature поле: %s%nАктуальное значение kafka поля: %s%n"
                    + "Ожидаемое значение feature поля: %s%nОшибка: %s%nMessage content:%n%s",
                record.offset(),
                record.partition(),
                fieldPath,
                actual,
                expected,
                matchFieldError,
                record.value())));
  }

  @Override
  public void reportValidationError(
      ConsumerRecord<?, ?> record, String format, String errorMessage) {
    var error =
        String.format(
            "Сообщение с офсетом %s не соответствует формату %s", record.offset(), format);
    softly.collectAssertionError(new AssertionError(errorMessage));
  }
}
