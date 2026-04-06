package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.dataTableModels;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

import io.cucumber.java.DataTableType;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Модельный класс для получения java-объекта из таблицы параметров кукумбер-шага, реализующего
 * сравнение ожидаемых значений с фактическими, полученными из json'а.
 */
@Getter
@Setter
@NoArgsConstructor // без дефолтного конструктора кукумбер не сможет создать объект
public class JsonAssertableConditionParam extends AssertableConditionParam {

  private String[] ignoringJsonFields;

  /**
   * Конструктор параметра-условия
   *
   * @param path путь к полю json'а
   * @param type тип данных поля ("число", "строка", "null", "булевый", "json-объект",
   *     "json-массив", "дата")
   * @param condition условие проверки
   * @param expectedValue ожидаемое значение
   * @param offset допустимое отклонение от ожидаемого значения
   * @param jsonFieldsToIgnore игнорируемые поля (jsonpath'ы таких полей, через запятую, если их
   *     несколько) при сравнении json'ов
   */
  public JsonAssertableConditionParam(
      String path,
      String type,
      String condition,
      String expectedValue,
      String offset,
      String jsonFieldsToIgnore) {
    super(path, type, condition, expectedValue, offset);

    if (jsonFieldsToIgnore != null) {
      ignoringJsonFields = jsonFieldsToIgnore.split(",\\s*");
    }
  }

  /**
   * Метод для конвертации кукумбер-таблицы параметров шага проверки json'а в объект класса.
   * Вызывается кукумбером.
   *
   * @param entry карта параметров шага
   * @return объект класса JsonConditionParam, соответствующий переданным параметрам
   */
  @DataTableType
  public JsonAssertableConditionParam jsonConditionParamTransformer(Map<String, String> entry) {
    if (entry.get("Path") == null
        && (entry.get("Значение") == null
            || (entry.get("Тип") == null && entry.get("Условие") == null))) {
      throw new RuntimeException(
          "В таблице параметров шага должна быть строка-заголовок с как минимум двумя обязательными столбцами - "
              + "'Path' и 'Значение' (при проверке на null) - или с как минимум тремя обязательными столбцами - "
              + "'Path', 'Тип' и 'Условие' (при проверке пустого значения строки)");
    }
    return new JsonAssertableConditionParam(
        resolveVariables(entry.get("Path")),
        entry.get("Тип"),
        entry.get("Условие"),
        entry.get("Значение"),
        entry.get("Допуск"),
        entry.get("Игнорируемые поля"));
  }
}
