package ru.gazprombank.automation.akitagpb.modules.kafkamq.steps.kafka;

import static java.util.Optional.ofNullable;
import static ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables.resolveVars;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.И;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.PropertyLoader;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.config.KafkaProperties;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.Helpers;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.KafkaRequestParam;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.MessageType;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.last.JsonMatchCondition;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.MatchStrategy;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.processors.MessageProcessor;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.service.KafkaTestRunner;

/** Шаги для тестирования Kafka, доступные по умолчанию в каждом новом проекте */
@Slf4j
public class KafkaSteps extends BaseMethods {

  long time = new Date().getTime();
  long testTime = new Date().getTime();
  private static final Properties kafkaProperties = new KafkaProperties().toProperties();

  /**
   * Парсит DataTable в список карт, где каждая карта содержит JSONPath (featureName) и ожидаемое
   * значение (featureValue). Разрешает переменные в значениях с использованием ScopedVariables и
   * BaseMethods.
   *
   * @param dataTable DataTable для парсинга
   * @return Список карт с парсингом JSONPath и ожидаемыми значениями
   */
  private List<Map<String, String>> parseDataTable(DataTable dataTable) {
    return dataTable.cells().stream()
        .map(
            it -> {
              String jsonPath = ScopedVariables.resolveVars(it.get(0));
              var expectedValue = String.valueOf(it.get(1));
              expectedValue =
                  BaseMethods.getInstance().getPropertyOrStringVariableOrValue(expectedValue);
              expectedValue = ScopedVariables.resolveVars(expectedValue);
              return Map.of(
                  "featureName", jsonPath,
                  "featureValue", expectedValue);
            })
        .collect(Collectors.toList());
  }

  /**
   * Генерирует случайный UUID (без дефисов) и сохраняет его в сценарийную переменную с указанным
   * именем. Это полезно для создания уникальных идентификаторов в тестах.
   *
   * @param name Имя переменной для сохранения UUID
   */
  @И("^сгенерировать UUID и сохранить в переменную \"(.*)\"$")
  public void generateUuidAndSave(String name) {
    akitaScenario.setVar(name, UUID.randomUUID().toString().replaceAll("-", ""));
  }

  /**
   * Устанавливает подключение к Kafka и отправляет сообщение в указанный топик с параметрами из
   * таблицы. Поддерживает различные типы параметров: BODY (тело сообщения), TOPIC (имя топика), KEY
   * (ключ сообщения), HEADER (заголовок как строка), HEADER_LONG (заголовок как long), HEADER_HEX
   * (заголовок как hex-строка). Значение времени отправки сохраняется в переменную "timestamp".
   * Параметры из таблицы разрешаются через переменные и свойства.
   *
   * @param kafkaRequestParams Список параметров KafkaRequestParam из DataTable
   * @throws Exception Если произошла ошибка при подключении или отправке
   */
  @И("^выполнено подключение к kafka и отправлено сообщение с параметрами из таблицы$")
  public void connectKafkaAndSendMessage(List<KafkaRequestParam> kafkaRequestParams)
      throws Exception {
    KafkaProducer<String, String> kafkaProducer = null;
    ProducerRecord record = null;
    time = new Date().getTime();

    try {
      kafkaProducer = new KafkaProducer<>(kafkaProperties);
      String body = null;
      String topic = null;
      String key = null;

      for (KafkaRequestParam kafkaRequestParam : kafkaRequestParams) {
        String name = kafkaRequestParam.getName();
        String value = kafkaRequestParam.getValue();
        switch (kafkaRequestParam.getType()) {
          case VAR:
            value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
            value = resolveVars(value);
            this.akitaScenario.setVar(name, value);
            break;
          case BODY:
            value =
                PropertyLoader.loadValueFromFileOrPropertyOrVariableOrDefault(resolveVars(value));
            body = resolveVars(value);
            break;
          case TOPIC:
            value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
            value = resolveVars(value);
            topic = value;
            break;
          case KEY:
            value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
            value = resolveVars(value);
            key = value;
            break;
        }
      }
      record = new ProducerRecord(topic, 0, time, key, body, null);
      for (KafkaRequestParam kafkaRequestParam : kafkaRequestParams) {
        String name = kafkaRequestParam.getName();
        String value = kafkaRequestParam.getValue();
        switch (kafkaRequestParam.getType()) {
          case HEADER:
            value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
            value = resolveVars(value);
            if (value.contains("\"HEX\"")) {
              byte[] bytes = Hex.decodeHex(value.replaceAll("\"HEX\"", "").toCharArray());
              record.headers().add(name, bytes);
              break;
            } else {
              record.headers().add(name, value.getBytes());
            }
            break;
          case HEADER_LONG:
            value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
            value = resolveVars(value);
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(Long.parseLong(value));
            record.headers().add(name, buffer.array());
            break;
          case HEADER_HEX:
            value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
            value = resolveVars(value);
            byte[] bytes = Hex.decodeHex(value.toCharArray());
            record.headers().add(name, bytes);
            break;
        }
      }

      Future<RecordMetadata> future = kafkaProducer.send(record);

    } catch (Exception e) {
      throw new Exception(e);
    } finally {
      kafkaProducer.close();
      akitaScenario.setVar("timestamp", time);
    }
  }

