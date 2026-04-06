package ru.gazprombank.automation.akitagpb.modules.ccl.helpers.loggers;

import java.io.OutputStream;
import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Утилитный класс - логгер http-ответов */
public class HttpResponseLoggerPrintStream extends PrintStream {

  private static final Logger logger = LoggerFactory.getLogger(HttpResponseLoggerPrintStream.class);

  public HttpResponseLoggerPrintStream() {
    super(System.out);
  }

  public HttpResponseLoggerPrintStream(OutputStream out) {
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
