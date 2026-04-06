package ru.gazprombank.automation.akitagpb.modules.ccl.email.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.И;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import microsoft.exchange.webservices.data.core.service.item.Item;
import org.assertj.core.api.Assertions;
import ru.gazprombank.automation.akitagpb.modules.ccl.email.exceptions.EmailException;
import ru.gazprombank.automation.akitagpb.modules.ccl.email.helpers.OutlookEmailHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.HtmlHelper;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

/** Класс для описания шагов для работы с почтой */
public class EmailSteps extends BaseMethods {

  /**
   * Ожидание получения письма с заданными параметрами на почту. Время ожидания указывается в шаге
   * или, в ином случае, дефолтное - из конфига "email.waiting.timeout.seconds". Письмо ищется
   * только среди последних N писем (параметр конфига "email.lastItemsFetchCount"). Параметры
   * таблицы: - Название папки - папка, в которой искать письмо (если не указать - по дефолту ищется
   * в папке "Входящие"). Письма ищутся только конкретно в этой папке, если нужное письмо находится
   * в какой-то подпапке - нужно передавать название именно этой подпапки. - Название родительской
   * папки - например - "Отправленные", "Удаленные" - нужно указывать, если письмо находится в
   * подпапке и не во "Входящих". - Тема содержит - подстрока, которую содержит тема письма. -
   * Сообщение содержит - подстрока, которую содержит само письмо. - Email отправителя - email-адрес
   * отправителя. - Прочитано - да, нет, true, false - искать письмо только среди прочитанных или
   * только среди непрочитанных. Если не указывать - будет искать среди всех писем. - Отметить как
   * прочитанное - да, нет, true, false - помечать все найденные письма как прочитанные. Если не
   * указывать - не будет помечать.
   *
   * <p>Пример: И на почту пришло сообщение с параметрами из таблицы | Параметр | Значение | |
   * Название папки | Gpbitpr_ccl_alert | | Тема содержит | abcde | | Сообщение содержит | 12345 | |
   * Сообщение содержит | 67890 | | Email отправителя | xx | | Прочитано | да |
   *
   * @param timeout - время ожидания письма
   * @param dataTable - таблица параметров
   */
  @И("^на почту пришло сообщение с параметрами из таблицы( в течение (\\d+) секунд)?$")
  public void receiveEmail(Integer timeout, DataTable dataTable) {
    List<Map<String, String>> params = prepareParamsFromDataTable(dataTable);
    var items = OutlookEmailHelper.getItemsWithTimeout(params, timeout);
    assertThat(items)
        .as(
            "Найдено писем: "
                + items.size()
                + ". Ожидалось найти только одно письмо по заданным параметрам")
        .hasSize(1);
  }

  /**
   * Ожидание не получения письма с заданными параметрами на почту. Время ожидания указывается в
   * шаге или, в ином случае, дефолтное - из конфига "email.waiting.timeout.seconds". Письмо ищется
   * только среди последних N писем (параметр конфига "email.lastItemsFetchCount"). Параметры
   * таблицы: - Название папки - папка, в которой искать письмо (если не указать - по дефолту ищется
   * в папке "Входящие"). Письма ищутся только конкретно в этой папке, если нужное письмо находится
   * в какой-то подпапке - нужно передавать название именно этой подпапки. - Название родительской
   * папки - например - "Отправленные", "Удаленные" - нужно указывать, если письмо находится в
   * подпапке и не во "Входящих". - Тема содержит - подстрока, которую содержит тема письма. -
   * Сообщение содержит - подстрока, которую содержит само письмо. - Email отправителя - email-адрес
   * отправителя. - Прочитано - да, нет, true, false - искать письмо только среди прочитанных или
   * только среди непрочитанных. Если не указывать - будет искать среди всех писем.
   *
   * <p>Пример: И на почту не пришло сообщение с параметрами из таблицы | Параметр | Значение | |
   * Название папки | Gpbitpr_ccl_alert | | Тема содержит | abcde | | Сообщение содержит | 12345 | |
   * Сообщение содержит | 67890 | | Email отправителя | xx | | Прочитано | да |
   *
   * @param timeout - время ожидания письма
   * @param dataTable - таблица параметров
   */
  @И("^на почту не пришло сообщение с параметрами из таблицы( в течение (\\d+) секунд)?$")
  public void notReceiveEmail(Integer timeout, DataTable dataTable) {
    List<Map<String, String>> params = prepareParamsFromDataTable(dataTable);
    List<Item> items;
    try {
      items = OutlookEmailHelper.getItemsWithTimeout(params, timeout);
    } catch (EmailException e) {
      return;
    }
    Assertions.fail(
        "Найдено писем: "
            + items.size()
            + ". Ожидалось, что по заданным параметрам писем не будет");
  }

  /**
   * Сохранить письмо с заданными параметрами в переменную. Этот шаг без какого-либо ожидания. Если
   * сразу письмо не будет найдено - шаг упадёт. Параметры такие же, как в шаге {@link
   * #receiveEmail(Integer, DataTable)}
   *
   * <p>Пример: И сохранить письмо с параметрами из таблицы в переменную "email" | Параметр |
   * Значение | | Тема содержит | abcde | | Сообщение содержит | 12345 | | email отправителя | xx |
   *
   * @param varName - имя переменной сценария
   * @param dataTable - таблица параметров
   */
  @И("^сохранить письмо с параметрами из таблицы в переменную \"(.*)\"$")
  public void saveEmail(String varName, DataTable dataTable) {
    List<Map<String, String>> params = prepareParamsFromDataTable(dataTable);
    var items = OutlookEmailHelper.getItems(params);
    assertThat(items)
        .as(
            "Найдено писем: "
                + items.size()
                + ". Ожидалось найти только одно письмо по заданным параметрам")
        .hasSize(1);
    akitaScenario.setVar(varName, items.get(0));
  }

