package ru.gazprombank.automation.akitagpb.modules.core.cucumber;

import com.google.common.collect.Maps;
import com.google.gson.JsonParser;
import groovy.lang.GroovyShell;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

/** Реализация хранилища переменных, заданных пользователем, внутри тестовых сценариев */
public class ScopedVariables {

  /**
   * Название переменной должно быть длинной минимум 2 символа и начинаться и заканчиваться буквой.
   * Между словами или буквами могут быть точки или тире.
   */
  public static final String VARIABLE_NAME_PATTERN =
      "\\$[{]([\\wа-яА-Я]+[\\wа-яА-Я.-]*[\\wа-яА-Я]+)[}]";

  private Map<String, Object> variables = Maps.newHashMap();

  /**
   * Компилирует и выполняет в рантайме переданный на вход java/groovy-код. Предварительно загружает
   * в память все переменные, т.е. на вход в строковом аргументе могут быть переданы переменные из
   * "variables"
   *
   * @param expression java/groovy-код, который будет выполнен
   */
  public Object evaluate(String expression) {
    GroovyShell shell = new GroovyShell();
    variables
        .entrySet()
        .forEach(
            e -> {
              try {
                shell.setVariable(e.getKey(), new BigDecimal(e.getValue().toString()));
              } catch (NumberFormatException exp) {
                shell.setVariable(e.getKey(), e.getValue());
              }
            });
    return shell.evaluate(expression);
  }

  /**
   * Заменяет в строке все ключи переменных из "variables" на их значения
   *
   * @param textToReplaceIn строка, в которой необходимо выполнить замену (не модифицируется)
   */
  public String replaceVariables(String textToReplaceIn) {
    Pattern p = Pattern.compile(VARIABLE_NAME_PATTERN);
    Matcher m = p.matcher(textToReplaceIn);
    StringBuffer buffer = new StringBuffer();
    while (m.find()) {
      String varName = m.group(1);
      String value = get(varName).toString();
      m.appendReplacement(buffer, value);
    }
    m.appendTail(buffer);
    return buffer.toString();
  }

  /**
   * Производит поиск в заданной строке на наличие совпадений параметров. В случае нахождения
   * параметра в строке заменяет его значение на значение из properties или хранилища переменных
   *
   * @param inputString заданная строка
   * @return новая строка
   */
  public static String resolveVars(String inputString) {
    Pattern p = Pattern.compile(VARIABLE_NAME_PATTERN);
    Matcher m = p.matcher(inputString);
    String newString = inputString;
    List<String> unresolvedVariables = new ArrayList<>();
    while (m.find()) {
      String varName = m.group(1);
      String value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(varName);
      if (value.equals(varName)) {
        unresolvedVariables.add(varName);
        value = varName;
      }
      if (value.contains("$") || value.contains("\\")) {
        // Если значение содержит символы $ или \ (например, пароли), то оно некорректно
        // обрабатывается методом replaceFirst
        newString = newString.replace("${" + varName + "}", value);
      } else {
        newString = m.replaceFirst(value);
      }

      m = p.matcher(newString);
    }
    if (!unresolvedVariables.isEmpty()) {
      throw new IllegalArgumentException(
          "Значения "
              + unresolvedVariables
              + " не были найдены ни в application.properties, ни в environment переменной");
    }
    return newString;
  }

  /**
   * Проверяет, является ли переданная в качестве аргумента строка валидным JSON
   *
   * @param jsonInString - строка для валидации
   * @return
   */
  public static boolean isJSONValid(String jsonInString) {
    try {
      JsonParser parser = new JsonParser();
      parser.parse(jsonInString);
    } catch (com.google.gson.JsonSyntaxException ex) {
      return false;
    }
    return true;
  }

  /**
   * Мтеод проверяет, что имя переменной удовлетворяет регулярному выражению, по которому резолвятся
   * переменные. Если нет - брсоается исключение, чтобы не создавать переменные, которые нельзя
   * будет обработать. Если да - то в Map добавляется новая переменная или обновляется её значение.
   *
   * @param name имя переменной.
   * @param value значение переменной.
   */
  public void put(String name, Object value) {
    Pattern p = Pattern.compile(VARIABLE_NAME_PATTERN);
    Matcher m = p.matcher("${" + name + "}");
    if (!m.find()) {
      throw new RuntimeException(
          String.format(
              "Имя переменной не может быть %s.\nИмя переменной должно удовлетворять регулярному выражению %s",
              name, VARIABLE_NAME_PATTERN));
    }

    variables.put(name, value);
  }

  public Object get(String name) {
    return variables.get(name);
  }

  public void clear() {
    variables.clear();
  }

  public Object remove(String key) {
    return variables.remove(key);
  }

  public Map<String, Object> getVariables() {
    return variables;
  }
}