  /**
   * Устанавливает подключение к Kafka, получает сообщение из указанного топика и проверяет, что оно
   * соответствует значениям из таблицы (JSONPath и ожидаемые значения). Поиск начинается с времени
   * начала теста. Если совпадение найдено, шаг завершается досрочно. Если нет сообщений или нет
   * совпадений, выбрасывается AssertionError. Логирует время начала сценария и детали сообщений.
   *
   * @param topicName Имя топика для чтения
   * @param dataTable Таблица с парами JSONPath и ожидаемыми значениями
   * @throws Exception Если произошла ошибка при подключении или чтении
   */
  @И(
      "^выполнено подключение к kafka и полученно сообщение из топика \"(.*)\" равно значениям из таблицы$")
  public void connectKafkaAndGetMessage(String topicName, DataTable dataTable) throws Exception {
    var scenarioTime =
        Instant.ofEpochMilli(testTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
    akitaScenario.log("Время начала сценария: " + scenarioTime);
    var softAssertions = new SoftAssertions();
    final long timeoutMs = 2000;
    long endPollingTimestamp = System.currentTimeMillis() + timeoutMs;
    boolean partitionsAssigned;
    Set<TopicPartition> assignedPartitions = new HashSet<>();
    try (KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(kafkaProperties)) {
      kafkaConsumer.subscribe(Collections.singletonList(topicName));
      // 1. Дожидаемся назначения партиций
      while (System.currentTimeMillis() < endPollingTimestamp) {
        kafkaConsumer.poll(Duration.ZERO);
        assignedPartitions = kafkaConsumer.assignment();
        partitionsAssigned = !assignedPartitions.isEmpty();
        if (partitionsAssigned) {
          akitaScenario.log("Партиции назначены автоматически через метод subscribe");
          break;
        }
      }
      // 2. Переходим к ручному назначению партиций если в 1 шаге через subscribe не успело пройти
      // назначение
      if (kafkaConsumer.assignment().isEmpty()) {
        akitaScenario.log(
            String.format(
                "Партиции не назначены за отведенные %dмс. Переходим к ручному назначению%n",
                timeoutMs));
        kafkaConsumer.unsubscribe();
        var partitions = kafkaConsumer.partitionsFor(topicName);
        assignedPartitions =
            partitions.stream()
                .map(partition -> new TopicPartition(partition.topic(), partition.partition()))
                .collect(Collectors.toSet());
        kafkaConsumer.assign(assignedPartitions);
      }
      // 3. Делаем ручное смещение по времени начала тестов
      Map<TopicPartition, Long> timestamps = new HashMap<>();
      assignedPartitions.forEach(topicPartition -> timestamps.put(topicPartition, testTime));
      Map<TopicPartition, OffsetAndTimestamp> offsetAndTimestampMap =
          kafkaConsumer.offsetsForTimes(timestamps);
      offsetAndTimestampMap.forEach(
          (topicPartition, offsetAndTimestamp) -> {
            if (offsetAndTimestamp != null) {
              kafkaConsumer.seek(topicPartition, offsetAndTimestamp.offset());
            }
          });
      // 4. Основной цикл с досрочным выходом из степа если совпадение найдено
      var endTime = System.currentTimeMillis() + 2000;
      akitaScenario.log("Назначенные партиции: " + assignedPartitions);
      var countOfKafkaTotalMessages = 0;
      var endOffsets = kafkaConsumer.endOffsets(assignedPartitions);
      outer:
      while (System.currentTimeMillis() < endTime) {
        ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(100));
        countOfKafkaTotalMessages += records.count();
        akitaScenario.log(String.format("Из kafka считано записей: %d", records.count()));
        for (var kafkaDoc : records) {
          var currentPositionOffset =
              kafkaConsumer.position(new TopicPartition(topicName, kafkaDoc.partition()));
          var lastPartitionOffset =
              endOffsets.get(new TopicPartition(topicName, kafkaDoc.partition()));
          akitaScenario.log("Документ: " + kafkaDoc.value());
          // Сравниваем поля из сообщений кафки со всеми полями из feature
          var featuresDataTable = parseDataTable(dataTable);
          var countOfMatches = 0;
          for (var featureRow : featuresDataTable) {
            try {
              var jsonExtractedKafkaVal =
                  JsonPath.read(kafkaDoc.value(), featureRow.get("featureName")).toString();
              var error =
                  String.format(
                      "Значение feature поля %s сообщения с офсетом %d не равно значению поля kafka",
                      featureRow.get("featureName"), kafkaDoc.offset());
              Assertions.assertEquals(jsonExtractedKafkaVal, featureRow.get("featureValue"), error);
              countOfMatches++;
            } catch (AssertionFailedError error) {
              softAssertions.collectAssertionError(new AssertionError(error.getMessage()));
            } catch (PathNotFoundException exception) {
              var error =
                  String.format(
                      "feature поле %s у сообщения с офсетом %d отсутствует в сообщении kafka",
                      featureRow.get("featureName"), kafkaDoc.offset());
              softAssertions.collectAssertionError(new AssertionError(error));
            }
            /*В случае полного совпадения выходим досрочно из внешнего цикла в котором в теории может оказаться больше 1 сообщения kafka
            в ином случае заходим на новое сообщение и заново ищем совпадения*/
            if (countOfMatches == featuresDataTable.size()) {
              akitaScenario.log(
                  String.format(
                      "Значения полей feature из степа у сообщения с офсетом %d сошлись со всеми значениями полей kafka",
                      kafkaDoc.offset()));
              akitaScenario.log("Найденное сообщение: " + kafkaDoc.value());
              return;
            }
          }
          if (currentPositionOffset == lastPartitionOffset) {
            akitaScenario.log("В партиции больше нет сообщений");
            break outer;
          }
        }
      }
      if (countOfKafkaTotalMessages == 0) {
        var error =
            String.format(
                "В топике %s не было новых сообщений с момента начала сценария %s",
                topicName, scenarioTime);
        softAssertions.collectAssertionError(new AssertionError(error));
      }
      softAssertions.assertAll();
    } catch (Exception e) {
      throw new Exception(e);
    }
  }

