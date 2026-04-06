package ru.gazprombank.automation.akitagpb.modules.kafkamq.mq.artemis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArtemisRequestParam {
  private ArtemisRequestParamType type;
  private String name;
  private String value;

  public void setType(String type) {
    this.type = ArtemisRequestParamType.valueOf(type.toUpperCase());
  }
}
