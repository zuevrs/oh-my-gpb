package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka;

public enum MessageType {
  TEXT,
  JSON,
  XML,
  HEADER;

  public static MessageType fromString(String messageTypeString) {
    return switch (messageTypeString.toLowerCase()) {
      case "xml" -> MessageType.XML;
      case "text" -> MessageType.TEXT;
      case "header" -> MessageType.HEADER;
      default -> MessageType.JSON;
    };
  }
}
