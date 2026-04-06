package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.xml;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.MessageValidator;
import ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.validators.ValidationException;

public class XmlMessageValidator implements MessageValidator {
  @Override
  public void validate(String message) throws ValidationException {
    try {
      DocumentBuilderFactory.newInstance()
          .newDocumentBuilder()
          .parse(new InputSource(new StringReader(message)));
    } catch (Exception e) {
      //            throw new AssertionError("Message is not valid XML: " + message, e);
      throw new ValidationException(ValidationException.ValidationErrorType.BUSINESS);
    }
  }
}
