package me.w1992wishes.spark.streaming.preprocess.util;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.Serializable;
import java.util.Properties;

public class KafkaSender implements Serializable {

    private static KafkaSender instance = null;

    private KafkaProducer<String, String> producer;

    private KafkaSender(Properties properties) {
        this.producer = new KafkaProducer<String, String>(properties);
    }

    public static synchronized KafkaSender getInstance(Properties properties) {
        if (instance == null) {
            instance = new KafkaSender(properties);
        }
        return instance;
    }

    // 单条发送
    public void send(String topic, String message) {
        producer.send(new ProducerRecord<String, String>(topic, message));
    }

    public void shutdown() {
        producer.close();
    }


}