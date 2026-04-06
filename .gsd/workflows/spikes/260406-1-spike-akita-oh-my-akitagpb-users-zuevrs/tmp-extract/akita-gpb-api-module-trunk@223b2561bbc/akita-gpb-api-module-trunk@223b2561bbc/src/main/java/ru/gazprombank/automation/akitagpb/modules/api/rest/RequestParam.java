package ru.gazprombank.automation.akitagpb.modules.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Builder для формирования http запроса */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestParam {

  private RequestParamType type;
  private String name;
  private String value;

  public void setType(String type) {
    this.type = RequestParamType.valueOf(type.toUpperCase());
  }
}
