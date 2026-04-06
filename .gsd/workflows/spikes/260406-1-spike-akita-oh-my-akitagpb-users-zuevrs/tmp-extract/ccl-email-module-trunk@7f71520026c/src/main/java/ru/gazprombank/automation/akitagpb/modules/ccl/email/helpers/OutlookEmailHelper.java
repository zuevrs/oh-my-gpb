package ru.gazprombank.automation.akitagpb.modules.ccl.email.helpers;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.getBooleanFromString;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.BasePropertySet;
import microsoft.exchange.webservices.data.core.enumeration.property.BodyType;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.search.SortDirection;
import microsoft.exchange.webservices.data.core.enumeration.service.ConflictResolutionMode;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.property.complex.FileAttachment;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.property.complex.MessageBody;
import microsoft.exchange.webservices.data.search.FindFoldersResults;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import microsoft.exchange.webservices.data.search.FolderView;
import microsoft.exchange.webservices.data.search.ItemView;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gazprombank.automation.akitagpb.modules.ccl.email.exceptions.EmailException;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.HtmlHelper;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.date.DateHelper;
import ru.gazprombank.automation.akitagpb.modules.core.cucumber.api.AkitaScenario;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.ConfigLoader;

/** Утилитный класс для работы с почтой */
public class OutlookEmailHelper {

  public static final String KEY = "параметр";
  public static final String VALUE = "значение";
  public static final String FOLDER_NAME_PARAM = "название папки";
  public static final String PARENT_FOLDER_NAME_PARAM = "название родительской папки";
  public static final String SUBJECT_CONTAINS_TEXT_PARAM = "тема содержит";
  public static final String BODY_CONTAINS_TEXT_PARAM = "сообщение содержит";
  public static final String SENDER_EMAIL_PARAM = "email отправителя";
  public static final String IS_READ_PARAM = "прочитано";
  public static final String READ_AFTER_PARAM = "отметить как прочитанное";

  private static final Logger LOGGER = LoggerFactory.getLogger(OutlookEmailHelper.class);

  private static final String OUTLOOK_SERVER_HOST =
      ConfigLoader.getConfigValue("global.email.host");
  private static final String EMAIL_ADDRESS = ConfigLoader.getConfigValue("global.email.address");
  private static final String EMAIL_USER = ConfigLoader.getConfigValue("global.email.xxx");
  private static final String EMAIL_PASSWORD = ConfigLoader.getConfigValue("global.email.password");
  private static final Integer FETCH_COUNT =
      ConfigLoader.getConfigValueOrDefault("global.email.lastItemsFetchCount", 20);
  private static final Integer WAITING_TIMEOUT_SECONDS =
      ConfigLoader.getConfigValueOrDefault("global.email.waiting.timeout.seconds", 300);
  private static final Integer WAITING_POLL_INTERVAL_SECONDS =
      ConfigLoader.getConfigValueOrDefault("global.email.waiting.pollInterval.seconds", 5);

  private static final ExchangeService EXCHANGE_SERVICE;
  private static Integer CUSTOM_TIMEOUT_SECONDS;

  static {
    EXCHANGE_SERVICE = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
    EXCHANGE_SERVICE.setTraceEnabled(true);
    ExchangeCredentials credentials = new WebCredentials(EMAIL_USER, EMAIL_PASSWORD);
    EXCHANGE_SERVICE.setCredentials(credentials);
    var url = String.format("https://%s/ews/exchange.asmx", OUTLOOK_SERVER_HOST);
    try {
      EXCHANGE_SERVICE.setUrl(new URI(url));
    } catch (URISyntaxException e) {
      throw new RuntimeException("Ошибка в url-адресе - " + url, e);
    }
    EXCHANGE_SERVICE.getHttpHeaders().put("X-AnchorMailbox", EMAIL_ADDRESS);
  }

