package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.date;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.XMLHelper;

/**
 * Класс описания прздничных дней (если в ресурсах есть файл календаря, то загружает данные по нему,
 * для остальных годов берётся значения по умолчанию)
 */
public class CalendarHolidays {

  private static CalendarHolidays instance;
  private final Map<Integer, List<LocalDate>> holidaysMap = new HashMap<>();
  private final Map<Integer, List<LocalDate>> transitionsDaysMap = new HashMap<>();

  private CalendarHolidays() {
    init();
  }

  public static CalendarHolidays getInstance() {
    if (instance == null) {
      instance = new CalendarHolidays();
    }
    return instance;
  }

  /** Проходимся по списку справочников календарей и записываем праздничные дни в holidaysMap */
  @SneakyThrows
  private void init() {
    Set<String> resourceList =
        new Reflections("dictionary", Scanners.Resources).getResources(".*\\.xml");

    resourceList.forEach(
        path -> {
          InputStream inputStream = this.getClass().getResourceAsStream("/" + path);
          Document document = XMLHelper.parseXml(inputStream);
          String year = document.getDocumentElement().getAttribute("year");

          List<LocalDate> holidays = getLocalDateListByXpath(document, "//day[@t='1']/@d");
          holidaysMap.put(Integer.parseInt(year), holidays);

          List<LocalDate> transitionsDays = getLocalDateListByXpath(document, "//day/@f");
          transitionsDaysMap.put(Integer.parseInt(year), transitionsDays);
        });
  }

  /**
   * Метод получения списка дат по xpath из справочника
   *
   * @param document справочника
   * @param xpath выражение
   * @return список дат
   */
  private List<LocalDate> getLocalDateListByXpath(Document document, String xpath) {
    String year = document.getDocumentElement().getAttribute("year");
    try {
      NodeList holidaysList =
          (NodeList)
              XPathFactory.newInstance()
                  .newXPath()
                  .compile(xpath)
                  .evaluate(document, XPathConstants.NODESET);

      return IntStream.range(0, holidaysList.getLength())
          .mapToObj(holidaysList::item)
          .map(node -> String.format("%s.%s", node.getNodeValue(), year))
          .map(holiday -> LocalDate.parse(holiday, DateTimeFormatter.ofPattern("MM.dd.yyyy")))
          .collect(Collectors.toList());

    } catch (XPathExpressionException e) {
      Assertions.fail("Ошибка разбора справочника праздничных дней");
      return Collections.emptyList();
    }
  }

  /**
   * Получение списка праздничных дней
   *
   * @param year год за который получаем список
   * @return список праздничных дней
   */
  public List<LocalDate> getHolidays(int year) {
    List<LocalDate> holidays = holidaysMap.get(year);

    if (holidays == null) {
      return getDefaultHolidays(year);
    }

    return holidays;
  }

  /**
   * Получение списка праздничных дней по умолчанию
   *
   * @param year год за который получаем список
   * @return список праздничных дней
   */
  private List<LocalDate> getDefaultHolidays(int year) {
    List<LocalDate> holidays = new ArrayList<>();
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    holidays.add(LocalDate.parse("01.01." + year, dateFormatter));
    holidays.add(LocalDate.parse("02.01." + year, dateFormatter));
    holidays.add(LocalDate.parse("03.01." + year, dateFormatter));
    holidays.add(LocalDate.parse("04.01." + year, dateFormatter));
    holidays.add(LocalDate.parse("05.01." + year, dateFormatter));
    holidays.add(LocalDate.parse("06.01." + year, dateFormatter));
    holidays.add(LocalDate.parse("07.01." + year, dateFormatter));
    holidays.add(LocalDate.parse("08.01." + year, dateFormatter));
    holidays.add(LocalDate.parse("23.02." + year, dateFormatter));
    holidays.add(LocalDate.parse("08.03." + year, dateFormatter));
    holidays.add(LocalDate.parse("01.05." + year, dateFormatter));
    holidays.add(LocalDate.parse("09.05." + year, dateFormatter));
    holidays.add(LocalDate.parse("12.06." + year, dateFormatter));
    holidays.add(LocalDate.parse("04.11." + year, dateFormatter));

    return holidays;
  }

  /**
   * Получение мапы праздничных дней, гду key - год , value - список праздничных дней
   *
   * @return мапа праздничных дней
   */
  public Map<Integer, List<LocalDate>> getHolidaysMap() {
    return holidaysMap;
  }

  /**
   * Получение списка перенесенных рабочих дней
   *
   * @param year год за который получаем список
   * @return список праздничных дней
   */
  public List<LocalDate> getTransitionsDays(int year) {
    List<LocalDate> workdays = transitionsDaysMap.get(year);

    if (workdays == null) {
      return new ArrayList<>();
    }

    return workdays;
  }

  /**
   * Получение мапы перенесенных рабочих дней, гду key - год , value - список дней
   *
   * @return мапа рабочих дней
   */
  public Map<Integer, List<LocalDate>> getTransitionsDaysMap() {
    return transitionsDaysMap;
  }
}
