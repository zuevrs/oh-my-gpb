package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka;

import io.cucumber.datatable.DataTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.last.UniversalMatchCondition;

public class Helpers {

  private static Map<String, String> parseDataTable(DataTable dataTable) {
    return dataTable.cells().stream()
        .collect(
            Collectors.toMap(
                it -> ScopedVariables.resolveVars(it.get(0)),
                it -> {
                  String expectedValue = String.valueOf(it.get(1));
                  expectedValue =
                      BaseMethods.getInstance().getPropertyOrStringVariableOrValue(expectedValue);
                  return ScopedVariables.resolveVars(expectedValue);
                }));
  }

  public static List<UniversalMatchCondition> parseDataTableToConditions(DataTable dataTable) {
    // Парсим DataTable в Map<String, String>, где key - поле (field), value - ожидаемое значение
    Map<String, String> paramsMap = parseDataTable(dataTable);

    List<UniversalMatchCondition> conditions = new ArrayList<>();

    for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
      String field = entry.getKey();
      String expectedValue = entry.getValue();

      // Создаем UniversalMatchCondition с условием "exact" по умолчанию
      // Можно добавить логику для других типов, если в будущем DataTable расширится (например,
      // третья колонка для type)
      UniversalMatchCondition condition =
          UniversalMatchCondition.create()
              .field(field)
              .required(true) // По умолчанию required=true, можно параметризовать
              .addExact(expectedValue) // Используем "exact" как дефолтный тип условия
              .build();

      conditions.add(condition);
    }

    return conditions;
  }

  public static List<UniversalMatchCondition> convertMapToConditions(
      Map<String, String> paramsMap) {
    List<UniversalMatchCondition> conditions = new ArrayList<>();

    for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
      String field = ScopedVariables.resolveVars(entry.getKey());
      String expectedValue = entry.getValue();
      expectedValue = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(expectedValue);
      expectedValue = ScopedVariables.resolveVars(expectedValue);
      System.out.println(expectedValue);
      UniversalMatchCondition condition =
          UniversalMatchCondition.create()
              .field(field)
              .required(true)
              .addExact(expectedValue)
              .build();

      conditions.add(condition);
    }

    return conditions;
  }
}
