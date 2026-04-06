package ru.gazprombank.automation.akitagpb.modules.core.hooks.statistics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StatisticsDto {

  @JsonProperty("report_date")
  private String reportDate;

  @JsonProperty("auto_system")
  private String autoSystem;

  @JsonProperty("app_name")
  private String appName;

  @JsonProperty("app_version")
  private String appVersion;

  @JsonProperty("env_name")
  private String envName;

  @JsonProperty("report_type")
  private String reportType;

  @JsonProperty("metrics")
  private List<MetricDto> metricsList;
}
