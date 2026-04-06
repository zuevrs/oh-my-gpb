package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.loggers;

import java.io.OutputStream;
import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Утилитный класс - логгер http-запросов */
public class HttpRequestLoggerPrintStream extends PrintStream {

  private static final Logger logger = LoggerFactory.getLogger(HttpRequestLoggerPrintStream.class);

  public HttpRequestLoggerPrintStream() {
    super(System.out);
  }

  public HttpRequestLoggerPrintStream(OutputStream out) {
    super(out);
  }

  @Override
  public void println(String x) {
    this.info(x);
  }

  public void info(String x) {
    logger.info(x);
  }

  public void warn(String x) {
    logger.warn(x);
  }
}
