package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Builder для формирования запроса к kafka */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KafkaRequestParam {
  private KafkaRequestParamType type;
  private String name;
  private String value;

  public void setType(String type) {
    this.type = KafkaRequestParamType.valueOf(type.toUpperCase());
  }
}
