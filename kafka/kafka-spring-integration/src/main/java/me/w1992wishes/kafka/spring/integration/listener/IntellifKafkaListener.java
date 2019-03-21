package me.w1992wishes.kafka.spring.integration.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface IntellifKafkaListener<T, V> {

    void listen(ConsumerRecord<T, V> record);

}
