package ru.gazprombank.automation.akitagpb.modules.core.helpers.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

public class XmlErrorHandler implements ErrorHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlErrorHandler.class);

  @Override
  public void warning(SAXParseException exception) {
    LOGGER.info(exception.getMessage());
  }

  @Override
  public void error(SAXParseException exception) {
    warning(exception);
  }

  @Override
  public void fatalError(SAXParseException exception) {
    warning(exception);
  }
}
