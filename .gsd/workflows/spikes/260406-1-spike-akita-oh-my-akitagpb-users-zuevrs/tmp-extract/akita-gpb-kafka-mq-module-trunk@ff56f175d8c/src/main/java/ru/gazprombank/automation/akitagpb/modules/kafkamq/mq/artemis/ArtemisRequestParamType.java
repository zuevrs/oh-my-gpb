package ru.gazprombank.automation.akitagpb.modules.kafkamq.mq.artemis;

public enum ArtemisRequestParamType {
  PROPERTY, // JMS message property (аналог HEADER в MQ)
  BODY, // тело сообщения
  VAR // сохранить в переменную сценария
}
