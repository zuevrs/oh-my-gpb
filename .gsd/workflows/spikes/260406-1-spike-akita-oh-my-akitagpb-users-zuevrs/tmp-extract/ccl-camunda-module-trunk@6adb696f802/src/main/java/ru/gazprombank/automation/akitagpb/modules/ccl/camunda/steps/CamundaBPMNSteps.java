package ru.gazprombank.automation.akitagpb.modules.ccl.camunda.steps;

import io.cucumber.java.ru.И;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.models.DeployedDefinitions;
import ru.gazprombank.automation.akitagpb.modules.ccl.camunda.api.rest.requests.CamundaRequests;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.XMLHelper;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

import java.util.List;
import java.util.Map;

import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.addEndScriptInBpmn;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.addFlagAutotest;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.getDeployedDefinitionInfo;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.getDocumentFromVarNameOrPath;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.getPrepareBpmnChangeToModerate;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.prepareBpmnChangeTestFileWithCallActivity;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.prepareBpmnChangeTestFileWithCallActivityEscalation;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.prepareBpmnChangeTestFileWithIntermediateCatchEvent;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.prepareBpmnChangeTestFileWithSendTask;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.prepareBpmnChangeTestFileWithServiceTask;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.prepareBpmnChangeTestFileWithUserTask;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.prepareCamundaChangeToModerate;
import static ru.gazprombank.automation.akitagpb.modules.ccl.camunda.helpers.CamundaHelpers.saveVarFromCamundaResponse;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.processValue;
import static ru.gazprombank.automation.akitagpb.modules.ccl.helpers.StringHelper.resolveVariables;

public class CamundaBPMNSteps extends BaseMethods {

    /**
     * Данный шаг позволяет деплоить BPMN-схемы и DMN-таблицы в Camunda.<br>
     * Для правильной работы метода, необходимо указать структуру в виде таблицы:<br>
     * | Путь до файла         | Переменная для key | Переменная для id |<br>
     * | /testing/ebg.bpmn     | process_value      |                   |<br>
     * | /testing/decision.dmn |                    | definitionId      |<br>
     * <p>
     * Заполнение колонки "Путь до файла" - обязательно. Остальные столбцы опциональны.
     * В колонке "Путь до файла" указываем путь до нужного файла. Поддерживаемый формат файлов: .bpmn и .dmn (пример: /testing/ebg.bpmn или
     * /testing/decision.dmn).
     * В колонке "Переменная для *" указываем наименование переменной, в которую нужно сохранить значение поля, имя которого указывается вместо *.
     * <p>
     * В данном примере, будет выполнен деплой BPMN-схемы (ebg.bpmn) и DMN-таблицы (decision.bpmn).
     * После деплоя BPMN-схемы (ebg.bpmn) индентификатор processValue будет сохранен в переменную process_value.
     * После деплоя DMN-таблицы (decision.bpmn) Definition Id будет сохранен в переменную definitionId.
     */
    @И("^загрузить BPMN-процессы и/или DMN-таблицы в Camunda")
    public void deployCamundaFiles(List<Map<String, String>> table) {
        for (Map<String, String> line : table) {
            String filePath = line.get("Путь до файла");
            Assertions.assertThat(filePath).as("Не указан путь до файла").isNotBlank();
            filePath = resolveVariables(filePath);

            Map<String, Object> deployedDefinitionInfo;

            if (DeployedDefinitions.getInstance().isDeployed(filePath)) {
                akitaScenario.log(
                        String.format("Файл %s уже был задеплоен во время текущего запуска и актуален, дополнительная загрузка не тербуется.",
                                      filePath));
                deployedDefinitionInfo = DeployedDefinitions.getInstance().getLastDeployedDefinitionByKey(filePath).getDeployedDefinitionInfo();
            } else {
                if (line.get("deployment-name") == null) {
                    deployedDefinitionInfo = getDeployedDefinitionInfo(CamundaRequests.deployCamundaFiles(filePath));
                } else {
                    deployedDefinitionInfo = getDeployedDefinitionInfo(CamundaRequests.deployCamundaFiles(filePath, line.get("deployment-name")));
                }
            }

            saveVarFromCamundaResponse(line, deployedDefinitionInfo);
        }
    }

