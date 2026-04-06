package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.dataTableModels;

import io.cucumber.java.DataTableType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

/**
 * Модельный класс для создания java-объекта из кукумбер-таблицы параметров, передаваемых в шаге обновления переменных процесса Camunda.
 */
@Getter
@Setter
@NoArgsConstructor //без дефолтного конструктора кукумбер не сможет создать объект
public class CamundaVariableUpdatingParam {

    private String action;
    private String name;
    private String type;
    private Object value;

    public CamundaVariableUpdatingParam(String action, String name, String type, String value) {
        this.action = action;
        this.name = resolveVariables(name);
        if (type != null) {
            this.type = StringUtils.capitalize(resolveVariables(type));
            if (value != null) {
                value = resolveVariables(value);
                switch (type) {
                    case "Integer" -> this.value = Integer.parseInt(value);
                    case "Long" -> this.value = Long.parseLong(value);
                    case "Double" -> this.value = Double.parseDouble(value);
                    default -> this.value = value;
                }
            } else {
                this.value = null;
            }
        }
    }

    /**
     * Метод для конвертации кукумбер-таблицы параметров шага обновления переменных процесса Camunda. Вызывается кукумбером.
     *
     * @param entry карта параметров шага
     * @return объект класса CamundaVariableUpdatingParam, соответствующий переданным параметрам
     */
    @DataTableType
    public CamundaVariableUpdatingParam camundaVariableUpdatingParamTransformer(Map<String, String> entry) {
        return new CamundaVariableUpdatingParam(
                entry.get("Действие"),
                entry.get("name"),
                entry.get("type"),
                entry.get("value")
        );
    }
}
