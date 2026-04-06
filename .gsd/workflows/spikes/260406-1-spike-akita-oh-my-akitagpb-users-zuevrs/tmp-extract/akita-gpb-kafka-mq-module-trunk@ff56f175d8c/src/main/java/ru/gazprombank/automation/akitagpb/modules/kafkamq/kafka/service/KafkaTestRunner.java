package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.api.AkitaScenario;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.processors.Processor;

@Slf4j
public class KafkaTestRunner {
  private final Processor messageProcessor;
  //    private final long partitionAssignmentTimeoutMs;
  private final KafkaConsumer<Object, Object> consumer;
  private final String topicName;
  private final boolean failIfNoMessages;
  private final boolean failIfNoMatches;
  private Set<TopicPartition> assignedPartitions;
  private final AkitaScenario akitaScenario = AkitaScenario.getInstance();
  private final Logger logger = LogManager.getLogger(getClass());
  final long partitionAssignmentTimeoutMsNew = 500;

  public KafkaTestRunner(
      KafkaConsumer<Object, Object> consumer,
      String topicName,
      Processor messageProcessor,
      boolean failIfNoMessages,
      boolean failIfNoMatches) {
    this.consumer = consumer;
    this.topicName = topicName;
    this.messageProcessor = messageProcessor;
    this.failIfNoMessages = failIfNoMessages;
    this.failIfNoMatches = failIfNoMatches;
  }

