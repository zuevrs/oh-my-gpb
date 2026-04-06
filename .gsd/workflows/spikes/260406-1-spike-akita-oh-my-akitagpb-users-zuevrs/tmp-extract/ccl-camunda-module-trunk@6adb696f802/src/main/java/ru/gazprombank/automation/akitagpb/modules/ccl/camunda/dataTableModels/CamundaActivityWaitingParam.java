package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.dataTableModels;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.DataTableType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.dataTableModels.CamundaActivityWaitingParam.TableParameters.ACTIVITY_ID;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.dataTableModels.CamundaActivityWaitingParam.TableParameters.BUSINESS_KEY;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.dataTableModels.CamundaActivityWaitingParam.TableParameters.PROCESS_ID;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.dataTableModels.CamundaActivityWaitingParam.TableParameters.PROCESS_NAME;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.dataTableModels.CamundaActivityWaitingParam.TableParameters.PROCESS_OR_SUBPROCESS;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.dataTableModels.CamundaActivityWaitingParam.TableParameters.SUBPROCESS_NAME;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.dataTableModels.CamundaActivityWaitingParam.TableParameters.checkActivityWaitingParameters;

/**
 * Модельный класс для создания java-объекта из кукумбер-таблицы параметров (см. {@link TableParameters}), передаваемых
 * в шаге ожидания кубика процесса.
 */
@Getter
@Setter
@NoArgsConstructor //без дефолтного конструктора кукумбер не сможет создать объект
public class CamundaActivityWaitingParam {

    private String activityId;
    private String processId;
    private String processName;
    private boolean isSubprocess;
    private String subprocessName;
    private String businessKey;

    public CamundaActivityWaitingParam(Map<String, String> table) {
        checkActivityWaitingParameters(table);

        activityId = StringHelper.getVariableOrValue(table.get(ACTIVITY_ID.getDescription()));
        processId = StringHelper.getVariableOrValue(table.getOrDefault(PROCESS_ID.getDescription(), null));
        processName = StringHelper.getVariableOrValue(table.getOrDefault(PROCESS_NAME.getDescription(), null));
        isSubprocess = table.containsKey(PROCESS_OR_SUBPROCESS.getDescription()) &&
                (table.get(PROCESS_OR_SUBPROCESS.getDescription()).equalsIgnoreCase("Да") ||
                        table.get(PROCESS_OR_SUBPROCESS.getDescription()).equalsIgnoreCase("true"));
        subprocessName = StringHelper.getVariableOrValue(table.getOrDefault(SUBPROCESS_NAME.getDescription(), null));
        if (!isSubprocess && subprocessName != null) {
            throw new IllegalArgumentException(String.format("Задан параметр '%s' при ожидании кубика на основном процессе",
                                                             SUBPROCESS_NAME.getDescription()));
        }
        businessKey = StringHelper.getVariableOrValue(table.get(BUSINESS_KEY.getDescription()));
    }

    /**
     * Метод для конвертации кукумбер-таблицы параметров шага ожидания кубика в объект класса. Вызывается кукумбером.
     *
     * @param table таблица параметров шага
     * @return объект класса CamundaActivityWaitingParam, соответствующий переданным параметрам
     */
    @DataTableType
    public CamundaActivityWaitingParam camundaActivityWaitingParamTransformer(DataTable table) {
        Map<String, String> map = table.cells().stream().collect(Collectors.toMap(e -> e.get(0), e -> e.get(1)));
        return new CamundaActivityWaitingParam(map);
    }