  private List<Map<String, String>> prepareParamsFromDataTable(DataTable dataTable) {
    List<Map<String, String>> params = new ArrayList<>();
    dataTable
        .asMaps()
        .forEach(
            e -> {
              Map<String, String> map = new HashMap<>();
              map.put(OutlookEmailHelper.KEY, e.get("Параметр").toLowerCase());
              map.put(OutlookEmailHelper.VALUE, processValue(e.get("Значение")));
              params.add(map);
            });
    return params;
  }

  /**
   * Сохранить тему письма в переменную. Само письмо должно быть сохранено заранее в переменную,
   * которую нужно передать в этот шаг.
   *
   * @param emailVar - переменная, в которой сохранено письмо
   * @param varName - переменная для сохранения результата
   */
  @И("^сохранить тему письма \"(.*)\" в переменную \"(.*)\"$")
  public void saveEmailSubject(String emailVar, String varName) {
    emailVar = emailVar.replace("${", "").replace("}", "");
    var email = (Item) akitaScenario.getVar(emailVar);
    var emailSubject = OutlookEmailHelper.getEmailSubject(email);
    akitaScenario.setVar(varName, emailSubject);
    akitaScenario.log(String.format("Значение %s: %s", varName, emailSubject));
  }

  /**
   * Сохранить тело сообщения письма в переменную. Само письмо должно быть сохранено заранее в
   * переменную, которую нужно передать в этот шаг.
   *
   * @param emailVar - переменная, в которой сохранено письмо
   * @param varName - переменная для сохранения результата
   */
  @И("^сохранить (HTML-)?тело сообщения письма \"(.*)\" в переменную \"(.*)\"$")
  public void saveEmailBody(String isHtml, String emailVar, String varName) {
    emailVar = emailVar.replace("${", "").replace("}", "");
    var email = (Item) akitaScenario.getVar(emailVar);
    var emailBody =
        isHtml == null
            ? OutlookEmailHelper.getEmailBody(email)
            : OutlookEmailHelper.getEmailBodyHtml(email);
    akitaScenario.setVar(varName, emailBody);
    akitaScenario.log(String.format("Значение %s: %s", varName, emailBody));
  }

  /**
   * Сохранить файлы из вложения письма в переменную. Само письмо должно быть сохранено заранее в
   * переменную, которую нужно передать в этот шаг.
   *
   * @param emailVar - переменная, в которой сохранено письмо
   * @param varName - переменная для сохранения списка файлов
   */
  @И("^сохранить список файлов из вложения письма \"(.*)\" в переменную \"(.*)\"$")
  public void saveEmailAttachments(String emailVar, String varName) {
    emailVar = emailVar.replace("${", "").replace("}", "");
    var email = (Item) akitaScenario.getVar(emailVar);
    var files = OutlookEmailHelper.getEmailAttachments(email);
    akitaScenario.setVar(varName, files);
    akitaScenario.log(
        String.format("В переменную %s сохранён список файлов из вложения письма", varName));
  }

  /**
   * Шаг для проверки гиперссылок (соответствие текста ссылки с самим url'ом из атрибута href) в
   * письме, сообщение которых имеет html-формат, с учётом порядка ссылок в тексте. Пример: И
   * сообщения письма "${email}" содержит следующие ссылки: | Текст ссылки | Ссылка | | 1000743 |
   * https://ccl-release.xxx.xxx.gazprombank.ru/ebg/search/ebgRequests |
   *
   * @param emailOrEmailBodyVar переменная, в которой сохранено письмо или тело сообщения письма
   * @param dataTable таблица параметров с ожидаемыми ссылками
   */
  @И("^сообщение письма \"(.*)\" содержит следующие ссылки:?$")
  public void assertEmailBodyHtmlContainsHyperlinks(
      String emailOrEmailBodyVar, DataTable dataTable) {
    emailOrEmailBodyVar = emailOrEmailBodyVar.replace("${", "").replace("}", "");
    var emailOrEmailBody = akitaScenario.getVar(emailOrEmailBodyVar);
    var html =
        emailOrEmailBody.getClass() == String.class
            ? (String) emailOrEmailBody
            : OutlookEmailHelper.getEmailBodyHtml((Item) emailOrEmailBody);
    var actualLinks = HtmlHelper.getTextsAndHrefsFromHyperlinks(html);

    var maps = dataTable.asMaps();
    final Map<String, String> expectedLinks =
        new LinkedHashMap<>(); // новая мапа - для сохранения порядка параметров в изначальной
    // таблице
    maps.forEach(
        map ->
            expectedLinks.put(
                processValue(map.get("Текст ссылки")), processValue(map.get("Ссылка"))));

    akitaScenario.log("Ожидаемые ссылки:\n" + expectedLinks);
    akitaScenario.log("Фактические ссылки:\n" + actualLinks);
    assertThat(actualLinks).as("").containsExactlyEntriesOf(expectedLinks);
  }
}
