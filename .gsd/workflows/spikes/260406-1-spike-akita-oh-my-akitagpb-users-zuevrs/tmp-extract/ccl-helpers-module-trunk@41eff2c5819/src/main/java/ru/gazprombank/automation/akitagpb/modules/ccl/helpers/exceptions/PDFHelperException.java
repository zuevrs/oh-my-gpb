package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.exceptions;

public class PDFHelperException extends RuntimeException {

  public PDFHelperException(String message, Object... params) {
    super(String.format(message, params));
  }

  public PDFHelperException(String message) {
    super(message);
  }
}