    /**
     * Список параметров для поиска кубика инстанса процесса в Camunda
     * <p>
     * - "заголовок" таблицы 'parameter - value' - НЕобязательный, существует для "красоты" (поэтому в enum отсутствует),
     * можно не указывать;
     * - параметр 'ID кубика' - ID кубика (активности), на который должен перейти инстанс;
     * - параметр 'Имя процесса' - НЕобязательный (если задан параметр 'ID процесса'), название (имя / name)
     * процесса, на схеме которого находится ожидаемый кубик;
     * - параметр 'ID процесса' - НЕобязательный (если задан параметр 'Имя процесса'), ID процесса,
     * на схеме которого находится ожидаемый кубик - может быть равен как просто definitionKey
     * процесса (например, order-execution для процесса "Обработка кредитной заявки" - можно взять
     * в CamundaModeler'е в поле "ID" или в CamundaCockpit'е в поле "Definition Key"), так и полному definitionId процесса
     * (например, order-execution:143:9da57dff-42f6-11ed-bc41-02426003a31a для "Обработка кредитной заявки",
     * или 9acd4dc3-4566-11ed-86d8-02426003a31a (просто UUID) для процесса "АТ: Проверка создания заявки и клиента" -
     * значение брать по пути jsonPath = deployedProcessDefinitions.keySet().getAt(0) ответа на POST /deployment/create);
     * - параметр 'Кубик SubProcess'а со схемы процесса?' - НЕобязательный, аналог флага для обозначения "места" поиска
     * кубика - на SubProcess'е (подпроцессе) или на процессе.
     * Принимает значения "да", "нет", "true" и "false" (без кавычек, в любом регистре).
     * Если параметра нет в таблице - по умолчанию считается, что кубик на процессе;
     * - параметр 'Имя SubProcess'а' - НЕобязательный, указывается для уточнения SubProcess'а, если на процессе их запущено несколько.
     * - параметр 'businessKey' - businessKey процесса. Принимает как просто название переменной вида "businessKey",
     * так и конструкцию "${businessKey}" - кому как больше нравится.
     */
    public enum TableParameters {
        ACTIVITY_ID("ID кубика"),
        PROCESS_ID("ID процесса"),
        PROCESS_NAME("Имя процесса"),
        PROCESS_OR_SUBPROCESS("Кубик SubProcess'а со схемы процесса?"),
        SUBPROCESS_NAME("Имя SubProcess'а"),
        BUSINESS_KEY("businessKey");

        private final String description;

        TableParameters(String description) {
            this.description = description;
        }

        /**
         * Проверка валидности переданных в cucumber-шаге параметров для поиска кубика.
         * Параметры 'ID кубика' и 'businessKey' - обязательные - проверяется их наличие.
         * Параметр 'Имя процесса' или 'ID процесса' - обязательно должен быть задан хотя бы один из них (если оба сразу
         * заданы - 'ID процесса' будет проигнорирован).
         * Параметр 'Кубик SubProcess'а со схемы процесса?' - необязательный, но если он есть - может принимать только
         * значения 'Да', 'Нет', 'true' или 'false' (без кавычек, в любом регистре)
         *
         * @param dataTable таблица параметров для валидации
         */
        public static void checkActivityWaitingParameters(Map<String, String> dataTable) {
            String message = "Таблица параметров шага должна содержать параметр '%s'";
            assertThat(dataTable.containsKey(ACTIVITY_ID.getDescription()))
                    .as(String.format(message, ACTIVITY_ID.getDescription()))
                    .isEqualTo(true);
            assertThat(dataTable.containsKey(BUSINESS_KEY.getDescription()))
                    .as(String.format(message, BUSINESS_KEY.getDescription()))
                    .isEqualTo(true);
            assertThat(dataTable.containsKey(PROCESS_NAME.getDescription()) || dataTable.containsKey(PROCESS_ID.getDescription()))
                    .as(String.format("Таблица параметров шага должна содержать один из параметров - '%s' или '%s'",
                                      PROCESS_NAME.getDescription(), PROCESS_ID.getDescription()))
                    .isEqualTo(true);
            //'Кубик SubProcess'а со схемы процесса?' - необязательный параемтр. Если его нет - будем считать, что это процесс (isSubprocess = false)
            if (dataTable.containsKey(PROCESS_OR_SUBPROCESS.getDescription())) {
                assertThat(Arrays.asList("да", "нет", "true", "false"))
                        .as("Значение параметра '%s' в таблице должно быть 'Да', 'Нет', 'true' или 'false' (без кавычек, в любом регистре)",
                            PROCESS_OR_SUBPROCESS.getDescription())
                        .contains(dataTable.get(PROCESS_OR_SUBPROCESS.getDescription()).toLowerCase());
            }
        }

        public String getDescription() {
            return description;
        }
    }
}
