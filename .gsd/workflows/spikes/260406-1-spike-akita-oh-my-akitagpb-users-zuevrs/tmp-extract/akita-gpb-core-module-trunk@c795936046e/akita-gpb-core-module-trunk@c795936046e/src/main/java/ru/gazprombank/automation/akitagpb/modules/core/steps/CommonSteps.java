package ru.gazprombank.automation.akitagpb.modules.core.steps;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.*;
import static ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables.resolveVars;

import com.codeborne.pdftest.PDF;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.И;
import io.cucumber.java.ru.Когда;
import io.cucumber.java.ru.Тогда;
import java.io.File;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.PropertyLoader;

/**
 * Шаги для тестирования взаимодействия с внешним окружением (устройствами, файлами, переменными
 * окружения, property и т.д.)
 */
@Slf4j
public class CommonSteps extends BaseMethods {

  /**
   * Значение заданной переменной из application.properties сохраняется в переменную в akitaScenario
   * для дальнейшего использования
   */
  @И("^сохранено значение \"(.*)\" из property файла в переменную \"(.*)\"$")
  public void saveValueToVar(String propertyVariableName, String variableName) {
    propertyVariableName = PropertyLoader.loadProperty(propertyVariableName);
    akitaScenario.setVar(variableName, propertyVariableName);
    akitaScenario.log("Значение сохраненной переменной " + propertyVariableName);
  }

  /**
   * Устанавливается значение переменной в хранилище переменных. Один из кейсов: установка login
   * пользователя
   */
  @И("^установлено значение переменной \"(.*)\" равным \"(.*)\"$")
  public void setVariable(String variableName, String value) {
    value = PropertyLoader.getPropertyOrValue(value);
    akitaScenario.setVar(variableName, value);
  }

  /** Ожидание в течение заданного количества секунд */
  @Когда("^выполнено ожидание в течение (\\d+) (?:секунд|секунды)")
  public void waitForSeconds(long seconds) throws InterruptedException {
    sleep(1000 * seconds);
  }

  /** Выполняется чтение файла с шаблоном и заполнение его значениями из таблицы */
  @И("^шаблон \"(.*)\" заполнен данными из таблицы и сохранён в переменную \"(.*)\"$")
  public void fillTemplate(String templateName, String varName, DataTable table) {
    String template = PropertyLoader.loadValueFromFileOrPropertyOrVariableOrDefault(templateName);
    boolean error = false;
    for (List<String> list : table.cells()) {
      String regexp = list.get(0);
      String replacement = list.get(1);
      if (template.contains(regexp)) {
        template = template.replaceAll(regexp, replacement);
      } else {
        akitaScenario.log("В шаблоне не найден элемент " + regexp);
        error = true;
      }
    }
    if (error) {
      throw new RuntimeException("В шаблоне не найдены требуемые регулярные выражения");
    }
    akitaScenario.setVar(varName, template);
  }

  /** Проверка равенства двух переменных из хранилища */
  @Тогда("^значения в переменных \"(.*)\" и \"(.*)\" совпадают$")
  public void compareTwoVariables(String firstVariableName, String secondVariableName) {
    String firstValueToCompare = akitaScenario.getVar(firstVariableName).toString();
    String secondValueToCompare = akitaScenario.getVar(secondVariableName).toString();
    assertThat(
        String.format(
            "Значения в переменных [%s] и [%s] не совпадают",
            firstVariableName, secondVariableName),
        firstValueToCompare,
        equalTo(secondValueToCompare));
  }

  /** Проверка неравенства двух переменных из хранилища */
  @Тогда("^значения в переменных \"(.*)\" и \"(.*)\" не совпадают$")
  public void checkingTwoVariablesAreNotEquals(
      String firstVariableName, String secondVariableName) {
    String firstValueToCompare = akitaScenario.getVar(firstVariableName).toString();
    String secondValueToCompare = akitaScenario.getVar(secondVariableName).toString();
    assertThat(
        String.format(
            "Значения в переменных [%s] и [%s] совпадают", firstVariableName, secondVariableName),
        firstValueToCompare,
        Matchers.not(equalTo(secondValueToCompare)));
  }

  @И("^удалить файл \"(.*)\" из директории \"(.*)\"$")
  @SneakyThrows
  public void deleteDownloadedFile(String fileName, String path) {
    var directory = new File(path);
    var files = directory.listFiles();
    var i = 0;
    for (var f : files) {
      if (f.getName().equals(fileName)) {
        i++;
        if (f.delete()) {
          log.info("Файл удален.");
        } else {
          log.warn("Невозможно удалить файл!");
        }
      }
    }
    if (i == 0) {
      throw new Exception("Файл не найден!");
    }
  }

  @И("^в файле \"(.*)\" содержится текст \"(.*)\"$")
  @SneakyThrows
  public void checkTextFromFile(String varName, String text) {
    var file = (File) akitaScenario.getVar(varName);
    PDF pdf = new PDF(file);
    assertTrue(
        pdf.text.contains(getPropertyOrStringVariableOrValue(resolveVars(text))),
        String.format("В файле [%s] отсутствует фрагмент [%s]", varName, text));
  }

  /** Проверка совпадения значения из переменной и значения из property */
  @Тогда("^значения из переменной \"(.*)\" и из property файла \"(.*)\" совпадают$")
  public void checkIfValueFromVariableEqualPropertyVariable(
      String envVariable, String propertyVariable) {
    assertThat(
        "Переменные " + envVariable + " и " + propertyVariable + " не совпадают",
        (String) akitaScenario.getVar(envVariable),
        equalToIgnoringCase(PropertyLoader.loadProperty(propertyVariable)));
  }

  /**
   * Проверка выражения на истинность выражение из property, из переменной сценария или значение
   * аргумента Например, string1.equals(string2) OR string.equals("string") Любое Java-выражение,
   * возвращающие boolean
   */
  @Тогда("^верно, что \"(.*)\"$")
  public void expressionExpression(String expression) {
    akitaScenario.getVars().evaluate("assert(" + expression + ")");
  }
}
