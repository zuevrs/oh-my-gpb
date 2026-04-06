package ru.gazprombank.automation.akitagpb.modules.kafkamq.hooks;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.config.KafkaProperties;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.KafkaRequestParam;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.KafkaRequestParamType;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers.registry.MatcherRegistryInitializer;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.mq.ibm.MqRequestParam;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.mq.ibm.MqRequestParamType;

@Slf4j
public class KafkaMqHooks extends BaseMethods {
  private static final Properties kafkaProperties = new KafkaProperties().toProperties();
  private static volatile Boolean isKafkaAvailable = null; // null = ещё не проверяли

  public static boolean isKafkaAlive() {
    if (isKafkaAvailable == null) {
      synchronized (KafkaMqHooks.class) {
        if (isKafkaAvailable == null) { // double-checked locking
          isKafkaAvailable = checkKafkaAvailability();
        }
      }
    }
    return isKafkaAvailable;
  }

  private static boolean checkKafkaAvailability() {
    try (AdminClient admin = AdminClient.create(kafkaProperties)) {
      admin.listTopics().names().get();
      System.out.println("Kafka is available");
      MatcherRegistryInitializer.initialize();
      return true;
    } catch (InterruptedException | ExecutionException e) {
      System.err.println(String.format("Kafka is not available: %s", e.getMessage()));
      return false;
    }
  }

  /*Только для сценариев с тегом @kafka. Если kafka недоступна, то все сценарии выключаются разом
  Тег @kafka сделан для обратной совместимости*/
  @Before("@kafka")
  public void checkKafka() {
    assertThat(isKafkaAlive()).as("Kafka is not available").isTrue();
  }

  @DataTableType
  public MqRequestParam mqRequestParamTransformer(Map<String, String> entry) {
    return new MqRequestParam(
        MqRequestParamType.valueOf(entry.get("type").toUpperCase()),
        entry.get("name"),
        entry.get("value"));
  }

  @DataTableType
  public KafkaRequestParam kafkaRequestParamTransformer(Map<String, String> entry) {
    return new KafkaRequestParam(
        KafkaRequestParamType.valueOf(entry.get("type").toUpperCase()),
        entry.get("name"),
        entry.get("value"));
  }
}