    /**
     * Шаг для добавления скрипта в BPMN-процесс
     *
     * @param endScriptFile файл с содержимым скрипта
     * @param id            кубика в camunda
     * @param table         необходимо указать структуру в виде таблицы:<br>
     *                      * | Путь до файла         | Переменная для key | <br>
     *                      * | /testing/ebg.bpmn     | process_value      | <br>
     *                      или
     *                      * | Путь до файла         | Переменная для key | Позиция endScript |<br>
     *                      * | /testing/ebg.bpmn     | process_value      | 1                 |<br>
     *                      <p>
     *                      нумерация позиции начинается с 1
     */
    @И("^endScript из файла \"(.*)\" добавить в BPMN-процесс на кубик \"(.*)\" и загрузить в Camunda$")
    public void deployCamundaFileWithEndScript(String endScriptFile, String id, List<Map<String, String>> table) {
        for (Map<String, String> line : table) {
            String bpmnFilePath = line.get("Путь до файла");
            Assertions.assertThat(bpmnFilePath).as("Не указан путь до файла").isNotBlank();
            bpmnFilePath = resolveVariables(bpmnFilePath);
            id = resolveVariables(id);

            Document document = XMLHelper.parseXml(resolveVariables(bpmnFilePath));
            addEndScriptInBpmn(document, endScriptFile, id, line.get("Позиция endScript"));
            addFlagAutotest(document);
            String camundaContentBody = XMLHelper.getContentFromDocumentXml(document);

            Map<String, Object> deployedDefinitionInfo = getDeployedDefinitionInfo(CamundaRequests.deployCamundaContentBodyFile(camundaContentBody));
            saveVarFromCamundaResponse(line, deployedDefinitionInfo);
        }
    }

    /**
     * Шаг для добавления скриптов в схему bpmn-процесса и сохранение результата в переменную
     * |Id кубика   | Путь до endScript         | Позиция endScript |<br>
     * |Activity_1  | /testing/endScript1.txt   |  1                |<br>
     * |Activity_2  | /testing/endScript2.txt   |                   |<br>
     *
     * @param bpmnFilePath переменная в которой содержится тело bpmn
     * @param table        таблица с параметрами endScript
     */
    @SneakyThrows
    @И("^добавить в BPMN-схему \"(.*)\" endScript и сохранить результат в переменную \"(.*)\"$")
    public void prepareBpmnSchema(String bpmnFilePath, String varName, List<Map<String, String>> table) {
        Document document = getDocumentFromVarNameOrPath(bpmnFilePath);

        for (Map<String, String> line : table) {
            String scriptPath = line.get("Путь до endScript");
            String id = line.get("Id кубика");
            String positionEndScript = line.get("Позиция endScript");
            addEndScriptInBpmn(document, scriptPath, id, positionEndScript);
        }
        addFlagAutotest(document);
        String bpmnContent = XMLHelper.getContentFromDocumentXml(document);
        akitaScenario.setVar(varName, bpmnContent);
        akitaScenario.log(bpmnContent);
    }

    /**
     * Данный шаг позволяет деплоить BPMN-схемы из переменной в Camunda.<br>
     * Для правильной работы метода, необходимо указать структуру в виде таблицы:<br>
     * | Переменная для key | <br>
     * | process_value      | <br>
     * |                    | <br>
     * <p>
     * В колонке "Переменная для *" указываем наименование переменной, в которую нужно сохранить значение поля, имя которого указывается вместо *.
     * <p>
     * В данном примере, будет выполнен деплой BPMN-схемы (ebg.bpmn)
     * После деплоя BPMN-схемы (ebg.bpmn) индентификатор processValue будет сохранен в переменную process_value.
     */
    @И("^загрузить BPMN-схему из переменной \"(.*)\" в Camunda$")
    public void deployCamundaFromVar(String contentBody, List<Map<String, String>> table) {
        contentBody = processValue(contentBody);
        Map<String, String> line = table.get(0);
        Map<String, Object> deployedDefinitionInfo = getDeployedDefinitionInfo(CamundaRequests.deployCamundaContentBodyFile(contentBody));
        saveVarFromCamundaResponse(line, deployedDefinitionInfo);
    }

