package ru.gazprombank.automation.akitagpb.modules.ccl.helpers;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HtmlHelper {

  /**
   * Получить текст из html-строки. Текст берётся из тега /html/body. Если вдруг данного тега в
   * html-строке найдено не будет - метод вернёт null.
   *
   * @param html html-строка
   * @return текст из html-строки из тега /html/body
   */
  public static String getTextFromHtml(String html) {
    var document = parseHtml(html);
    var body = getElementByXpath(document, "/html/body");
    return getElementWholeText(body);
  }

  /**
   * Получить объект Document из html-строки.
   *
   * @param html html-строка
   * @return Document
   */
  public static Document parseHtml(String html) {
    return Jsoup.parse(html);
  }

  /**
   * Получить html-элемент (тег) из html-строки по xpath.
   *
   * @param html html-строка
   * @param xpath xpath
   * @return html-элемент (тег)
   */
  public static Element getElementByXpath(String html, String xpath) {
    return getElementByXpath(parseHtml(html), xpath);
  }

  /**
   * Получить html-элементы (теги) из html-строки по xpath.
   *
   * @param html html-строка
   * @param xpath xpath
   * @return html-элементы (теги)
   */
  public static Elements getElementsByXpath(String html, String xpath) {
    return getElementsByXpath(parseHtml(html), xpath);
  }

  /**
   * Получить html-элемент (тег) из документа Document по xpath.
   *
   * @param document html-Document
   * @param xpath xpath
   * @return html-элемент (тег)
   */
  public static Element getElementByXpath(Document document, String xpath) {
    var elements = getElementsByXpath(document, xpath);
    if (elements.size() != 1) {
      throw new RuntimeException(
          String.format(
              "По переданному xpath [%s] найденное число элементов не равно 1 (%s)",
              xpath, elements.size()));
    }
    return elements.first();
  }

  /**
   * Получить html-элементы (теги) из документа Document по xpath.
   *
   * @param document html-Document
   * @param xpath xpath
   * @return html-элементы (теги)
   */
  public static Elements getElementsByXpath(Document document, String xpath) {
    return document.selectXpath(xpath);
  }

  /**
   * Получить текст элемента (включая текст в дочерних элементах).
   *
   * @param element html-элемент
   * @return текст элемента (включая текст в дочерних элементах)
   */
  public static String getElementText(Element element) {
    return element == null ? null : element.text();
  }

  /**
   * Получить собственный текст элемента (исключая текст в дочерних элементах).
   *
   * @param element html-элемент
   * @return собственный текст элемента (исключая текст в дочерних элементах)
   */
  public static String getElementOwnText(Element element) {
    return element == null ? null : element.ownText();
  }

  /**
   * Получить собственный текст элемента (исключая текст в дочерних элементах) ненормализованный (с
   * пробелами и переносами строк).
   *
   * @param element html-элемент
   * @return собственный текст элемента (исключая текст в дочерних элементах) ненормализованный (с
   *     пробелами и переносами строк)
   */
  public static String getElementWholeOwnText(Element element) {
    return element == null ? null : element.wholeOwnText();
  }

  /**
   * Получить текст элемента (включая текст в дочерних элементах) ненормализованный (с пробелами и
   * переносами строк).
   *
   * @param element html-элемент
   * @return текст элемента (включая текст в дочерних элементах) ненормализованный (с пробелами и
   *     переносами строк)
   */
  public static String getElementWholeText(Element element) {
    return element == null ? null : element.wholeText();
  }

  /**
   * Получить значение атрибута элемента по названию атрибута.
   *
   * @param element html-элемент
   * @param attributeKey название атрибута
   * @return значение атрибута
   */
  public static String getElementAttributeValue(Element element, String attributeKey) {
    return element.attr(attributeKey);
  }

  /**
   * Получить мапу ссылок из html-строки (текст ссылки из тега <a> - значение атрибута href) с
   * сохранеием порядка этих ссылок в тексте.
   *
   * @param html html-строка
   * @return ссылки из html-строки (текст ссылки из тега <a> - значение атрибута href)
   */
  public static Map<String, String> getTextsAndHrefsFromHyperlinks(String html) {
    Map<String, String> result = new LinkedHashMap<>();
    getElementsByXpath(parseHtml(html), "//a")
        .forEach(
            e -> {
              result.put(getElementOwnText(e), getElementAttributeValue(e, "href"));
            });
    return result;
  }
}