  /**
   * Отправить письмо на почту. Пример: sendEmail("тема письма", "<a href=\"ref\">ссылка</a>",
   * "xx");
   *
   * @param subject тема письма
   * @param body тело письма
   * @param recipients email-адреса получателей
   */
  public static void sendEmail(String subject, String body, String... recipients) {
    try {
      EmailMessage email = new EmailMessage(EXCHANGE_SERVICE);
      email.setSubject(subject);
      email.setBody(new MessageBody(body));
      email.getToRecipients().addSmtpAddressRange(List.of(recipients).iterator());
      email.send();
    } catch (Exception e) {
      throw new RuntimeException("Ошибка при отправке письма на почту", e);
    }
  }

  /**
   * Получить письма с заданными параметрами на почту с ожиданием. Ключи таблицы: - Название папки -
   * папка, в которой искать письмо (если не указать - по дефолту ищется в папке "Входящие"). Письма
   * ищутся только конкретно в этой папке, если нужное письмо находится в какой-то подпапке - нужно
   * передавать название именно этой подпапки. - Название родительской папки - например -
   * "Отправленные", "Удаленные" - нужно указывать, если письмо находится в подпапке и не во
   * "Входящих". - Тема содержит - подстрока, которую содержит тема письма. - Сообщение содержит -
   * подстрока, которую содержит само письмо. - Email отправителя - email-адрес отправителя. -
   * Прочитано - да, нет, true, false - искать письмо только среди прочитанных или только среди
   * непрочитанных. Если не указывать - будет искать среди всех писем. - Отметить как прочитанное -
   * да, нет, true, false - помечать все найденные письма как прочитанные. Если не указывать - не
   * будет помечать.
   *
   * @param searchFilterParams параметры поиска писем
   * @param timeout время ожидания (если null - будет дефолтный таймаут)
   * @return список найденных писем
   */
  public static List<Item> getItemsWithTimeout(
      List<Map<String, String>> searchFilterParams, Integer timeout) throws EmailException {
    String folderName =
        searchFilterParams.stream()
            .filter(e -> FOLDER_NAME_PARAM.equals(e.get(KEY)))
            .findFirst()
            .orElse(Map.of())
            .get(VALUE);
    String parentFolderName =
        searchFilterParams.stream()
            .filter(e -> PARENT_FOLDER_NAME_PARAM.equals(e.get(KEY)))
            .findFirst()
            .orElse(Map.of())
            .get(VALUE);
    Boolean readAfter =
        getBooleanFromString(
            searchFilterParams.stream()
                .filter(e -> READ_AFTER_PARAM.equals(e.get(KEY)))
                .findFirst()
                .orElse(Map.of())
                .get(VALUE));
    return getItemsWithTimeout(
        folderName, parentFolderName, searchFilterParams, timeout, readAfter);
  }

  /**
   * Получить письма с заданными параметрами на почту без ожидания. Ключи таблицы такие же, как в
   * методе {@link #getItemsWithTimeout(List, Integer)}
   *
   * @param searchFilterParams параметры поиска писем
   * @return список найденных писем
   */
  public static List<Item> getItems(List<Map<String, String>> searchFilterParams) {
    String folderName =
        searchFilterParams.stream()
            .filter(e -> FOLDER_NAME_PARAM.equals(e.get(KEY)))
            .findFirst()
            .orElse(Map.of())
            .get(VALUE);
    String parentFolderName =
        searchFilterParams.stream()
            .filter(e -> PARENT_FOLDER_NAME_PARAM.equals(e.get(KEY)))
            .findFirst()
            .orElse(Map.of())
            .get(VALUE);
    Boolean readAfter =
        getBooleanFromString(
            searchFilterParams.stream()
                .filter(e -> READ_AFTER_PARAM.equals(e.get(KEY)))
                .findFirst()
                .orElse(Map.of())
                .get(VALUE));
    var items = getItems(folderName, parentFolderName, searchFilterParams);
    if (readAfter) {
      items.forEach(OutlookEmailHelper::setEmailIsRead);
    }
    return items;
  }