    /**
     * Шаг для изменения типа кубика на 'moderate'
     *
     * @param id    кубика в camunda
     * @param table необходимо указать структуру в виде таблицы:<br>
     *              * | Путь до файла         | Переменная для key | <br>
     *              * | /testing/ebg.bpmn     | process_value      | <br>
     */
    @И("^добавить в BPMN-процесс на кубик \"(.*)\" топик moderate и загрузить в Camunda$")
    public void deployCamundaChangeToModerate(String id, List<Map<String, String>> table) {
        for (Map<String, String> line : table) {
            String bpmnFilePath = line.get("Путь до файла");
            Assertions.assertThat(bpmnFilePath).as("Не указан путь до файла").isNotBlank();
            bpmnFilePath = resolveVariables(bpmnFilePath);
            id = resolveVariables(id);

            String camundaContentBody = getPrepareBpmnChangeToModerate(bpmnFilePath, id);
            Map<String, Object> deployedDefinitionInfo = getDeployedDefinitionInfo(CamundaRequests.deployCamundaContentBodyFile(camundaContentBody));
            saveVarFromCamundaResponse(line, deployedDefinitionInfo);
        }
    }

    /**
     * Шаг для добавления признака isAutotest в BPMN-процесс
     *
     * @param id    кубика в camunda
     * @param table необходимо указать структуру в виде таблицы:<br>
     *              * | Путь до файла         | Переменная для key | <br>
     *              * | /testing/ebg.bpmn     | process_value      | <br>
     */
    @И("^на кубик \"(.*)\" в BPMN-процесс добавить признак isAutotest и загрузить в Camunda$")
    public void deployCamundaFileWithEndScript(String id, List<Map<String, String>> table) {
        for (Map<String, String> line : table) {
            String bpmnFilePath = line.get("Путь до файла");
            Assertions.assertThat(bpmnFilePath).as("Не указан путь до файла").isNotBlank();
            bpmnFilePath = resolveVariables(bpmnFilePath);
            id = resolveVariables(id);

            Document document = XMLHelper.parseXml(resolveVariables(bpmnFilePath));

            String xpath = String.format("//*[local-name()='callActivity'][@id='%s']", id);
            Element callActivityNode = (Element) XMLHelper.findNodeByXpath(document, xpath);
            callActivityNode.setAttribute("calledElement", callActivityNode.getAttribute("calledElement") + "_isAutotest");

            Element processNode = (Element) XMLHelper.findNodeByXpath(document, "//*[local-name()='process']");
            String idProcess = processNode.getAttribute("id");
            processNode.setAttribute("id", idProcess + "_isAutotest");
            processNode.setAttribute("name", processNode.getAttribute("name") + " (isAutotest)");

            Element diagramNode = (Element) XMLHelper.findNodeByXpath(document,
                                                                      String.format("//*[local-name()='BPMNPlane'][@bpmnElement='%s']", idProcess));
            diagramNode.setAttribute("bpmnElement", idProcess + "_isAutotest");

            Map<String, Object> deployedDefinitionInfo = getDeployedDefinitionInfo(
                    CamundaRequests.deployCamundaContentBodyFile(XMLHelper.getContentFromDocumentXml(document)));
            saveVarFromCamundaResponse(line, deployedDefinitionInfo);
        }
    }


    /**
     * @param bpmnFilePath путь до bpmn-схемы или переменная содержащая схему
     * @param varName      имя переменной в которую будет сохранятся изменённая схема
     * @param table        таблица с id кубиков в схеме, пример
     *                     | Id кубика        |
     *                     | Activity_13stxk3 |
     *                     | Activity_0mhgjom |
     */
    @И("^добавить в BPMN-схему \"(.*)\" топик moderate на (сервисную таску|кубики вызова подпроцессов)? и сохранить результат в переменную \"(.*)\"$")
    public void saveVarCamundaChangeToModerate(String bpmnFilePath, String typeTask, String varName, List<Map<String, String>> table) {
        Document document = getDocumentFromVarNameOrPath(bpmnFilePath);

        String bpmnContent = prepareCamundaChangeToModerate(typeTask, table, document);

        akitaScenario.log(bpmnContent);
        akitaScenario.setVar(varName, bpmnContent);
    }