  public Set<TopicPartition> tryAutomaticPartitionAssignment() {
    var a = LogManager.getLogger(getClass());
    Set<TopicPartition> assignedPartitions = new HashSet<>();
    long endPollingTimestamp = System.currentTimeMillis() + partitionAssignmentTimeoutMsNew;
    consumer.subscribe(
        Collections.singletonList(topicName),
        new ConsumerRebalanceListener() {
          @Override
          public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            // Логика при отзыве партиций
          }

          @Override
          public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            assignedPartitions.addAll(partitions);
            akitaScenario.log("Партиции назначены автоматически: " + partitions);
          }
        });
    // Ожидаем назначения партиций
    while (System.currentTimeMillis() < endPollingTimestamp && consumer.assignment().isEmpty()) {
      consumer.poll(Duration.ZERO);
    }
    if (consumer.assignment().isEmpty()) {
      akitaScenario.log(
          String.format(
              "Автоматическое назначение партиций не удалось за %sмс",
              partitionAssignmentTimeoutMsNew));
    }
    this.assignedPartitions = assignedPartitions;
    return assignedPartitions;
  }

  public void tryPartitionAssignment() {
    if (this.tryAutomaticPartitionAssignment().isEmpty()) {
      this.performManualPartitionAssignment();
    }
  }

  public Set<TopicPartition> performManualPartitionAssignment() {
    akitaScenario.log("Выполняем ручное назначение партиций для топика: " + topicName);
    consumer.unsubscribe();
    List<PartitionInfo> partitions = consumer.partitionsFor(topicName);
    Set<TopicPartition> topicPartitions =
        partitions.stream()
            .map(p -> new TopicPartition(p.topic(), p.partition()))
            .collect(Collectors.toSet());
    consumer.assign(topicPartitions);
    akitaScenario.log("Ручное назначение завершено: " + partitions);
    this.assignedPartitions = topicPartitions;
    return topicPartitions;
  }

  public void seekToTestStartTime(long testStartTime) {
    Map<TopicPartition, Long> timestamps = new HashMap<>();
    assignedPartitions.forEach(topicPartition -> timestamps.put(topicPartition, testStartTime));
    Map<TopicPartition, OffsetAndTimestamp> offsetAndTimestampMap =
        consumer.offsetsForTimes(timestamps);
    offsetAndTimestampMap.forEach(
        (topicPartition, offsetAndTimestamp) -> {
          if (offsetAndTimestamp != null) {
            consumer.seek(topicPartition, offsetAndTimestamp.offset());
          }
        });
  }

  public ConsumerRecord<Object, Object> runPolling(long pollingTimeoutMs, long testStartTime) {
    ConsumerRecord<Object, Object> matchedRecord = null;
    long endTime = System.currentTimeMillis() + pollingTimeoutMs;
    int processedMessages = 0; // Счётчик обработанных сообщений
    seekToTestStartTime(testStartTime);
    while (System.currentTimeMillis() < endTime) {
      try {
        ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofMillis(500));
        for (ConsumerRecord<Object, Object> record : records) {
          if (record.timestamp() >= testStartTime) {
            processedMessages++; // Увеличиваем счётчик
            try {
              if (messageProcessor.process(record)) {
                matchedRecord = record;
                akitaScenario.log(
                    String.format("Найдено подходящее сообщение: offset={%s}", record.offset()));
                return matchedRecord; // Прерываем polling при успехе
              }
            } catch (RuntimeException e) {
              System.err.println(e.getMessage());
              akitaScenario.log(
                  String.format(
                      "Ошибка обработки записи: offset={%s}. Прерываем polling.", record.offset()));
              throw new AssertionError(e.getMessage());
              //                            return null; // Прерываем при ошибке (например,
              // FIELD_MATCH в NEGATIVE_ALL/NEGATIVE_ANY)
            }
          }
        }
      } catch (Exception e) {
        consumer.close();
        System.err.println(e.getMessage());
        log.error("Ошибка во время poll", e);
        break;
      }
    }
    if (processedMessages == 0 && failIfNoMessages) {
      var errorMessage =
          String.format(
              "В топике %s не было новых сообщений с момента начала сценария %s",
              topicName, testStartTime);
      throw new RuntimeException(errorMessage);
    }

    if (matchedRecord == null && failIfNoMatches) {
      String errorMessage = "Нет ни одного матчинга за период polling";
      throw new RuntimeException(errorMessage);
    }

    akitaScenario.log(
        String.format(
            "Polling завершён, подходящее сообщение: %s, обработано сообщений: %s",
            matchedRecord != null ? "найдено" : "не найдено", processedMessages));
    return matchedRecord;
  }

  public ConsumerRecord<Object, Object> pollLastMessage(long pollingTimeoutMs, long testStartTime) {
    ConsumerRecord<Object, Object> latestRecord = null;
    long latestOffset = -1;
    int processedMessages = 0;

    try {
      //            tryPartitionAssignment(); // Назначаем партиции
      long endTime = System.currentTimeMillis() + pollingTimeoutMs;

      while (System.currentTimeMillis() < endTime) {
        ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofMillis(500));
        for (ConsumerRecord<Object, Object> record : records) {
          if (record.timestamp() >= testStartTime) {
            processedMessages++;
            if (record.offset() > latestOffset) {
              latestOffset = record.offset();
              latestRecord = record;
            }
          }
        }
      }

      if (processedMessages == 0 && failIfNoMessages) {
        String errorMessage =
            String.format(
                "В топике %s не было новых сообщений с момента начала сценария %s",
                topicName, testStartTime);
        throw new RuntimeException(errorMessage);
      }

      if (latestRecord != null) {
        messageProcessor.process(latestRecord);
        akitaScenario.log(
            String.format(
                "Найдено последнее сообщение: topic=%s, partition=%s, offset=%s",
                latestRecord.topic(), latestRecord.partition(), latestRecord.offset()));
      } else if (failIfNoMatches) {
        String errorMessage =
            String.format("Нет сообщений в топике %s за период polling", topicName);
        throw new RuntimeException(errorMessage);
      }

      return latestRecord;
    } catch (Exception e) {
      log.error("Ошибка при опросе сообщений в Kafka: {}", e.getMessage(), e);
      throw new RuntimeException("Ошибка при получении последнего сообщения: " + e.getMessage(), e);
    }
  }
}