  /**
   * Получить тему письма.
   *
   * @param email письмо (объект класса Item)
   * @return тема письма
   */
  public static String getEmailSubject(Item email) {
    try {
      return email.getSubject();
    } catch (ServiceLocalException exception) {
      return null;
    }
  }

  /**
   * Получить дату получения письма.
   *
   * @param email письмо (объект класса Item)
   * @return дата получения письма
   */
  public static String getEmailDateTimeReceived(Item email) {
    try {
      return DateHelper.formatDate(email.getDateTimeReceived(), "HH:mm:ss dd-MM-yyyy");
    } catch (ServiceLocalException exception) {
      return null;
    }
  }

  /**
   * Получить тело сообщения письма. Если содержимое сообщения - HTML - метод вернёт текст из этого
   * HTML'а.
   *
   * @param email письмо (объект класса Item)
   * @return тело сообщения письма
   */
  public static String getEmailBody(Item email) {
    try {
      var body = email.getBody().toString();
      return email.getBody().getBodyType() == BodyType.HTML
          ? HtmlHelper.getTextFromHtml(body)
          : body;
    } catch (ServiceLocalException exception) {
      return null;
    }
  }

  /**
   * Получить исходное html-содержимое сообщения письма, если тип содержимого - HTML. Если
   * содержимое - обычный текст - будет выброшено исключение.
   *
   * @param email письмо (объект класса Item)
   * @return исходное html-содержимое сообщения письма
   */
  public static String getEmailBodyHtml(Item email) {
    try {
      if (email.getBody().getBodyType() != BodyType.HTML) {
        throw new RuntimeException("Сообщение данного письма имеет тип 'Text', а не 'HTML'");
      }
      return email.getBody().toString();
    } catch (ServiceLocalException exception) {
      return null;
    }
  }

  /**
   * Получить список файлов из вложения письма.
   *
   * @param email письмо (объект класса Item)
   * @return список файлов из вложения письма
   */
  public static List<File> getEmailAttachments(Item email) {
    try {
      List<File> files = new ArrayList<>();
      var attachments =
          email.getAttachments().getItems().stream()
              .filter(e -> e instanceof FileAttachment)
              .map(e -> (FileAttachment) e)
              .toList();
      LOGGER.debug("Во вложении письма найдено файлов: " + attachments.size());

      Path directory =
          Paths.get(
              System.getProperty("user.dir"),
              "/build/email/",
              AkitaScenario.getInstance().getScenario().getId());
      if (!attachments.isEmpty()) {
        Files.createDirectories(directory);
        attachments.forEach(
            e -> {
              var file = Paths.get(directory.toString(), e.getName()).toFile();
              try {
                e.load(file.getAbsolutePath());
              } catch (Exception exception) {
                throw new RuntimeException(
                    "Возникла ошибка при скачивании вложения из письма", exception);
              }
              LOGGER.debug("Получен файл из вложения письма:\n" + file.getAbsolutePath());
              files.add(file);
            });
      }
      return files;
    } catch (ServiceLocalException | IOException e) {
      throw new RuntimeException("Ошибка при получении вложения из письма", e);
    }
  }

  /**
   * Получить адрес отправителя письма.
   *
   * @param email письмо (объект класса Item)
   * @return адрес отправителя письма
   */
  public static String getEmailFromAddress(Item email) {
    try {
      return ((EmailMessage) email).getFrom().getAddress();
    } catch (ServiceLocalException exception) {
      return null;
    }
  }

  /**
   * Получить флаг, прочитано ли письмо.
   *
   * @param email письмо (объект класса Item)
   * @return флаг, прочитано ли письмо
   */
  public static Boolean getEmailIsRead(Item email) {
    try {
      return ((EmailMessage) email).getIsRead();
    } catch (ServiceLocalException exception) {
      return null;
    }
  }

  /**
   * Пометить письмо прочитанным.
   *
   * @param email письмо (объект класса Item)
   */
  public static void setEmailIsRead(Item email) {
    try {
      ((EmailMessage) email).setIsRead(true);
      email.update(ConflictResolutionMode.AlwaysOverwrite);
    } catch (Exception e) {
      throw new RuntimeException("Не смогли отметить письмо как прочитанное", e);
    }
  }

