package ru.gazprombank.automation.akitagpb.modules.kafkamq.mq.ibm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Builder для формирования запроса к mq */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MqRequestParam {
  private MqRequestParamType type;
  private String name;
  private String value;

  public void setType(String type) {
    this.type = MqRequestParamType.valueOf(type.toUpperCase());
  }
}
