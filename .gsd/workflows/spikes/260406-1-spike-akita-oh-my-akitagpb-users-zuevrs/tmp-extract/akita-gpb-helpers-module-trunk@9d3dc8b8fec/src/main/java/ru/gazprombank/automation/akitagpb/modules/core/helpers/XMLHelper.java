package ru.gazprombank.automation.akitagpb.modules.core.helpers;

import static ru.gazprombank.automation.akitagpb.modules.core.helpers.FileHelper.getFileFromResources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import lombok.SneakyThrows;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Класс содержит статические методы для получение различных данных из XMl файлов. */
public class XMLHelper {

  /**
   * Метод получает список {@link NodeList} всех тэгов из XML файла, которые имеют указанное имя.
   * <br>
   * После этого пытается получить значение указанного аттрибута у всех тэгов из списка и возвращает
   * первое найденное.<br>
   * Если нет тэгов с таким именем или аттрибутом - возвращается null.
   *
   * @param document XML document.
   * @param tagName имя тэга (если в имени присутствует namespace, его также надо указать, например,
   *     bpmn:process).
   * @param attributeName имя аттрибута тэга.
   * @return значение аттрибута тэга в виде {@link String} или null, если не удалось найти тэг или
   *     аттрибут с указанным именем.
   */
  public static String getXmlElementAttributeValue(
      Document document, String tagName, String attributeName) {
    NodeList list = getNodeListByTagName(document, tagName);
    for (Node node : iterable(list)) {
      if (node.getAttributes().getNamedItem(attributeName) != null) {
        return node.getAttributes().getNamedItem(attributeName).getNodeValue();
      }
    }
    return null;
  }

  /**
   * Метод получает список {@link NodeList} всех тэгов из XML файла, которые имеют указанное имя.
   * <br>
   * И возвращает значение первого найденного тэга.
   *
   * @param document XML Document.
   * @param tagName имя тэга (если в имени присутствует namespace, его также надо указать, например,
   *     bpmn:process).
   * @return значение тэга в виде {@link String} или null, если не удалось найти тэг с указанным
   *     именем.
   */
  public static String getElementValue(Document document, String tagName) {
    NodeList list = getNodeListByTagName(document, tagName);
    if (list.getLength() > 0) {
      return list.item(0).getNodeValue();
    }
    return null;
  }

  /**
   * Метод получает список {@link NodeList} всех тэгов из XML файла, которые имеют указанное имя.
   * <br>
   * И проверяет каждый полученный тэг, на соответсвие всем аттрибутам, указанным в массиве
   * attributes, с учётом, что каждый элемент массива это {@link String} в формате
   * "имя_аттрибута=значение_аттрибута". Любое другое значение будет игнорироваться.<br>
   * Метод вернёт первый тэг, который удовлевторяет условиям, или null.
   *
   * @param document XML document.
   * @param tagName имя тэга (если в имени присутствует namespace, его также надо указать, например,
   *     bpmn:process).
   * @param attributes массив строк ({@link String}), каждый элемент которого это строка в формате
   *     "имя_аттрибута=значение_аттрибута".
   * @return первый найденный тэг в виде {@link Node} или null, если ничего не было найдено.
   */
  public static Node getNodeByTagAndAttributes(
      Document document, String tagName, String... attributes) {
    Map<String, String> attrMap = new HashMap<>();
    Arrays.stream(attributes)
        .forEach(
            attribute -> {
              String[] keyValue = attribute.split("=");
              if (keyValue.length == 2) {
                attrMap.put(keyValue[0], keyValue[1]);
              }
            });
    return getNodeByTagAndAttributes(document, tagName, attrMap);
  }

