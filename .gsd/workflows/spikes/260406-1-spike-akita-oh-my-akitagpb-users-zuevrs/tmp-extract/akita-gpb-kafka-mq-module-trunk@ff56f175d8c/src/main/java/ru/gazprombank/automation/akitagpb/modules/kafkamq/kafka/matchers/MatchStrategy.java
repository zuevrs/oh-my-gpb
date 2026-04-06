package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka.matchers;

public enum MatchStrategy {
  POSITIVE_ANY, // Хотя бы одно поле совпадает
  POSITIVE_ALL, // Все поля совпадают
  NEGATIVE_ANY, // Хотя бы одно поле НЕ совпадает
  NEGATIVE_ALL, // Все поля НЕ совпадают
  CUSTOM_HEADER
}
