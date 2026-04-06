package ru.gazprombank.automation.akitagpb.modules.ccl.files.steps;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.HtmlHelper.getElementAttributeValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.HtmlHelper.getElementByXpath;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.HtmlHelper.getElementOwnText;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;

import io.cucumber.java.ru.И;
import org.jsoup.nodes.Element;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class HtmlSteps extends BaseMethods {

  /**
   * Шаг для сохранения html-элемента (тега) по xpath в переменную сценария. Пример: И из
   * HTML-строки "${emailBody}" получить элемент по xpath "//a" и сохранить в переменную "hyperlink"
   *
   * @param html переменная сценария или html-строка
   * @param xpath xpath к нужному элементу
   * @param varName переменная сценария для сохранения
   */
  @И(
      "^из HTML-строки \"(.+)\" получить элемент по xpath \"(.+)\" и сохранить в переменную \"(.+)\"$")
  public void getElementFromHtmlByXpath(String html, String xpath, String varName) {
    html = processValue(html);
    xpath = processValue(xpath);
    var element = getElementByXpath(html, xpath);
    akitaScenario.setVar(varName, element);
    akitaScenario.log(
        String.format("В переменную '%s' сохранён элемент '%s'", varName, element.toString()));
  }

  /**
   * Шаг для получения внутреннего текста html-элемента (тега).
   *
   * @param elementVar переменная сценария, в которой сохранён html-элемент
   * @param varName переменная сценария для сохранения
   */
  @И("^из HTML-элемента \"(.+)\" сохранить внутренний текст в переменную \"(.+)\"$")
  public void getHtmlElementOwnText(String elementVar, String varName) {
    elementVar = elementVar.replace("${", "").replace("}", "");
    var element = (Element) akitaScenario.getVar(elementVar);
    var value = getElementOwnText(element);
    akitaScenario.setVar(varName, value);
    akitaScenario.log(String.format("В переменную '%s' сохранено значение '%s'", varName, value));
  }

  /**
   * Шаг для получения значения атрибута html-элемента (тега) по названию атрибута.
   *
   * @param elementVar переменная сценария, в которой сохранён html-элемент
   * @param varName переменная сценария для сохранения
   */
  @И("^из HTML-элемента \"(.+)\" сохранить значение атрибута \"(.+)\" в переменную \"(.+)\"$")
  public void getHtmlElementAttribute(String elementVar, String attributeKey, String varName) {
    elementVar = elementVar.replace("${", "").replace("}", "");
    var element = (Element) akitaScenario.getVar(elementVar);
    var value = getElementAttributeValue(element, processValue(attributeKey));
    akitaScenario.setVar(varName, value);
    akitaScenario.log(String.format("В переменную '%s' сохранено значение '%s'", varName, value));
  }
}