  /**
   * Метод получает список {@link NodeList} всех тэгов из XML файла, которые имеют указанное имя.
   * <br>
   * И проверяет каждый полученный тэг, на соответсвием всем атрибутам, указанным в связном списке
   * attributes, с учётом, что ключ списка - это имя аттрибута, а значение списка - значение
   * аттрибута. <br>
   * Метод вернёт первый тэг, который удовлевторяет условиям, или null.
   *
   * @param document XML document.
   * @param tagName имя тэга (если в имени присутствует namespace, его также надо указать, например,
   *     bpmn:process).
   * @param attributes связный список, где ключ списка - имя атрибута (тип {@link String}), значение
   *     списка - значение аттрибута (тип {@link String}).
   * @return первый найденный тэг в виде {@link Node} или null, если ничего не было найдено.
   */
  public static Node getNodeByTagAndAttributes(
      Document document, String tagName, Map<String, String> attributes) {
    NodeList list = getNodeListByTagName(document, tagName);
    for (Node node : iterable(list)) {
      if (checkNodeByAttributes(node, attributes)) {
        return node;
      }
    }
    return null;
  }

  /**
   * Метод получает список {@link NodeList} всех тэгов из XML файла, которые имеют указанное имя.
   *
   * @param document XML document.
   * @param tagName имя тэга (если в имени присутствует namespace, его также надо указать, например,
   *     bpmn:process).
   * @return {@link NodeList} содержащий все найденные тэги.
   */
  public static NodeList getNodeListByTagName(Document document, String tagName) {
    Element rootElement = document.getDocumentElement();
    return rootElement.getElementsByTagName(tagName);
  }

  /**
   * Метод считывает XML файл и преобразовывает его в {@link Document}.<br>
   * Метод пробрасывает RuntimeException при ошибки открытия файла или ошибках преобразования файла.
   *
   * @param filePath путь до XML файла.
   * @return объект {@link Document}, содрежащий структуру XML файла.
   */
  public static Document parseXml(String filePath) {
    try {
      DocumentBuilderFactory factory = getNewInstanceDocumentBuilderFactory();
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(getFileFromResources(filePath));
    } catch (SAXException | ParserConfigurationException e) {
      throw new RuntimeException("Невозможно обработать XML файл: " + e.getMessage());
    } catch (IOException e) {
      throw new RuntimeException("Невозможно открыть XML файл: " + e.getMessage());
    }
  }

  /**
   * Метод считывает XML файл и преобразовывает его в {@link Document}.<br>
   * Метод пробрасывает RuntimeException при ошибки открытия файла или ошибках преобразования файла.
   *
   * @param inputStream поток содержащий XML данные.
   * @return объект {@link Document}, содрежащий структуру XML файла.
   */
  public static Document parseXml(InputStream inputStream) {
    try {
      DocumentBuilderFactory factory = getNewInstanceDocumentBuilderFactory();
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(inputStream);
    } catch (SAXException | ParserConfigurationException e) {
      throw new RuntimeException("Невозможно обработать XML файл: " + e.getMessage());
    } catch (IOException e) {
      throw new RuntimeException("Невозможно открыть XML файл: " + e.getMessage());
    }
  }