  /**
   * Получить письма с заданными параметрами на почту с ожиданием.
   *
   * @param folderName название папки, в которой ищется письмо (если null - поиск будет в дефолтной
   *     папке - "Входящие")
   * @param parentFolderName например - "Отправленные", "Удаленные" - нужно указывать, если письмо
   *     находится в подпапке и не во "Входящих".
   * @param searchFilterParams параметры поиска писем
   * @param timeout время ожидания (если null - будет дефолтный таймаут)
   * @return письма с заданными параметрами
   */
  private static List<Item> getItemsWithTimeout(
      String folderName,
      String parentFolderName,
      List<Map<String, String>> searchFilterParams,
      Integer timeout,
      Boolean readAfter)
      throws EmailException {
    CUSTOM_TIMEOUT_SECONDS = timeout == null ? WAITING_TIMEOUT_SECONDS : timeout;
    AtomicReference<List<Item>> result = new AtomicReference<>();
    try {
      waitEmail()
          .until(
              () -> {
                result.set(getItems(folderName, parentFolderName, searchFilterParams));
                return result.get().size() > 0;
              });
    } catch (ConditionTimeoutException e) {
      throw new EmailException(
          "По заданным параметрам не найдено ни одного письма за указанное время", e);
    }
    List<Item> items = result.get();
    if (readAfter) {
      items.forEach(OutlookEmailHelper::setEmailIsRead);
    }
    return items;
  }

  /**
   * Получить письма с заданными параметрами на почту без ожидания.
   *
   * @param folderName название папки, в которой ищется письмо (если null - поиск будет в дефолтной
   *     папке - "Входящие")
   * @param parentFolderName например - "Отправленные", "Удаленные" - нужно указывать, если письмо
   *     находится в подпапке и не во "Входящих".
   * @param searchFilterParams параметры поиска писем
   * @return письма с заданными параметрами
   */
  @SneakyThrows
  private static List<Item> getItems(
      String folderName, String parentFolderName, List<Map<String, String>> searchFilterParams) {
    ItemView view = new ItemView(FETCH_COUNT);
    view.getOrderBy().add(ItemSchema.DateTimeReceived, SortDirection.Descending);
    view.setPropertySet(
        new PropertySet(
            BasePropertySet.FirstClassProperties, ItemSchema.Subject, ItemSchema.DateTimeReceived));
    FindItemsResults<Item> findResults;
    findResults =
        EXCHANGE_SERVICE.findItems(getFolderIdByFolderName(folderName, parentFolderName), view);
    if (!findResults.getItems().isEmpty()) {
      EXCHANGE_SERVICE.loadPropertiesForItems(findResults, PropertySet.FirstClassProperties);
    }
    var result = filterEmailsByParams(findResults.getItems(), searchFilterParams);

    LOGGER.debug("Найдено писем: " + result.size());
    if (result.size() > 0) {
      result.forEach(
          e ->
              LOGGER.debug(
                  String.format(
                      "\nТема: %s \nПолучено: %s",
                      getEmailSubject(e), getEmailDateTimeReceived(e))));
    }
    return result;
  }

