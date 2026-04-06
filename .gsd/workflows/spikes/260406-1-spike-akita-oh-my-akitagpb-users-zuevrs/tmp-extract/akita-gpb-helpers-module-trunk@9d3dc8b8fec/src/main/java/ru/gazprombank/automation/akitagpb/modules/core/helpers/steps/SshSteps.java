package ru.gazprombank.automation.akitagpb.modules.core.helpers.steps;

import static ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables.resolveVars;
import static ru.gazprombank.automation.akitagpb.modules.core.helpers.SshConnectionHelper.connectionSsh;
import static ru.gazprombank.automation.akitagpb.modules.core.helpers.SshConnectionHelper.sendCommandSsh;
import static ru.gazprombank.automation.akitagpb.modules.core.steps.isteps.IBaseMethods.akitaScenario;

import io.cucumber.java.ru.И;
import io.cucumber.java.ru.Пусть;
import java.io.IOException;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.assertj.core.api.Assertions;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class SshSteps {
  @Пусть(
      "^создана SSH сессия с хостом: \"(.*)\", портом: \"(.*)\", логином: \"(.*)\", паролем: \"(.*)\" и сохранена в переменную \"(.*)\"$")
  public void getSshConnect(
      String host, String port, String login, String password, String varName) {
    akitaScenario.setVar(
        varName,
        connectionSsh(
            BaseMethods.getInstance().getPropertyOrStringVariableOrValue(resolveVars(host)),
            BaseMethods.getInstance().getPropertyOrStringVariableOrValue(resolveVars(port)),
            BaseMethods.getInstance().getPropertyOrStringVariableOrValue(resolveVars(login)),
            BaseMethods.getInstance().getPropertyOrStringVariableOrValue(resolveVars(password))));
  }

  @И("^выполнена команда \"(.*)\" по SSH сессии \"(.*)\", лог сохранен в переменную \"(.*)\"$")
  public void sendSshCommand(String commandVar, String sshName, String logVar) {
    SSHClient client = (SSHClient) akitaScenario.getVar(sshName);
    String command =
        BaseMethods.getPropertyOrStringVariableOrValueAndReplace(resolveVars(commandVar));
    try {
      akitaScenario.setVar(logVar, sendCommandSsh(client, command));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @И("^в переменной \"(.*)\" содержится текст \"(.*)\"$")
  public void isVarContainText(String varName, String text) {
    String actualValue = (String) akitaScenario.getVar(varName);
    Assertions.assertThat(
            actualValue.contains(
                BaseMethods.getInstance().getPropertyOrStringVariableOrValue(text)))
        .withFailMessage("Переменная " + varName + " не содержит текст: " + text)
        .isTrue();
  }

  @И("^в переменной \"(.*)\" не содержится текст \"(.*)\"$")
  public void isVarNotContainText(String varName, String text) {
    String actualValue = (String) akitaScenario.getVar(varName);
    Assertions.assertThat(
            actualValue.contains(
                BaseMethods.getInstance().getPropertyOrStringVariableOrValue(text)))
        .withFailMessage("Переменная " + varName + " содержит текст: " + text)
        .isFalse();
  }

  @И("^выполнить копирование файла в сессию \"(.*)\" по пути \"(.*)\" на сервер по пути \"(.*)\"$")
  public static void transferFileByShh(
      String clientName, String localFilePath, String remoteFilePath) {
    SSHClient client = (SSHClient) akitaScenario.getVar(clientName);
    try {
      client.useCompression();
      client.newSCPFileTransfer().upload(new FileSystemFile(localFilePath), remoteFilePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @И("^закрыть сессию с именем \"(.*)\"$")
  public void closeSshSession(String clientName) throws IOException {
    SSHClient client = (SSHClient) akitaScenario.getVar(clientName);
    client.disconnect();
  }

  //  @И(
  //      "^выполнена команда открыть файл по пути \"(.*)\" по SSH сессии \"(.*)\", содержимое файла
  // сохранено в переменную \"(.*)\"$")
  //  public void openFileInSSHConnect(String pathToFile, String sshName, String logVar)
  //      throws JSchException, SftpException, IOException {
  //    String result = "";
  //    ClientSession session = (ClientSession) akitaScenario.getVar(sshName);
  //    Channel ch = session.openChannel("sftp");
  //    ch.connect();
  //    ChannelSftp sftpCh = (ChannelSftp) ch;
  //
  //    InputStream is = sftpCh.get(pathToFile);
  //    result = new String(is.readAllBytes());
  //    is.close();
  //    sftpCh.disconnect();
  //    akitaScenario.setVar(logVar, result);
  //  }
  //
  //  @И("^выполнить копирование файла в сессию \"(.*)\" по пути \"(.*)\" на сервер по пути
  // \"(.*)\"$")
  //  public static void transferFileByShh(
  //      String sshName, String localFilePath, String remoteFilePath) {
  //
  //    Session session = (Session) akitaScenario.getVar(sshName);
  //
  //    try {
  //      session.connect();
  //
  //      ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
  //      channelSftp.connect();
  //
  //      channelSftp.put(localFilePath, remoteFilePath);
  //    } catch (JSchException | SftpException e) {
  //      e.printStackTrace();
  //    }
  //  }
}