  /**
   * Метод проверяет, совпадают ли у переданного {@link Node} все аттрибуты и их значения со связным
   * списком attributes, с учётом, что ключ связнного списка - это имя аттрибута,а значение списка -
   * значение аттрибута.<br>
   * При первом расхождение метод возвращает false.<br>
   * Если расхождений не было найдено, метод возвращает true.
   *
   * @param node объект тэга.
   * @param attributes связный список, где ключ списка - имя атрибута (тип {@link String}), значение
   *     списка - значение аттрибута (тип {@link String}).
   * @return возвращает true, если все аттрибуты совпадают с указанными, или false если найдено
   *     расхождение.
   */
  private static boolean checkNodeByAttributes(Node node, Map<String, String> attributes) {
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      Node attributeNode = node.getAttributes().getNamedItem(entry.getKey());
      if (attributeNode == null) {
        return false;
      } else {
        if (!attributeNode.getNodeValue().equals(entry.getValue())) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Служебный метод, который нужен для реализации {@link Iterable<Node>} операций с {@link
   * NodeList}<br>
   * Возвращает {@link Iterable} объект, который можно применять в for, foreach и т.д.
   *
   * @param nodeList объект {@link NodeList}.
   * @return возвращает {@link Iterable<Node>} объект.
   */
  private static Iterable<Node> iterable(final NodeList nodeList) {
    return () ->
        new Iterator<>() {
          private int index = 0;

          @Override
          public boolean hasNext() {
            return index < nodeList.getLength();
          }

          @Override
          public Node next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            return nodeList.item(index++);
          }
        };
  }

  /**
   * Метод ищет по xpath и возвращает ноду
   *
   * @param document документ xml
   * @param xpath выражение
   * @return ноду из xml
   */
  @SneakyThrows
  public static Node findNodeByXpath(Document document, String xpath) {
    return (Node)
        XPathFactory.newInstance()
            .newXPath()
            .compile(xpath)
            .evaluate(document, XPathConstants.NODE);
  }

  /**
   * Метод ищет по xpath и возвращает список нод
   *
   * @param document документ xml
   * @param xpath выражение
   * @return список нод из xml
   */
  @SneakyThrows
  public static NodeList findNodeListByXpath(Document document, String xpath) {
    return (NodeList)
        XPathFactory.newInstance()
            .newXPath()
            .compile(xpath)
            .evaluate(document, XPathConstants.NODESET);
  }

  /**
   * Метод возвращает тело xml документа
   *
   * @param node документ xml
   * @return тело xml документа
   */
  @SneakyThrows
  public static <T extends Node> String getContentFromDocumentXml(T node) {
    DOMSource source = new DOMSource(node);
    Transformer transformer = getNewInstanceTransformerFactory().newTransformer();
    StreamResult result = new StreamResult(new StringWriter());
    transformer.transform(source, result);

    return result.getWriter().toString();
  }

  @SneakyThrows
  public static Element getElementByString(String text) {
    return getNewInstanceDocumentBuilderFactory()
        .newDocumentBuilder()
        .parse(new ByteArrayInputStream(text.getBytes()))
        .getDocumentElement();
  }

  /**
   * Метод для получения нового документа из Node xml
   *
   * @param node xml
   * @return объект Document
   */
  @SneakyThrows
  public static Document getNewDocumentFromNode(Node node) {
    DOMSource source = new DOMSource(node);
    Transformer transformer = getNewInstanceTransformerFactory().newTransformer();
    StreamResult result = new StreamResult(new StringWriter());
    transformer.transform(source, result);

    DocumentBuilderFactory factory = getNewInstanceDocumentBuilderFactory();
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(new InputSource(new StringReader(result.getWriter().toString())));
  }

  /**
   * Метод для создания DocumentBuilderFactory с установлеными параметрами для прохождения
   * безопасности
   *
   * @return DocumentBuilderFactory
   */
  @SneakyThrows
  private static DocumentBuilderFactory getNewInstanceDocumentBuilderFactory() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    // to be compliant, completely disable DOCTYPE declaration:
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    // or completely disable external entities declarations:
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    // or prohibit the use of all protocols by external entities:
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    // or disable entity expansion but keep in mind that this doesn't prevent fetching external
    // entities
    // and this solution is not correct for OpenJDK < 13 due to a bug:
    // https://bugs.openjdk.java.net/browse/JDK-8206132
    factory.setExpandEntityReferences(false);

    return factory;
  }

  /**
   * Метод для создания TransformerFactory с установлеными параметрами для прохождения безопасности
   *
   * @return TransformerFactory
   */
  @SneakyThrows
  private static TransformerFactory getNewInstanceTransformerFactory() {
    TransformerFactory factory =
        TransformerFactory.newInstance(
            "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", null);
    // to be compliant, prohibit the use of all protocols by external entities:
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

    return factory;
  }
}