  /**
   * Устанавливает подключение к Kafka, находит последнее сообщение в указанном топике и сохраняет
   * значение указанного заголовка в сценарийную переменную. Поддерживает формат UUID (использует
   * UUID.nameUUIDFromBytes для байтов). Поиск последнего сообщения основан на максимальном
   * timestamp.
   *
   * @param topicName Имя топика
   * @param header Имя заголовка
   * @param typeUUID Строка, указывающая на формат UUID (если содержит "UUID")
   * @param value Имя переменной для сохранения значения
   * @throws Exception Если произошла ошибка при подключении или чтении
   */
  @И(
      "^выполнено подключение к kafka и из последнего сообщения из топика \"(.*)\" header \"(.*)\" сохранен( в формате UUID)? в переменную \"(.*)\"")
  public void saveHeaderFromLastMassage(
      String topicName, String header, String typeUUID, String value) throws Exception {
    KafkaConsumer<String, String> consumer = null;
    try {
      consumer = new KafkaConsumer<>(kafkaProperties);
      consumer.subscribe(Collections.singletonList(topicName));
      consumer.poll(Duration.ofSeconds(10));

      AtomicLong maxTimestamp = new AtomicLong();
      AtomicReference<ConsumerRecord<String, String>> latestRecord = new AtomicReference<>();
      KafkaConsumer<String, String> finalConsumer = consumer;
      consumer
          .endOffsets(consumer.assignment())
          .forEach(
              (topicPartition, offset) -> {
                finalConsumer.seek(topicPartition, (offset == 0) ? offset : offset - 1);
                finalConsumer
                    .poll(Duration.ofSeconds(10))
                    .forEach(
                        record -> {
                          if (record.timestamp() > maxTimestamp.get()) {
                            maxTimestamp.set(record.timestamp());
                            latestRecord.set(record);
                          }
                        });
              });

      latestRecord
          .get()
          .headers()
          .headers(header)
          .forEach(
              header1 -> {
                if (typeUUID != null && typeUUID.contains("UUID")) {
                  akitaScenario.setVar(value, UUID.nameUUIDFromBytes(header1.value()).toString());
                  akitaScenario.log(UUID.nameUUIDFromBytes(header1.value()).toString());
                } else {
                  akitaScenario.setVar(value, new String(header1.value()));
                  akitaScenario.log(new String(header1.value()));
                }
              });

    } catch (Exception e) {
      throw new Exception(e);
    } finally {
      consumer.close();
    }
  }

