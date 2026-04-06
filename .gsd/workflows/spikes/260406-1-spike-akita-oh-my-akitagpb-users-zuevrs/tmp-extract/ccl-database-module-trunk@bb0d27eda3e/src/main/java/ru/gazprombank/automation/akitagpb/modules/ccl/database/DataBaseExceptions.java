package ru.gazprombank.automation.akitagpb.modules.ccl.database;

public class DataBaseExceptions extends RuntimeException {

  public DataBaseExceptions(String message, Object... params) {
    super(String.format(message, params));
  }

  public DataBaseExceptions(String message) {
    super(message);
  }
}