    /**
     * @param bpmnFilePath путь до bpmn-схемы или переменная содержащая схему
     * @param table        таблица с id кубиков в схеме, пример
     *                     | Id кубика        |
     *                     | Activity_13stxk3 |
     *                     | Activity_0mhgjom |
     */
    @И("^добавить в BPMN-схему \"(.*)\" топик moderate на (сервисную таску|кубики вызова подпроцессов)? и загрузить в Camunda$")
    public void deployCamundaChangeToModerate(String bpmnFilePath, String typeTask, List<Map<String, String>> table) {
        Document document = getDocumentFromVarNameOrPath(bpmnFilePath);

        String bpmnContent = prepareCamundaChangeToModerate(typeTask, table, document);

        akitaScenario.log(bpmnContent);
        Map<String, String> line = table.get(0);
        Map<String, Object> deployedDefinitionInfo = getDeployedDefinitionInfo(CamundaRequests.deployCamundaContentBodyFile(bpmnContent));
        saveVarFromCamundaResponse(line, deployedDefinitionInfo);
    }

    /**
     * Шаг для добавления признака isAutotest на указанные кубики
     *
     * @param bpmnFilePath путь до bpmn-схемы или переменная содержащая схему
     * @param varName      имя переменной в которую будет сохранятся изменённая схема
     * @param table        таблица с id кубиков в схеме, пример
     *                     | Id кубика        |
     *                     | Activity_13stxk3 |
     */
    @И("^добавить в BPMN-схему \"(.*)\" признак isAutotest на кубики вызова подпроцессов и сохранить результат в переменную \"(.*)\"$")
    public void deployCamundaFileWithEndScript2(String bpmnFilePath, String varName, List<Map<String, String>> table) {
        Document document = getDocumentFromVarNameOrPath(bpmnFilePath);

        for (Map<String, String> line : table) {
            String id = resolveVariables(line.get("Id кубика"));
            String xpath = String.format("//*[local-name()='callActivity'][@id='%s']", id);
            Element callActivityNode = (Element) XMLHelper.findNodeByXpath(document, xpath);
            callActivityNode.setAttribute("calledElement", callActivityNode.getAttribute("calledElement") + "_isAutotest");
        }

        addFlagAutotest(document);
        String bpmnContent = XMLHelper.getContentFromDocumentXml(document);
        akitaScenario.log(bpmnContent);
        akitaScenario.setVar(varName, bpmnContent);
    }

    /**
     * Шаг для добавления сервисной/пользовательской таски со схемы процесса из Bitbucket на тестовую схему
     *
     * @param bpmnFilePath путь до bpmn-схемы, с которой берём кубик для проверки
     * @param id           идентификатор кубика, который берём для проверки
     *                     Для правильной работы метода, необходимо указать структуру в виде таблицы:<br>
     *                     | Переменная для key | <br>
     *                     | process_value      | <br>
     *                     |                    | <br>
     *                     <p>
     *                     В колонке "Переменная для *" указываем наименование переменной, в которую нужно сохранить значение поля, имя которого
     *                     указывается вместо *.
     *                     <p>
     */
    @И("^добавить в тестовую BPMN-схему содержимое (сервисной таски|пользовательской таски|таски отправки|intermediateCatchEvent)? \"(.*)\" из BPMN-схемы \"(.*)\" и загрузить в Camunda$")
    public void deployCamundaTestFileWithServiceTask(String typeTask, String id, String bpmnFilePath, List<Map<String, String>> table) {
        id = resolveVariables(id);
        String bpmnContent = switch (typeTask) {
            case "сервисной таски" -> prepareBpmnChangeTestFileWithServiceTask(bpmnFilePath, id);
            case "пользовательской таски" -> prepareBpmnChangeTestFileWithUserTask(bpmnFilePath, id);
            case "таски отправки" -> prepareBpmnChangeTestFileWithSendTask(bpmnFilePath, id);
            case "intermediateCatchEvent" -> prepareBpmnChangeTestFileWithIntermediateCatchEvent(bpmnFilePath, id);
            default -> throw new RuntimeException("Нет такого типа подставляемой таски: " + typeTask);
        };

        Map<String, String> line = table.get(0);
        Map<String, Object> deployedDefinitionInfo = getDeployedDefinitionInfo(CamundaRequests.deployCamundaContentBodyFile(bpmnContent));
        saveVarFromCamundaResponse(line, deployedDefinitionInfo);
    }

