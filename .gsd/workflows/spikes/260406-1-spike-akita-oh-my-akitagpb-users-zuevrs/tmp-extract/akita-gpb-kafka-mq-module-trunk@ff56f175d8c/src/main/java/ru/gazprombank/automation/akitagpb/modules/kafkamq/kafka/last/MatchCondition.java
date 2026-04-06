package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.last;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Getter;

public abstract class MatchCondition {
  // Геттеры
  @Getter protected String field; // headerKey для Header, jsonPath для JSON, xpath для XML
  @Getter protected boolean required;
  protected List<Condition> conditions;

  // Конструктор
  protected MatchCondition(String field, boolean required, List<Condition> conditions) {
    this.field = field;
    this.required = required;
    this.conditions = conditions == null ? new ArrayList<>() : new ArrayList<>(conditions);
  }

  public List<Condition> getConditions() {
    return new ArrayList<>(conditions); // Возвращаем копию для иммутабельности
  }

  // Внутренний класс Condition
  public static class Condition {
    private String type; // "regex", "contains", "exact", "auto"
    private String value;

    // Конструктор
    public Condition(String type, String value) {
      this.type = type;
      this.value = value;
    }

    // Геттеры
    public String getType() {
      return type;
    }

    public String getValue() {
      return value;
    }
  }

  // Проверка условия
  public boolean checkCondition(String fieldValue, Condition condition) {
    if ("auto".equals(condition.getType()) || "required".equals(condition.getType())) {
      return true; // Обрабатывается отдельно
    }
    return switch (condition.getType()) {
      case "regex" -> fieldValue != null && Pattern.matches(condition.getValue(), fieldValue);
      case "contains" -> fieldValue != null && fieldValue.contains(condition.getValue());
      case "exact" -> fieldValue != null && fieldValue.equals(condition.getValue());
      default -> false;
    };
  }
}
