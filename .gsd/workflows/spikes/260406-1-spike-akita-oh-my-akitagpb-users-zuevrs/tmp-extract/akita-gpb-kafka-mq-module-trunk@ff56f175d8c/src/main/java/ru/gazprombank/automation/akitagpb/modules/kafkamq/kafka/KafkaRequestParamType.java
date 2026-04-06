package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka;

/** Параметры для формирования запроса к kafka */
public enum KafkaRequestParamType {
  HEADER,
  HEADER_LONG,
  HEADER_HEX,
  BODY,
  VAR,
  TOPIC,
  KEY
}