  private static ConditionFactory waitEmail() {
    return Awaitility.await()
        .pollInSameThread()
        .timeout(CUSTOM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .pollInterval(WAITING_POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  @SneakyThrows
  private static FolderId getFolderIdByFolderName(String folderName, String parentFolderName) {
    if (folderName == null
        || List.of("входящие", "исходящие", "удаленные", "черновики", "отправленные", "root")
            .contains(folderName.toLowerCase())) {
      return new FolderId(getStandardFolderByName(folderName));
    }
    FindFoldersResults foldersResults =
        EXCHANGE_SERVICE.findFolders(
            getStandardFolderByName(parentFolderName), new FolderView(1000));
    return foldersResults.getFolders().stream()
        .filter(
            e -> {
              try {
                return e.getDisplayName().equals(folderName);
              } catch (ServiceLocalException exception) {
                throw new RuntimeException(
                    "Ошибка при поиске папки по имени " + folderName, exception);
              }
            })
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Папка с именем '" + folderName + "' не найдена"))
        .getId();
  }

  private static WellKnownFolderName getStandardFolderByName(String folderName) {
    folderName = folderName == null ? "входящие" : folderName.toLowerCase();
    return switch (folderName) {
      case "исходящие" -> WellKnownFolderName.Outbox;
      case "удаленные" -> WellKnownFolderName.DeletedItems;
      case "черновики" -> WellKnownFolderName.Drafts;
      case "отправленные" -> WellKnownFolderName.SentItems;
      case "root" -> WellKnownFolderName.MsgFolderRoot;
      default -> WellKnownFolderName.Inbox;
    };
  }

  private static List<Item> filterEmailsByParams(
      List<Item> source, List<Map<String, String>> searchFilterParams) {
    return source.stream()
        .filter(
            e -> {
              var subject = Objects.requireNonNull(getEmailSubject(e));
              for (var param : searchFilterParams) {
                switch (param.get(KEY)) {
                  case SUBJECT_CONTAINS_TEXT_PARAM -> {
                    if (!subject.toLowerCase().contains(param.get(VALUE).toLowerCase())) {
                      LOGGER.debug(
                          String.format(
                              "%nТема письма '%s' не содержит ожидаемую подстроку '%s'",
                              subject, param.get(VALUE)));
                      return false;
                    }
                  }
                  case BODY_CONTAINS_TEXT_PARAM -> {
                    var body = Objects.requireNonNull(getEmailBody(e));
                    var actual =
                        body.toLowerCase()
                            .replaceAll("\r\n", " ")
                            .replaceAll("\n", " ")
                            .replaceAll("\\s{2,}", " ");
                    var expected =
                        param
                            .get(VALUE)
                            .toLowerCase()
                            .replaceAll("\r\n", " ")
                            .replaceAll("\n", " ")
                            .replaceAll("\\s{2,}", " ");
                    if (!actual.contains(expected)) {
                      LOGGER.debug(
                          String.format(
                              "%nСообщение письма '%s':%n%s%nне содержит ожидаемую подстроку:%n'%s'",
                              subject, body, param.get(VALUE)));
                      return false;
                    }
                  }
                  case SENDER_EMAIL_PARAM -> {
                    var addressFrom = Objects.requireNonNull(getEmailFromAddress(e));
                    if (!addressFrom.equalsIgnoreCase(param.get(VALUE))) {
                      LOGGER.debug(
                          String.format(
                              "%nАдрес отправителя письма '%s' [%s] не равен ожидаемому адресу [%s]",
                              subject, addressFrom, param.get(VALUE)));
                      return false;
                    }
                  }
                  case IS_READ_PARAM -> {
                    if (!Objects.requireNonNull(getEmailIsRead(e))
                        .equals(getBooleanFromString(param.get(VALUE)))) {
                      LOGGER.debug(
                          String.format(
                              "%nПисьмо '%s' не соответствует ожидаемому параметру [Прочитано: %s]",
                              subject, param.get(VALUE)));
                      return false;
                    }
                  }
                  case FOLDER_NAME_PARAM, PARENT_FOLDER_NAME_PARAM, READ_AFTER_PARAM -> {
                    // do nothing
                  }
                  default -> throw new RuntimeException(
                      String.format(
                          "Неизвестный параметр - '%s'\nДопустимые параметры поиска: %s, %s, %s, %s",
                          param.get(KEY),
                          SUBJECT_CONTAINS_TEXT_PARAM,
                          BODY_CONTAINS_TEXT_PARAM,
                          SENDER_EMAIL_PARAM,
                          IS_READ_PARAM));
                }
              }
              return true;
            })
        .collect(Collectors.toList());
  }
}
