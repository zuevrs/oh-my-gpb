package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.last;

import java.util.ArrayList;
import java.util.List;

public class UniversalMatchCondition extends MatchCondition {
  // Конструктор
  public UniversalMatchCondition(String field, boolean required, List<Condition> conditions) {
    super(field, required, conditions != null ? new ArrayList<>(conditions) : new ArrayList<>());
  }

  // Builder
  public static class Builder {
    private String field;
    private boolean required;
    private List<Condition> conditions = new ArrayList<>();

    public Builder field(String field) {
      this.field = field;
      return this;
    }

    public Builder required(boolean required) {
      this.required = required;
      return this;
    }

    public Builder addAuto() {
      conditions.add(new Condition("auto", null));
      return this;
    }

    public Builder addRegex(String regex) {
      conditions.add(new Condition("regex", regex));
      return this;
    }

    public Builder addContains(String substring) {
      conditions.add(new Condition("contains", substring));
      return this;
    }

    public Builder addExact(String exactValue) {
      conditions.add(new Condition("exact", exactValue));
      return this;
    }

    public UniversalMatchCondition build() {
      return new UniversalMatchCondition(field, required, conditions);
    }
  }

  public static Builder create() {
    return new Builder();
  }
}