  /**
   * Устанавливает подключение к Kafka, находит последнее сообщение в указанном топике и сохраняет
   * тело сообщения в сценарийную переменную. Поиск основан на максимальном timestamp с таймаутом 4
   * секунды.
   *
   * @param topicName Имя топика
   * @param value Имя переменной для сохранения тела сообщения
   * @throws Exception Если произошла ошибка при подключении или чтении
   */
  @И(
      "^выполнено подключение к kafka и из последнего сообщения из топика \"(.*)\" тело сообщения сохранено в переменную \"(.*)\"")
  public void saveValueFromLastMassage(String topicName, String value) throws Exception {
    final long timeoutMs = 4000;
    long endPollingTimestamp = System.currentTimeMillis() + timeoutMs;
    boolean partitionsAssigned;
    Set<TopicPartition> assignedPartitions = new HashSet<>();
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProperties); ) {
      consumer.subscribe(Collections.singletonList(topicName));
      // 1. Дожидаемся автоматического назначения партиций
      while (System.currentTimeMillis() < endPollingTimestamp) {
        consumer.poll(Duration.ZERO);
        assignedPartitions = consumer.assignment();
        partitionsAssigned = !assignedPartitions.isEmpty();
        if (partitionsAssigned) {
          akitaScenario.log("Партиции назначены автоматически через метод subscribe");
          break;
        }
      }
      // 2. Переходим к ручному назначению партиций
      if (assignedPartitions.isEmpty()) {
        akitaScenario.log(
            String.format(
                "партиции не назначены за отведенные %dмс.Переходим к ручному назначению%n",
                timeoutMs));
        consumer.unsubscribe();
        var partitions = consumer.partitionsFor(topicName);
        assignedPartitions =
            partitions.stream()
                .map(partition -> new TopicPartition(partition.topic(), partition.partition()))
                .collect(Collectors.toSet());
        consumer.assign(assignedPartitions);
      }
      akitaScenario.log(String.format("Назначенные партиции: %s", assignedPartitions));
      AtomicLong maxTimestamp = new AtomicLong();
      AtomicReference<ConsumerRecord<String, String>> latestRecord = new AtomicReference<>();
      akitaScenario.log(consumer.endOffsets(consumer.assignment()));
      consumer
          .endOffsets(consumer.assignment())
          .forEach(
              (topicPartition, offset) ->
                  consumer.seek(topicPartition, (offset == 0) ? offset : offset - 1));
      var endTime = System.currentTimeMillis() + timeoutMs;
      while (System.currentTimeMillis() < endTime) {
        for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
          if (record.timestamp() > maxTimestamp.get()) {
            akitaScenario.log(
                String.format(
                    "Офсет последнего документа после ручного смещения на -1: %d",
                    record.offset()));
            maxTimestamp.set(record.timestamp());
            latestRecord.set(record);
          }
        }
      }
      akitaScenario.setVar(value, latestRecord.get().value());
      akitaScenario.log(latestRecord.get().value());
    } catch (Exception e) {
      throw new Exception(e);
    }
  }

  @И(
      "^выполнено подключение к kafka и из сообщения из топика \"(.*)\" тело сообщения сохранено в переменную \"(.*)\" и соответствует параметрам из таблицы")
  public void saveValueFromLastMessageWithParams(
      String topicName, String varName, Map<String, String> params) {
    var conditions =
        params.entrySet().stream()
            .map(
                key ->
                    JsonMatchCondition.create()
                        .field(key.getKey())
                        .addExact(key.getValue())
                        .build())
            .toList();
    var scenarioTime =
        Instant.ofEpochMilli(testTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
    akitaScenario.log("Время начала сценария: " + scenarioTime);
    MessageType messageType = MessageType.JSON;
    var softAssertions = new SoftAssertions();
    KafkaConsumer<Object, Object> kafkaConsumer = new KafkaConsumer<>(kafkaProperties);
    var processor =
        MessageProcessor.validatingAndMatchingProcessor(
            messageType,
            false,
            softAssertions,
            conditions,
            MatchStrategy.POSITIVE_ALL,
            false,
            varName);
    var kafkaRunner = new KafkaTestRunner(kafkaConsumer, topicName, processor, true, false);
    kafkaRunner.tryPartitionAssignment();
    kafkaRunner.seekToTestStartTime(testTime);
    var record = kafkaRunner.runPolling(5000, testTime);
    if (record != null) {
      akitaScenario.log("Совпадение найдено. Завершаем шаг");
      return;
    }
    softAssertions.assertAll();
  }

  /**
   * Получает сообщение из указанного топика с типом (JSON, XML, TEXT), соответствующее параметрам
   * из таблицы, и сохраняет его в сценарийную переменную. Использует ValidatorFactory и
   * MatcherFactory для валидации и матчинга. Поиск начинается с времени начала теста, с досрочным
   * выходом при совпадении. Если нет совпадений, проверяет все ошибки.
   *
   * @param topicName Имя топика
   * @param messageTypeString Тип сообщения (JSON|XML|TEXT)
   * @param varName Имя переменной для сохранения сообщения
   * @param params Параметры для матчинга (из таблицы)
   */
  @И(
      "^получить сообщение из топика \"(.+)\", с типом (JSON|XML|TEXT) соответствующее параметрам из таблицы, и сохранить в переменную \"(.+)\"$")
  public void getMessageFromTopicAndSave(
      String topicName, String messageTypeString, String varName, Map<String, String> params) {
    MessageType messageType = MessageType.fromString(messageTypeString);
    // 1. Получаем время начала теста в миллисекундах
    var conditions = Helpers.convertMapToConditions(params);
    var scenarioTime =
        Instant.ofEpochMilli(testTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
    akitaScenario.log("Время начала сценария: " + scenarioTime);
    var softAssertions = new SoftAssertions();
    var kafkaConsumer = new KafkaConsumer<>(kafkaProperties);
    System.out.println(MessageType.fromString(messageTypeString));
    MessageProcessor messageProcessor =
        MessageProcessor.validatingAndMatchingProcessor(
            messageType,
            true,
            softAssertions,
            conditions,
            MatchStrategy.POSITIVE_ALL,
            true,
            varName);
    var kafkaRunner = new KafkaTestRunner(kafkaConsumer, topicName, messageProcessor, true, false);
    // 2. Если за отведенное время партиции назначены автоматически, то выходим из ожидания иначе
    // переход к ручному назначению
    kafkaRunner.tryPartitionAssignment();
    kafkaRunner.seekToTestStartTime(testTime);
    // 3. Основной цикл с досрочным выходом из степа если совпадение найдено
    var record = kafkaRunner.runPolling(5000, testTime);
    if (record != null) {
      akitaScenario.log("Совпадение найдено. Завершаем шаг и сохраняем документ в переменную");
      akitaScenario.setVar(varName, record);
      return;
    }
    softAssertions.assertAll();
  }

  /**
   * Проверяет отсутствие сообщения в указанном топике с типом (JSON, XML, TEXT), соответствующего
   * параметрам из таблицы. Использует ValidatorFactory и MatcherFactory с негативной стратегией
   * матчинга. Если найдено совпадение, выбрасывается ошибка. Поиск начинается с времени начала
   * сценария.
   *
   * @param topicName Имя топика
   * @param messageTypeString Тип сообщения (JSON|XML|TEXT)
   * @param params Параметры для матчинга (из таблицы)
   */
  @И(
      "^отсутствует сообщение в топике \"(.+)\", с типом (JSON|XML|TEXT) соответствующее параметрам из таблицы$")
  public void messageIsAbsenceInTopic(
      String topicName, String messageTypeString, Map<String, String> params) {
    long scenarioCoreStartTime = (long) akitaScenario.getVar("scenarioCoreStartTime");
    var scenarioTime =
        Instant.ofEpochMilli(scenarioCoreStartTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
    var conditions = Helpers.convertMapToConditions(params);
    akitaScenario.log("Время начала сценария: " + scenarioTime);
    MessageType messageType = MessageType.fromString(messageTypeString);
    var softAssertions = new SoftAssertions();
    KafkaConsumer<Object, Object> kafkaConsumer = new KafkaConsumer<>(kafkaProperties);
    var messageProcessor =
        MessageProcessor.validatingAndMatchingProcessor(
            messageType, false, softAssertions, conditions, MatchStrategy.NEGATIVE_ALL, true, null);
    var kafkaRunner = new KafkaTestRunner(kafkaConsumer, topicName, messageProcessor, false, false);
    kafkaRunner.tryPartitionAssignment();
    kafkaRunner.seekToTestStartTime(testTime);
    kafkaRunner.runPolling(2000, testTime);
    softAssertions.assertAll();
  }

  /**
   * Устанавливает подключение к Kafka, находит сообщение в топике с указанным заголовком и
   * значением (в hex-формате), и сохраняет значение заголовка в сценарийную переменную. Поиск
   * начинается с времени отправки (timestamp). Убеждается, что значение найдено
   * (Assertions.assertTrue).
   *
   * @param topicName Имя топика
   * @param headerKey Имя заголовка
   * @param headerValue Ожидаемое значение заголовка (в hex)
   * @param value Имя переменной для сохранения
   * @throws Exception Если произошла ошибка при подключении или чтении
   */
  @И(
      "^выполнено подключение к kafka и из сообщения из топика \"(.*)\" header \"(.*)\" со значением \"(.*)\" сохранен в переменную \"(.*)\"")
  public void saveHaderFromTopic(
      String topicName, String headerKey, String headerValue, String value) throws Exception {
    Consumer<String, String> consumer = null;
    String header = "";
    headerValue =
        BaseMethods.getPropertyOrStringVariableOrValueAndReplace(resolveVars(headerValue));
    try {
      consumer = new KafkaConsumer<>(kafkaProperties);
      int partitionsCount = consumer.partitionsFor(topicName).size();

      for (int partIndex = 0; partIndex < partitionsCount; partIndex++) {
        var topicPartition = new TopicPartition(topicName, partIndex);
        Map<TopicPartition, OffsetAndTimestamp> offsets =
            consumer.offsetsForTimes(Map.of(topicPartition, time));
        consumer.assign(Collections.singletonList(topicPartition));
        consumer.seek(
            topicPartition,
            ofNullable(offsets.get(topicPartition))
                .map(OffsetAndTimestamp::offset)
                .orElse(
                    consumer
                            .endOffsets(Collections.singletonList(topicPartition))
                            .get(topicPartition)
                        - 1));
        ConsumerRecords<String, String> tempRecords = consumer.poll(Duration.ofMillis(100L));
        for (ConsumerRecord<String, String> rec : tempRecords) {
          byte[] consumedHeader = rec.headers().lastHeader(headerKey).value();
          String headerRec = Hex.encodeHexString(consumedHeader);
          if (headerRec.equals(headerValue) && rec.timestamp() >= time) {
            header = headerRec;
            akitaScenario.setVar(value, header);
            akitaScenario.log(new String(rec.headers().lastHeader(headerKey).value()));
            break;
          }
        }
      }
    } catch (Exception e) {
      throw new Exception(e);
    } finally {
      consumer.close();
      Assertions.assertTrue(header.length() > 0);
    }
  }

  /**
   * Устанавливает подключение к Kafka, находит сообщение в топике по ключу и сохраняет тело
   * сообщения в сценарийную переменную. Поиск начинается с времени отправки (timestamp).
   *
   * @param key Ключ сообщения
   * @param topicName Имя топика
   * @param value Имя переменной для сохранения тела
   * @throws Exception Если произошла ошибка при подключении или чтении
   */
  @И(
      "^выполнено подключение к kafka и из сообщения по ключу \"(.*)\" из топика \"(.*)\" тело запроса сохранен в переменную \"(.*)\"")
  public void findMessageToKeyAndSaveValue(String key, String topicName, String value)
      throws Exception {
    Consumer<String, String> consumer = null;
    try {
      consumer = new KafkaConsumer<>(kafkaProperties);
      int partitionsCount = consumer.partitionsFor(topicName).size();

      for (int partIndex = 0; partIndex < partitionsCount; partIndex++) {
        var topicPartition = new TopicPartition(topicName, partIndex);
        Map<TopicPartition, OffsetAndTimestamp> offsets =
            consumer.offsetsForTimes(Map.of(topicPartition, time));
        consumer.assign(Collections.singletonList(topicPartition));
        consumer.seek(
            topicPartition,
            ofNullable(offsets.get(topicPartition))
                .map(OffsetAndTimestamp::offset)
                .orElse(
                    consumer
                            .endOffsets(Collections.singletonList(topicPartition))
                            .get(topicPartition)
                        - 1));
        ConsumerRecords<String, String> tempRecords = consumer.poll(Duration.ofMillis(100L));
        for (ConsumerRecord<String, String> rec : tempRecords) {

          if (rec.key().equals(key) && rec.timestamp() >= time) {
            akitaScenario.setVar(value, rec.value());
            akitaScenario.log(rec.value());
            break;
          }
        }
      }
    } catch (Exception e) {
      throw new Exception(e);
    } finally {
      consumer.close();
    }
  }

  /**
   * Устанавливает подключение к Kafka, ищет сообщение в топике с указанным заголовком (в hex) и
   * значением JSONPath. Если найдено, логирует сообщение; иначе выбрасывается AssertionError. Поиск
   * начинается с времени отправки (timestamp).
   *
   * @param topicName Имя топика
   * @param headerKey Имя заголовка
   * @param headerValue Значение заголовка (hex)
   * @param jsonPath JSONPath для проверки в теле
   * @param bodyValue Ожидаемое значение по JSONPath
   * @throws Exception Если произошла ошибка при подключении или чтении
   */
  @И(
      "^выполнено подключение к kafka и поиск сообщения в topic \"(.*)\" header \"(.*)\" с значением \"(.*)\" и jsonpath \"(.*)\" со значением \"(.*)\"")
  public void searchMessageWithHeaderAndValue(
      String topicName, String headerKey, String headerValue, String jsonPath, String bodyValue)
      throws Exception {
    Long time = (Long) akitaScenario.getVar("timestamp");
    boolean flag = false;
    Consumer<String, String> consumer = null;
    try {
      consumer = new KafkaConsumer<>(kafkaProperties);
      int partitionsCount = consumer.partitionsFor(topicName).size();

      for (int partIndex = 0; partIndex < partitionsCount; partIndex++) {
        var topicPartition = new TopicPartition(topicName, partIndex);
        Map<TopicPartition, OffsetAndTimestamp> offsets =
            consumer.offsetsForTimes(Map.of(topicPartition, time));
        consumer.assign(Collections.singletonList(topicPartition));
        consumer.seek(
            topicPartition,
            ofNullable(offsets.get(topicPartition))
                .map(OffsetAndTimestamp::offset)
                .orElse(
                    consumer
                        .endOffsets(Collections.singletonList(topicPartition))
                        .get(topicPartition)));
        ConsumerRecords<String, String> tempRecords = consumer.poll(Duration.ofMillis(100L));
        for (ConsumerRecord<String, String> rec : tempRecords) {
          String consumedHeader =
              Hex.encodeHexString(rec.headers().lastHeader(headerKey).value(), false);

          if (consumedHeader.equals(headerValue) && rec.timestamp() >= time) {
            String recValue = JsonPath.read(rec.value(), jsonPath).toString();
            if (recValue.equals(bodyValue)) {
              flag = true;
              akitaScenario.log("Сообщение найдено \n" + rec.value());
              break;
            }
          }
        }
      }
    } catch (Exception e) {
      throw new Exception(e);
    } finally {
      consumer.close();
      Assertions.assertTrue(flag, "Сообщение не найдено");
    }
  }

  /**
   * Устанавливает подключение к Kafka, ищет сообщение в топике с указанным заголовком и сохраняет
   * полное ConsumerRecord в сценарийную переменную. Поиск с таймаутом 10 секунд, начинается с
   * timestamp.
   *
   * @param topicName Имя топика
   * @param headerKey Имя заголовка
   * @param headerValue Значение заголовка (hex)
   * @param value Имя переменной для сохранения ConsumerRecord
   */
  @И(
      "^выполнено подключение к kafka и сообщение из топика \"(.*)\" header \"(.*)\" со значением \"(.*)\" сохранен в переменную \"(.*)\"")
  public void saveMessageFromTopic(
      String topicName, String headerKey, String headerValue, String value) {
    long time = (Long) akitaScenario.getVar("timestamp");
    long endPollingTimestamp = System.currentTimeMillis() + 10000;
    boolean flag = true;
    headerValue =
        BaseMethods.getPropertyOrStringVariableOrValueAndReplace(resolveVars(headerValue));
    KafkaConsumer<String, String> kafkaConsumer = null;

    kafkaConsumer = new KafkaConsumer<>(kafkaProperties);
    kafkaConsumer.subscribe(Collections.singletonList(topicName));
    while (System.currentTimeMillis() < endPollingTimestamp) {
      ConsumerRecords<String, String> records = kafkaConsumer.poll(100);
      if (flag) {
        Map<TopicPartition, Long> timestamps = new HashMap<>();
        Set<TopicPartition> topicPartitions = kafkaConsumer.assignment();
        topicPartitions.forEach(topicPartition -> timestamps.put(topicPartition, time));
        Map<TopicPartition, OffsetAndTimestamp> offsetAndTimestampMap =
            kafkaConsumer.offsetsForTimes(timestamps);
        KafkaConsumer<String, String> finalKafkaConsumer = kafkaConsumer;
        offsetAndTimestampMap.forEach(
            (topicPartition, offsetAndTimestamp) -> {
              if (offsetAndTimestamp != null) {
                finalKafkaConsumer.seek(topicPartition, offsetAndTimestamp.offset());
              }
            });
        flag = false;
      }
      for (ConsumerRecord<String, String> rec : records) {

        String headerRec = "";
        try {
          headerRec = Hex.encodeHexString(rec.headers().lastHeader(headerKey).value());
        } catch (NullPointerException e) {

        }
        if (headerRec.equals(headerValue) && rec.timestamp() >= time) {

          akitaScenario.setVar(value, rec);
          break;
        }
      }
    }
    kafkaConsumer.close();
  }

  /**
   * Из ранее сохраненного ConsumerRecord (из Kafka) извлекает значение указанного заголовка и
   * сохраняет в переменную. Поддерживает hex-формат (если headerKey содержит "HEX").
   *
   * @param messageKafka Имя переменной с ConsumerRecord
   * @param headerKey Имя заголовка (может содержать "HEX" для hex-декодирования)
   * @param value Имя переменной для сохранения значения
   */
  @И("^из сообщения от kafka \"(.*)\" header \"(.*)\" сохранен в переменную \"(.*)\"")
  public void fromTheMessageWithHeaderSaveValue(
      String messageKafka, String headerKey, String value) {
    ConsumerRecord<String, String> message =
        (ConsumerRecord<String, String>) akitaScenario.getVar(messageKafka);
    if (headerKey.contains("HEX")) {
      headerKey = headerKey.replaceAll("HEX", "");
      byte[] header = message.headers().lastHeader(headerKey).value();
      akitaScenario.setVar(value, Hex.encodeHexString(header));
      akitaScenario.log(Hex.encodeHexString(header));
    } else {
      String header = new String(message.headers().lastHeader(headerKey).value());
      akitaScenario.setVar(value, header);
      akitaScenario.log(header);
    }
  }

  /**
   * Устанавливает подключение к Kafka, ищет сообщение в топике по JSONPath и значению, сохраняет
   * тело в переменную. Если не найдено, выбрасывается AssertionError. Поиск начинается с timestamp.
   *
   * @param topicName Имя топика
   * @param jsonPath JSONPath для проверки
   * @param bodyValue Ожидаемое значение
   * @param value Имя переменной для сохранения тела
   * @throws Exception Если произошла ошибка при подключении или чтении
   */
  @И(
      "^выполнено подключение к kafka и поиск сообщения в topic \"(.*)\" и jsonpath \"(.*)\" со значением \"(.*)\" сохранено в переменную \"(.*)\"")
  public void searchMessageValueAndSave(
      String topicName, String jsonPath, String bodyValue, String value) throws Exception {
    Long time = (Long) akitaScenario.getVar("timestamp");
    boolean flag = false;
    Consumer<String, String> consumer = null;
    try {
      consumer = new KafkaConsumer<>(kafkaProperties);
      int partitionsCount = consumer.partitionsFor(topicName).size();

      for (int partIndex = 0; partIndex < partitionsCount; partIndex++) {
        if (flag) break;
        var topicPartition = new TopicPartition(topicName, partIndex);
        Map<TopicPartition, OffsetAndTimestamp> offsets =
            consumer.offsetsForTimes(Map.of(topicPartition, time));
        consumer.assign(Collections.singletonList(topicPartition));
        consumer.seek(
            topicPartition,
            ofNullable(offsets.get(topicPartition))
                .map(OffsetAndTimestamp::offset)
                .orElse(
                    consumer
                        .endOffsets(Collections.singletonList(topicPartition))
                        .get(topicPartition)));
        ConsumerRecords<String, String> tempRecords = consumer.poll(Duration.ofMillis(100L));
        for (ConsumerRecord<String, String> rec : tempRecords) {

          if (rec.timestamp() >= time) {
            String recValue = JsonPath.read(rec.value(), jsonPath).toString();
            if (recValue.equals(bodyValue)) {
              flag = true;
              akitaScenario.log("Сообщение найдено \n" + rec.value());
              akitaScenario.setVar(value, rec.value());
              break;
            }
          }
        }
      }
    } catch (Exception e) {
      throw new Exception(e);
    } finally {
      consumer.close();
      Assertions.assertTrue(flag, "Сообщение не найдено");
    }
  }

  /**
   * Устанавливает подключение к Kafka, проверяет отсутствие сообщения в топике по JSONPath и
   * значению. Если найдено, выбрасывается AssertionError.
   *
   * @param topicName Имя топика
   * @param jsonPath JSONPath для проверки
   * @param bodyValue Ожидаемое значение (которого не должно быть)
   * @throws Exception Если произошла ошибка при подключении или чтении
   */
  @И(
      "^выполнено подключение к kafka и поиск сообщения в topic \"(.*)\" и jsonpath \"(.*)\" со значением \"(.*)\" не существует")
  public void searchMessageAndDoesNotExists(String topicName, String jsonPath, String bodyValue)
      throws Exception {
    boolean flag = false;
    Consumer<String, String> consumer = null;
    try {
      consumer = new KafkaConsumer<>(kafkaProperties);
      int partitionsCount = consumer.partitionsFor(topicName).size();

      for (int partIndex = 0; partIndex < partitionsCount; partIndex++) {
        consumer.subscribe(Collections.singletonList(topicName));
        ConsumerRecords<String, String> tempRecords = consumer.poll(Duration.ofMillis(1000L));
        for (ConsumerRecord<String, String> rec : tempRecords) {
          String recValue = JsonPath.read(rec.value(), jsonPath).toString();
          if (recValue.equals(bodyValue)) {
            flag = true;
            Assertions.assertFalse(flag, "Сообщение найдено \n" + rec.value());
            break;
          }
        }
      }
    } catch (Exception e) {
      throw new Exception(e);
    } finally {
      consumer.close();
    }
  }

  /**
   * Устанавливает подключение к Kafka, ищет сообщение в топике по параметрам из таблицы
   * (header/jsonPath и value), сохраняет тело в переменную. Если не найдено, выбрасывается
   * AssertionError.
   *
   * @param topicName Имя топика
   * @param value Имя переменной для сохранения тела
   * @param dataTable Таблица с параметрами (header/jsonPath, value)
   * @throws Exception Если произошла ошибка при подключении или чтении
   */
  @И(
      "^выполнено подключение к kafka и поиск сообщения в topic \"(.*)\" c параметрами из таблицы сохранено в переменную \"(.*)\"")
  public void searchMessageInKafka(String topicName, String value, DataTable dataTable)
      throws Exception {
    List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
    Consumer<String, String> consumer = null;
    try {
      consumer = new KafkaConsumer<>(kafkaProperties);
      consumer.subscribe(Collections.singletonList(topicName));
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
      System.out.println(records.count());
      for (ConsumerRecord<String, String> record : records) {
        System.out.println(record);
        boolean matches = true;
        for (Map<String, String> row : rows) {
          String type = row.get("header/jsonPath");
          String expectedValue = row.get("value");
          try {
            if (type.startsWith("$."))
              matches &= matchJsonPath(record.value(), type, expectedValue);
            else matches &= matchHeader(record.headers(), type, expectedValue);
          } catch (Exception e) {
            matches = false;
          }

          if (!matches) break;
        }

        if (matches) {
          akitaScenario.setVar(value, record.value());

          akitaScenario.log("Сообщение найдено \n" + record.value());
          return;
        }
      }

      throw new AssertionError("Сообщение не найдено по заданным критериям");

    } catch (Exception e) {
      throw new Exception(e);
    } finally {
      consumer.close();
    }
  }

  /**
   * Проверяет совпадение значения по JSONPath в JSON-строке с ожидаемым значением.
   *
   * @param json JSON-строка
   * @param path JSONPath
   * @param expectedValue Ожидаемое значение
   * @return true, если совпадает
   */
  private boolean matchJsonPath(String json, String path, String expectedValue) {
    try {
      Object value = JsonPath.read(json, path);
      return value != null && value.toString().equals(expectedValue);
    } catch (PathNotFoundException e) {
      return false;
    }
  }

  /**
   * Проверяет наличие заголовка с указанным ключом и hex-значением в Headers.
   *
   * @param headers Заголовки сообщения
   * @param headerKey Ключ заголовка
   * @param expectedValue Ожидаемое hex-значение
   * @return true, если найден совпадающий заголовок
   */
  private boolean matchHeader(Headers headers, String headerKey, String expectedValue) {
    return StreamSupport.stream(headers.spliterator(), false)
        .anyMatch(
            header ->
                header.key().equals(headerKey)
                    && Hex.encodeHexString(header.value(), false).equals(expectedValue));
  }
}