    /**
     * Шаг для добавления кубика вызова подпроцесса со схемы процесса из Bitbucket на тестовую схему
     *
     * @param bpmnFilePath путь до bpmn-схемы, с которой берём кубик подпроцесса для проверки
     * @param id           идентификатор кубика подпроцесса, который берём для проверки
     * @param escalation   идентификатор эскалации, которую устанавливаем на тестируемом подпроцессе
     *                     Для правильной работы метода, необходимо указать структуру в виде таблицы:<br>
     *                     | Переменная для key | <br>
     *                     | process_value      | <br>
     *                     |                    | <br>
     *                     <p>
     *                     В колонке "Переменная для *" указываем наименование переменной, в которую нужно сохранить значение поля, имя которого
     *                     указывается вместо *.
     *                     <p>
     */
    @И("^добавить в тестовую BPMN-схему содержимое кубика \"(.*)\" для вызова (подпроцесса|мультиподпроцесса)? (с эскалацией \"(.*)\" )?из BPMN-схемы \"(.*)\" и загрузить в Camunda$")
    public void deployCamundaTestFileWithCallActivity(String id, String typeTask, String escalation, String bpmnFilePath,
                                                      List<Map<String, String>> table) {
        Document testDocument;
        String xpathCallActivityTest = "//*[local-name()='callActivity'][@id='checkActivity']";

        if (escalation == null) {
            testDocument = prepareBpmnChangeTestFileWithCallActivity(bpmnFilePath, id, xpathCallActivityTest);
        } else {
            testDocument = prepareBpmnChangeTestFileWithCallActivityEscalation(bpmnFilePath, id, escalation, xpathCallActivityTest);
        }

        if (typeTask.equals("мультиподпроцесса")) {
            Document document = XMLHelper.parseXml(resolveVariables(bpmnFilePath));
            String xpath = String.format("//*[local-name()='callActivity'][@id='%s']/*[local-name()='multiInstanceLoopCharacteristics']", id);

            Element multiInstanceNode = (Element) XMLHelper.findNodeByXpath(document, xpath);
            Node importElement = testDocument.importNode(multiInstanceNode, true);

            Element callActivityNodeTest = (Element) XMLHelper.findNodeByXpath(testDocument, xpathCallActivityTest);
            callActivityNodeTest.appendChild(importElement);
        }

        String bpmnContent = XMLHelper.getContentFromDocumentXml(testDocument);
        akitaScenario.log(bpmnContent);

        Map<String, String> line = table.get(0);
        Map<String, Object> deployedDefinitionInfo = getDeployedDefinitionInfo(CamundaRequests.deployCamundaContentBodyFile(bpmnContent));
        saveVarFromCamundaResponse(line, deployedDefinitionInfo);
    }

    /**
     * Шаг для добавления на указанную bpmn-схему признака isAutotest и дальнейшего её деплоя
     *
     * @param bpmnFilePath путь до bpmn-схемы, на которую добавляем признак isAutotest
     *                     Для правильной работы метода, необходимо указать структуру в виде таблицы:<br>
     *                     | Переменная для key | <br>
     *                     | process_value      | <br>
     *                     |                    | <br>
     *                     <p>
     *                     В колонке "Переменная для *" указываем наименование переменной, в которую нужно сохранить значение поля, имя которого
     *                     указывается вместо *.
     *                     <p>
     */
    @И("^в BPMN-процесс \"(.*)\" добавить признак isAutotest и загрузить в Camunda")
    public void deployCamundaFilesWithFlagAutotest(String bpmnFilePath, List<Map<String, String>> table) {
        Document document = XMLHelper.parseXml(resolveVariables(bpmnFilePath));

        addFlagAutotest(document);
        String bpmnContent = XMLHelper.getContentFromDocumentXml(document);

        akitaScenario.log(bpmnContent);

        Map<String, String> line = table.get(0);
        Map<String, Object> deployedDefinitionInfo = getDeployedDefinitionInfo(CamundaRequests.deployCamundaContentBodyFile(bpmnContent));
        saveVarFromCamundaResponse(line, deployedDefinitionInfo);
    }
}
