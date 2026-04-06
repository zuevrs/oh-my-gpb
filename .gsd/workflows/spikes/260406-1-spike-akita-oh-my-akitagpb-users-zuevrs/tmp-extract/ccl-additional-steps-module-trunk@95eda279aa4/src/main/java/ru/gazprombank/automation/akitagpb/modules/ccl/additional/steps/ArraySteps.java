package ru.gazprombank.automation.akitagpb.modules.ccl.additional.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;

import io.cucumber.java.ru.И;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class ArraySteps extends BaseMethods {

  @SuppressWarnings("unchecked")
  @И("^массив \"(.*)\" совпадает с массивом \"(.*)\", тип элементов (строка|число|дата)$")
  public void compareArray(String arrayName1, String arrayName2, String typeElements) {
    Object arrayList1 = akitaScenario.getVar(arrayName1);
    Object arrayList2 = akitaScenario.getVar(arrayName2);
    switch (typeElements) {
      case "число" -> compareArrays((List<Integer>) arrayList1, (List<Integer>) arrayList2);
      case "дата" -> compareArrays(
          (List<ZonedDateTime>) arrayList1, (List<ZonedDateTime>) arrayList2);
      default -> compareArrays((List<String>) arrayList1, (List<String>) arrayList2);
    }
  }

  private <T> void compareArrays(List<T> arrayList1, List<T> arrayList2) {
    Assertions.assertThat(arrayList1).hasSameElementsAs(arrayList2);
  }

  /**
   * Если переменная varName уже создана - проверяем, что она является списком и добавляем в список
   * значение. Если переменная не создана - создаём новый список и добавляем в него значение value.
   *
   * @param value значение, которое надо сохранить.
   * @param varName имя переменной сценария.
   */
  @SuppressWarnings("unchecked")
  @И("^значение \"(.*)\" добавляем к списку в переменной сценария \"(.*)\"$")
  public void addStringValueToList(String value, String varName) {
    value = processValue(value);
    var listObj = akitaScenario.tryGetVar(varName);
    if (listObj == null) {
      listObj = new ArrayList<String>();
      ((List<String>) listObj).add(value);
    } else {
      assertThat(listObj)
          .as("Переменная %s не является списком!", varName)
          .isInstanceOf(List.class);
      ((List<String>) listObj).add(value);
    }
    akitaScenario.setVar(varName, listObj);
  }
}
