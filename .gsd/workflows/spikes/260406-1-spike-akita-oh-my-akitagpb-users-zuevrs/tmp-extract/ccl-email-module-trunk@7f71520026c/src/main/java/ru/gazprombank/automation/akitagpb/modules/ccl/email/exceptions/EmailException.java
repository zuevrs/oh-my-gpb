package ru.gazprombank.automation.akitagpb.modules.ccl.email.exceptions;

public class EmailException extends RuntimeException {

  public EmailException(String message, Object... params) {
    super(String.format(message, params));
  }

  public EmailException(String message) {
    super(message);
  }
}
