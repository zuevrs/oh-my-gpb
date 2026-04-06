package ru.gazprombank.automation.akitagpb.modules.core.helpers;

import static ru.gazprombank.automation.akitagpb.modules.core.cucumber.ScopedVariables.resolveVars;

import com.hierynomus.sshj.key.KeyAlgorithms;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import ru.gazprombank.automation.akitagpb.modules.core.steps.BaseMethods;

public class SshConnectionHelper {

  //  static SshClient client = SshClient.setUpDefaultClient();
  static final SSHClient ssh = new SSHClient();

  //    static final long DEFAULT_TIMEOUT_SECONDS = 2000;

  public static SSHClient connectionSsh(String host, String port, String user, String password) {
    ssh.addHostKeyVerifier(new PromiscuousVerifier());
    ssh.getTransport()
        .getConfig()
        .setKeyAlgorithms(
            Arrays.asList(KeyAlgorithms.EdDSA25519(), KeyAlgorithms.EdDSA25519CertV01()));
    try {
      ssh.connect(host, Integer.parseInt(port));
      ssh.authPassword(user, password);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return ssh;
  }

  public static String sendCommandSsh(SSHClient client, String command) throws IOException {
    String responseString;
    Session session = client.startSession();
    final Session.Command cmd;
    try {
      cmd =
          session.exec(
              BaseMethods.getInstance().getPropertyOrStringVariableOrValue(resolveVars(command)));
      responseString = String.valueOf(IOUtils.readFully(cmd.getInputStream()));
      cmd.join(10, TimeUnit.SECONDS);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (session != null) {
          session.close();
        }
      } catch (TransportException | ConnectionException e) {
        e.printStackTrace();
      }
      ssh.disconnect();
    }
    return responseString;
  }

  //    public static ClientSession connectionSsh(
  //            String host, String port, String user, String password) {
  //        client.setSignatureFactories(
  //                Arrays.asList(
  //                        BuiltinSignatures.ed25519,
  //                        BuiltinSignatures.ed25519_cert,
  //                        BuiltinSignatures.sk_ssh_ed25519));
  //        client.start();
  //        ClientSession session = null;
  //        try {
  //            session =
  //                    client
  //                            .connect(user, host, Integer.parseInt(port))
  //                            .verify(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
  //                            .getSession();
  //            session.addPasswordIdentity(password);
  //            session.auth().verify(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
  //        } catch (IOException e) {
  //            e.printStackTrace();
  //        }
  //        return session;
  //    }

  //  public static Session connectionSsh(String host, String port, String user, String password) {
  //    Session session = null;
  //    try {
  //      session = new JSch().getSession(user, host, Integer.parseInt(port));
  //    } catch (JSchException e) {
  //      e.printStackTrace();
  //    }
  //    assert session != null;
  //    session.setPassword(password);
  //    session.setConfig("StrictHostKeyChecking", "no");
  //    return session;
  //  }

  //    public static String sendCommandSsh(ClientSession sessionName, String command) {
  //        String responseString = "";
  //        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
  //        ClientChannel channel;
  //        try {
  //            channel = sessionName.createChannel(Channel.CHANNEL_EXEC, command);
  //            channel.setOut(responseStream);
  //            channel.open().verify(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
  //            channel.waitFor(
  //                    EnumSet.of(ClientChannelEvent.CLOSED),
  //                    TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT_SECONDS));
  //            responseString = responseStream.toString();
  //            channel.close(false);
  //        } catch (IOException e) {
  //            e.printStackTrace();
  //        } finally {
  //            client.stop();
  //        }
  //        return responseString;
  //    }

  //  public static String sendCommandSsh(Session sessionName, String command) {
  //    ChannelExec channel = null;
  //    Session session = null;
  //    String commandLog = "";
  //
  //    try {
  //      session = sessionName;
  //      if (!(session.isConnected())) {
  //        session.connect();
  //      }
  //      channel = (ChannelExec) session.openChannel("exec");
  //
  //      channel.setCommand(command);
  //      ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
  //      channel.setOutputStream(responseStream);
  //      channel.connect();
  //
  //      while (channel.isConnected()) {
  //        Thread.sleep(100);
  //      }
  //
  //      commandLog = responseStream.toString();
  //      System.out.println(commandLog);
  //
  //    } catch (InterruptedException | JSchException e) {
  //      e.printStackTrace();
  //    } finally {
  //      if (session != null) {
  //        session.disconnect();
  //      }
  //      if (channel != null) {
  //        channel.disconnect();
  //      }
  //    }
  //    return commandLog;
  //  }
}
