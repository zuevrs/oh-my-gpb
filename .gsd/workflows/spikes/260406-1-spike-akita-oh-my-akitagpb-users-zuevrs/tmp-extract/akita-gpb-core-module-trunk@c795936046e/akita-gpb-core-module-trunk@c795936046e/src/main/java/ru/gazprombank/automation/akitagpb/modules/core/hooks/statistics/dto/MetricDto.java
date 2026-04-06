package ru.gazprombank.automation.akitagpb.modules.core.hooks.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MetricDto {

  private String metric;

  //  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String value = "";

  public MetricDto(String metric) {
    this.metric = metric;
  }
}
