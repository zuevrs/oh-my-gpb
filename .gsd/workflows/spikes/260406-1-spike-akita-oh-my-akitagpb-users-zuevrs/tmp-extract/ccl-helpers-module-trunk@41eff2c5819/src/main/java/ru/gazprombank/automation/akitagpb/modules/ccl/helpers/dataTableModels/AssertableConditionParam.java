package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.assertj.core.data.TemporalUnitOffset;
import org.json.JSONArray;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.date.DateHelper;

/**
 * Общий модельный класс для получения java-объекта из таблицы параметров кукумбер-шагов,
 * реализующих сравнение ожидаемых и фактических значений.
 */
@Getter
@Setter
@NoArgsConstructor // без дефолтного конструктора кукумбер не сможет создать объект
public abstract class AssertableConditionParam {

  protected String path;
  protected String type;
  protected String condition;
  protected Object expectedValue;
  protected Double numberOffset;
  protected TemporalUnitOffset dateOffset;

  public AssertableConditionParam(
      String path, String type, String condition, String expectedValue, String offset) {
    this.path = path;
    this.type = type;
    this.condition = condition == null ? "" : condition;

    if (expectedValue == null) {
      this.expectedValue = null;
    } else if ("json-массив".equals(type)
        && (condition != null && condition.contains("содержит"))) {
      var expectedResolved = resolveVariables(expectedValue);
      expectedResolved =
          expectedResolved.startsWith("[") ? expectedResolved : "[" + expectedResolved + "]";
      var jsonArray = new JSONArray(expectedResolved);
      // если массив json-ов - парсим его с помощью JSONArray и кладём каждый json в список в виде
      // строки
      if (jsonArray.optJSONObject(0) != null) {
        var jsons = new ArrayList<String>();
        for (int i = 0; i < jsonArray.length(); i++) {
          jsons.add(jsonArray.getJSONObject(i).toString());
        }
        this.expectedValue = jsons;
        // иначе разделяем массив в список регуляркой (чтобы сохранялись кавычки у строк)
      } else {
        this.expectedValue =
            Arrays.stream(expectedResolved.replace("[", "").replace("]", "").split(",\\s*"))
                .collect(Collectors.toList());
      }
    } else if (condition != null && condition.matches("од.+ из")) {
      this.expectedValue =
          Arrays.stream(expectedValue.split(","))
              .map(e -> processValue(e.trim()))
              .collect(Collectors.toList());
    } else if ("строка".equals(type) || "json-объект".equals(type) || "json-массив".equals(type)) {
      this.expectedValue = processValue(expectedValue);
      this.expectedValue =
          "json-массив".equals(type) && !this.expectedValue.toString().startsWith("[")
              ? "[" + this.expectedValue + "]"
              : this.expectedValue;
    } else {
      this.expectedValue = resolveVariables(expectedValue);
    }

    this.type = "null".equals(expectedValue) ? "null" : type;

    if (offset != null) {
      if (offset.contains("s")
          || offset.contains("m")
          || offset.contains("h")
          || offset.contains("d")) {
        this.dateOffset = DateHelper.getTemporalOffset(offset);
      } else {
        this.numberOffset = Double.parseDouble(processValue(offset));
      }
    }
  }
}
