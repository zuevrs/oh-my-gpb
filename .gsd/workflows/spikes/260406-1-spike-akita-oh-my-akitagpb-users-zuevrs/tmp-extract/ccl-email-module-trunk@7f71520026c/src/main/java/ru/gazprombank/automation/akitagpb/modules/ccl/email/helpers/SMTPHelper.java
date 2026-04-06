package ru.gazprombank.automation.akitagpb.modules.ccl.email.helpers;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import ru.gazprombank.automation.akitagpb.modules.ccl.helpers.VaultStore;

/**
 * Класс, содержащий методы для отправки электронных писем по протоколу SMTP. Использует конкретный
 * хост и ТУЗ.
 */
public class SMTPHelper {

  private static final VaultStore vaultStore = VaultStore.getInstance();
  private static final String smtpHostServer = "xxx.xxx.gazprombank.ru";
  private static final String emailID = "xx";

  /**
   * Отправляет электронное письмо по протоколу SMTP через хост {@link #smtpHostServer} и аккаунт
   * {@link #emailID}.
   *
   * @param toEmail получатели письма.
   * @param subject тема письма.
   * @param body тело письма.
   */
  public static void sendEmail(String toEmail, String subject, String body) {
    try {
      MimeMessage message = new MimeMessage(getSession());
      message.setFrom(new InternetAddress(emailID));
      message.setRecipients(RecipientType.TO, toEmail);
      message.setSubject(subject, "UTF-8");
      message.setText(body, "UTF-8", "html");
      Transport.send(message);
    } catch (MessagingException e) {
      throw new RuntimeException("Не удалось отправить письмо по SMTP.\n" + e.getMessage());
    }
  }

  /**
   * Метод создания сесси для подключения к SMTP хосту.
   *
   * @return {@link Session} объект.
   */
  private static Session getSession() {
    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.host", smtpHostServer);

    return Session.getDefaultInstance(props, getAuthenticator());
  }

  /**
   * Метод создаёт объект, содержащие данные для авторизации на SMTP хосте.
   *
   * @return {@link Authenticator} объект.
   */
  private static Authenticator getAuthenticator() {
    return new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        try {
          return new PasswordAuthentication(
              vaultStore.getVaultCertValue("ccl/application", "vault_secrets.exchange.username"),
              vaultStore.getVaultCertValue("ccl/application", "vault_secrets.exchange.password"));
        } catch (RuntimeException e) {
          throw new RuntimeException(
              "Не удалось получить имя пользователя и пароль для почты из Vault.\n"
                  + e.getMessage());
        }
      }
    };
  }
}
