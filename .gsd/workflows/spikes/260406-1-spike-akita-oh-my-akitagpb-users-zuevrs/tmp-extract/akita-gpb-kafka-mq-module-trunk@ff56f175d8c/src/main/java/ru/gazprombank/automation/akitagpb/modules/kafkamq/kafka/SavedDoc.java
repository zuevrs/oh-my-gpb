package ru.gazprombank.automation.akitagpb.modules.kafkamq.kafka;

public record SavedDoc(
    org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record, String lastHeader) {}
