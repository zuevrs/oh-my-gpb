package ru.gazprombank.automation.akitagpb.modules.kafkamq.hooks;

import io.cucumber.java.After;
import io.cucumber.java.DataTableType;
import java.util.Map;
import javax.jms.QueueConnection;
import lombok.extern.slf4j.Slf4j;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.mq.artemis.ArtemisRequestParam;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.mq.artemis.ArtemisRequestParamType;

@Slf4j
public class ArtemisHooks extends BaseMethods {

  private static final String CONNECTION_VAR = "__artemisConnection__";

  /**
   * Автоматически закрывает соединение после каждого сценария с тегом @artemis. Работает даже если
   * тест упал.
   */
  @After("@artemis")
  public void closeArtemisConnection() {
    var connection = akitaScenario.getVar(CONNECTION_VAR);
    if (connection != null) {
      try {
        ((QueueConnection) connection).close();
        akitaScenario.setVar(CONNECTION_VAR, null);
        log.info("Artemis соединение закрыто в @After хуке");
      } catch (Exception e) {
        log.warn("Ошибка при закрытии Artemis соединения: {}", e.getMessage());
      }
    }
  }

  /**
   * DataTableType для таблиц параметров Artemis. Формат таблицы: | type | name | value | | PROPERTY
   * | src_CorrelationID | ${correlationId} | | BODY | body | request.json |
   */
  @DataTableType
  public ArtemisRequestParam artemisRequestParamTransformer(Map<String, String> entry) {
    String type = entry.get("type");
    String name = entry.get("name");
    String value = entry.get("value");

    if (type == null) {
      throw new IllegalArgumentException(
          "Таблица параметров Artemis должна содержать заголовки: | type | name | value |\n"
              + "Полученные колонки: "
              + entry.keySet());
    }

    return new ArtemisRequestParam(
        ArtemisRequestParamType.valueOf(type.toUpperCase()), name, value);
  }
}
