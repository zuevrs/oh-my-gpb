package ru.gazprombank.automation.akitagpb.modules.kafkamq.steps.ibm;

import static ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables.resolveVars;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.И;
import java.security.KeyStore;
import java.util.List;
import java.util.Optional;
import javax.jms.DeliveryMode;
import javax.jms.QueueConnection;
import javax.jms.Session;
import javax.jms.TextMessage;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import ru.gazprombank.automation.akitagpb.modules.core.helpers.PropertyLoader;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.mq.ibm.MqRequestParam;

public class MqSteps extends BaseMethods {

  @И("^создано подключение к mq, с данными из таблицы. Результат сохранен в переменную \"(.*)\"$")
  @SneakyThrows
  public void createMqConnection(String variableName, DataTable dataTable) {
    var cf = new MQQueueConnectionFactory();
    cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
    cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);

    for (List<String> row : dataTable.cells()) {
      String name = row.get(0);
      String value = row.get(1);
      switch (name.toUpperCase()) {
        case "HOST_NAME":
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          cf.setHostName(value);
          break;
        case "PORT":
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          cf.setPort(Integer.parseInt(value));
          break;
        case "QUEUE_MANAGER":
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          cf.setQueueManager(value);
          break;
        case "CHANNEL":
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          cf.setChannel(value);
          break;
        case "USER":
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          cf.setStringProperty(WMQConstants.USERID, value);
          break;
        case "PASSWORD":
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          cf.setStringProperty(WMQConstants.PASSWORD, value);
          break;
        case "SSL":
          cf.setSSLCipherSuite("TLS_RSA_WITH_AES_256_CBC_SHA");
          System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "false");
          String sslKeyStore =
              BaseMethods.getInstance().getPropertyOrStringVariableOrValue("sslKeyStore");
          String sslKeyStorePassword =
              BaseMethods.getInstance().getPropertyOrStringVariableOrValue("sslKeyStorePassword");
          String sslKeyAlias =
              BaseMethods.getInstance().getPropertyOrStringVariableOrValue("sslKeyAlias");
          String sslTrustStore =
              BaseMethods.getInstance().getPropertyOrStringVariableOrValue("sslTrustStore");
          String sslTrustStorePassword =
              BaseMethods.getInstance().getPropertyOrStringVariableOrValue("sslTrustStorePassword");
          System.setProperty("javax.net.ssl.keyStore", sslKeyStore);
          System.setProperty("javax.net.ssl.keyStorePassword", sslKeyStorePassword);
          System.setProperty("javax.net.ssl.keyStoreType", KeyStore.getDefaultType());
          System.setProperty("javax.net.ssl.keyAlias", sslKeyAlias);
          System.setProperty("javax.net.ssl.trustStore", sslTrustStore);
          System.setProperty("javax.net.ssl.trustStorePassword", sslTrustStorePassword);
          System.setProperty("javax.net.ssl.trustStoreType", KeyStore.getDefaultType());
          break;
        case "SSL_CIPHER_SUITE":
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          cf.setSSLCipherSuite(value);
          break;
        case "CONN_NAME":
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          cf.setConnectionNameList(value);
          break;
      }
    }
    var queueConnection = cf.createQueueConnection();
    akitaScenario.setVar(variableName, queueConnection);
  }

  @И(
      "^отправлено сообщение в очередь \"(.*)\" через MQ соединение \"(.*)\", с параметрами из таблицы$")
  @SneakyThrows
  public void sendMessageToMq(
      String queue, String connection, List<MqRequestParam> mqRequestParams) {
    String body = null;
    var queueConnection = (QueueConnection) akitaScenario.getVar(connection);
    var queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
    var queueSender = queueSession.createSender(queueSession.createQueue(queue));
    queueSender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    var message = queueSession.createTextMessage();
    for (MqRequestParam mqRequestParam : mqRequestParams) {
      String name = mqRequestParam.getName();
      String value = mqRequestParam.getValue();
      switch (mqRequestParam.getType()) {
        case HEADER:
          if (value == null || value.isEmpty()) {
            message.setStringProperty(name, "");
          } else {
            value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
            value = resolveVars(value);
            if (name.equals("src_CorrelationID")) {
              message.setJMSCorrelationID(value);
            }
            message.setStringProperty(name, value);
          }
          break;
        case VAR:
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          this.akitaScenario.setVar(name, value);
          break;
        case BODY:
          value = PropertyLoader.loadValueFromFileOrPropertyOrVariableOrDefault(resolveVars(value));
          body = resolveVars(value);
          break;
      }
    }
    message.setText(body);
    queueSender.send(message);
    queueSender.close();
    queueSession.close();
  }

  @И(
      "^отправлено сообщение в очередь \"(.*)\" через MQ соединение \"(.*)\", с параметрами из таблицы и MessageID сохранен в переменную \"(.*)\"$")
  @SneakyThrows
  public void sendMessageToMqAndSaveId(
      String queue, String connection, String varName, List<MqRequestParam> mqRequestParams) {
    String body = null;
    var queueConnection = (QueueConnection) akitaScenario.getVar(connection);
    var queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
    var queueSender = queueSession.createSender(queueSession.createQueue(queue));
    queueSender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    var message = queueSession.createTextMessage();
    for (MqRequestParam mqRequestParam : mqRequestParams) {
      String name = mqRequestParam.getName();
      String value = mqRequestParam.getValue();
      switch (mqRequestParam.getType()) {
        case HEADER:
          if (value == null || value.isEmpty()) {
            message.setStringProperty(name, "");
          } else {
            value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
            value = resolveVars(value);
            if (name.equals("src_CorrelationID")) {
              message.setJMSCorrelationID(value);
            }
            message.setStringProperty(name, value);
          }
          break;
        case VAR:
          value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(value);
          value = resolveVars(value);
          this.akitaScenario.setVar(name, value);
          break;
        case BODY:
          value = PropertyLoader.loadValueFromFileOrPropertyOrVariableOrDefault(resolveVars(value));
          body = resolveVars(value);
          break;
      }
    }
    message.setText(body);
    queueSender.send(message);
    System.out.println("=================JMSMessageID=================");
    String id = message.getJMSMessageID();
    System.out.println(id);
    akitaScenario.setVar(varName, id);
    System.out.println("==============================================");
    queueSender.close();
    queueSession.close();
  }

  @И(
      "^прочитать сообщение из очереди \"(.*)\" по селектору \"(.*)\", через MQ соединение \"(.*)\". Результат сохранен в переменную \"(.*)\"$")
  @SneakyThrows
  public void getMessageFromMq(
      String queueName, String selector, String connection, String variableName) {
    var queueConnection = (QueueConnection) akitaScenario.getVar(connection);
    var queueSession = queueConnection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
    var queue = queueSession.createQueue(queueName);
    var value = BaseMethods.getInstance().getPropertyOrStringVariableOrValue(selector);
    value = resolveVars(value);
    var receiver = queueSession.createReceiver(queue, value);
    queueConnection.start();
    var message = Optional.ofNullable(receiver.receive(60 * 1000)).get();
    akitaScenario.log("Сообщение: " + ((TextMessage) message).getText());
    queueSession.close();
    receiver.close();

    akitaScenario.setVar(variableName, ((TextMessage) message).getText());
  }

  @И("^закрыть соединение \"(.*)\"$")
  @SneakyThrows
  public void closeConnection(String connection) {
    var queueConnection = (QueueConnection) akitaScenario.getVar(connection);
    queueConnection.close();
  }

  @И("^в сообщении \"(.*)\", содержится текст \"(.*)\"$")
  @SneakyThrows
  public void checkMessageFromMq(String messageName, String text) {
    var softAssertions = new SoftAssertions();
    var message = (String) akitaScenario.getVar(messageName);
    softAssertions
        .assertThat(message)
        .withFailMessage("В сообщении не содержится текст " + text)
        .contains(text);
    softAssertions.assertAll();
  }
}
