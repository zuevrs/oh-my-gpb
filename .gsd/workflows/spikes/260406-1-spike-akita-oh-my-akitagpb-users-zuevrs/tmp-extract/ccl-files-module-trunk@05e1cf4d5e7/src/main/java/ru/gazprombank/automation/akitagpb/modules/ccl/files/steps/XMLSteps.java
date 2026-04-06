package ru.gazprombank.automation.akitagpb.modules.ccl.files.steps;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

import io.cucumber.java.ru.И;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import lombok.SneakyThrows;
import org.apache.commons.text.StringEscapeUtils;
import org.assertj.core.api.Assertions;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.XMLHelper;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class XMLSteps extends BaseMethods {

  /**
   * Получает значение тэга из XML фалйа по имени тэга и сохраняет его в переменную Акита.
   *
   * @param xmlFile путь для XML фалйа.
   * @param tagName имя тэга.
   * @param varName имя переменной Акита, в которой будет сохранён результат.
   */
  @И("^из XML файла \"(.+)\" получить значение тэга \"(.+)\" и сохранить в переменную \"(.+)\"$")
  public void getElementValueAndSave(String xmlFile, String tagName, String varName) {
    String filePath = resolveVariables(xmlFile);
    Document document = XMLHelper.parseXml(filePath);
    String value = XMLHelper.getElementValue(document, tagName);

    Assertions.assertThat(value).as("Значение тэга %s - null!", tagName).isNotNull();
    akitaScenario.setVar(varName, value);
  }

  /**
   * Получаем значение атрибута тэга из XML фалйа и сохраняем в переменную Акита.<br>
   * Тэг выбирается по имени.
   *
   * @param xmlFile путь доя XML файла.
   * @param attrName имя атрибута тэга.
   * @param tagName имя тэга.
   * @param varName имя переменной Акита, в которую будет сохранён резуьтат.
   */
  @И(
      "^из XML файла \"(.+)\" получить значение атрибута \"(.+)\" тэга \"(.+)\" и сохранить в переменную \"(.+)\"$")
  public void getElementAttributeValueAndSave(
      String xmlFile, String attrName, String tagName, String varName) {
    String filePath = resolveVariables(xmlFile);
    Document document = XMLHelper.parseXml(filePath);
    String value = XMLHelper.getXmlElementAttributeValue(document, tagName, attrName);
    Assertions.assertThat(value)
        .as("Значение атрибута %s тэга %s - null!", attrName, tagName)
        .isNotNull();
    akitaScenario.setVar(varName, value);
  }

  /**
   * Получаем все теги из XML файла, которые имеют указанное имя и сохраняем в переменную Акита.<br>
   * Результат имеет тип {@link NodeList}.
   *
   * @param xmlFile путь до XML файла.
   * @param tagName имя тэга.
   * @param varName имя переменной Акита, в которую сохранится результат.
   */
  @И(
      "^из XML файла \"(.+)\" получить список тэгов по имени \"(.+)\" и сохранить в переменную \"(.+)\"$")
  public void getElementByNameAndSave(String xmlFile, String tagName, String varName) {
    String filePath = resolveVariables(xmlFile);
    Document document = XMLHelper.parseXml(filePath);
    akitaScenario.setVar(varName, XMLHelper.getNodeListByTagName(document, tagName));
  }

  /**
   * Плучаем первый найденный тэг с указанным именем, который имеет аттрибуты и их значения из
   * таблица под шагом.<br>
   * Рузкльтат сохраняется в переменную Акита.
   *
   * @param xmlFile путь до XMl файла.
   * @param tagName имя тега.
   * @param varName имя переменной Акита, в которую сохранится результат.
   * @param attributes таблица аттрибутов в виде связного списка, где ключ это имя тэга, а значение
   *     - значение атрибута.
   */
  @И(
      "^из XML файла \"(.+)\" получить тэг по имени \"(.+)\" с атрибутами из таблицы и сохранить в переменную \"(.+)\"$")
  public void getElementByNameAndParamsAndSave(
      String xmlFile, String tagName, String varName, Map<String, String> attributes) {
    String filePath = resolveVariables(xmlFile);
    Document document = XMLHelper.parseXml(filePath);
    Node node = XMLHelper.getNodeByTagAndAttributes(document, tagName, attributes);
    Assertions.assertThat(node)
        .as(
            "По заданному имени и атрибутам тэг не найден!\nИмя: %s\nАттрибуты: %s",
            tagName, attributes)
        .isNotNull();
    akitaScenario.setVar(varName, node);
  }

  /**
   * Получаем значение тэга из сохранённого в переменной Акита объект.<br>
   * Объект должен быть типа {@link Node}.<br>
   * Результат сохраняем в переменную Акита.
   *
   * @param tagVarName имя переменной Акита, содержащей объект с тэгом.
   * @param varName имя переменой Акита, в которую сохранится результат.
   */
  @И("^из XML тэга \"(.+)\" получить значение и сохранить в переменную \"(.+)\"$")
  public void getElementValueFromVar(String tagVarName, String varName) {
    Node node = (Node) akitaScenario.getVar(tagVarName);
    String value = node.getNodeValue();
    Assertions.assertThat(value)
        .as("Значение тэга из переменной %s - null!", tagVarName)
        .isNotNull();
    akitaScenario.setVar(varName, value);
  }

  /**
   * Получаем значение атрибута тэга из сохранённого в переменной Акита объект.<br>
   * Объект должен быть типа {@link Node}.<br>
   * Результат сохраняем в переменную Акита.
   *
   * @param tagVarName имя переменной Акита, содержащей объект с тэгом.
   * @param attrName имя атрибута, значение которого надо получить.
   * @param varName имя переменой Акита, в которую сохранится результат.
   */
  @И("^из XML тэга \"(.+)\" получить значение атрибута \"(.+)\" и сохранить в переменную \"(.+)\"$")
  public void getElementAttributeValueFromVar(String tagVarName, String attrName, String varName) {
    Node node = (Node) akitaScenario.getVar(tagVarName);
    Node attributeNode = node.getAttributes().getNamedItem(attrName);
    Assertions.assertThat(attributeNode).as("Аттрибут %s не найден у тэга", attrName).isNotNull();
    akitaScenario.setVar(varName, attributeNode.getNodeValue());
  }

  /**
   * Шаг получает массив из xml и сохраняте в хранилище
   *
   * @param xmlVar строка или файл xml
   * @param xmlPath значение для поиска
   * @param varName имя переменной в которую сохраняется значение
   */
  @SneakyThrows
  @И(
      "^значения из xml (?:строки|файла) \"(.*)\", найденные по xmlpath \"(.*)\", сохранены в виде массива в переменную \"(.*)\"$")
  public void getValuesFromXmlAsString(String xmlVar, String xmlPath, String varName) {
    var strXml = processValue(xmlVar);

    var dbf = DocumentBuilderFactory.newInstance();
    dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

    var db = dbf.newDocumentBuilder();
    var doc = db.parse(new InputSource(new StringReader(strXml)));

    xmlPath = resolveVariables(xmlPath);

    List<String> list = new ArrayList<>();
    NodeList nodeList =
        (NodeList)
            XPathFactory.newInstance()
                .newXPath()
                .compile(xmlPath)
                .evaluate(doc, XPathConstants.NODESET);
    for (int i = 0; i < nodeList.getLength(); i++) {
      list.add(
          nodeList.item(i).getFirstChild() == null
              ? nodeList.item(i).getNodeValue()
              : nodeList.item(i).getFirstChild().getNodeValue());
    }

    akitaScenario.setVar(varName, list);
    akitaScenario.log(String.format("В переменную '%s' сохранено значение '%s'", varName, list));
  }

  /**
   * Шаг получает xml строку и экранирует её (<> -> &lt;&gt;)
   *
   * @param str строка или переменная, которую требуется преобразовать
   * @param varName имя переменной, в которую сохраняется преобразованное значение
   */
  @И("^преобразовать XML строку \"(.*)\" и сохранить в переменную \"(.*)\"$")
  public void xmlEscape(String str, String varName) {
    String result = StringEscapeUtils.escapeXml11(processValue(str));
    akitaScenario.setVar(varName, result);
  }
}
