package me.w1992wishes.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Properties;

/**
 * @Description ConsumerExample
 * @Author w1992wishes
 * @Date 2018/6/20 14:54
 * @Version 1.0
 */
public class ConsumerExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerExample.class);

    public static void main(String[] args) {
        // 创建一个 properties 对象
        Properties kafkaProps = new Properties();
        // 为消费者对象设置三个必要属性，并且设置可选groupId
        kafkaProps.put("bootstrap.servers", "192.168.11.179:9092");
        kafkaProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProps.put("group.id", "CountryCounter");

        // 创建消费者
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProps);

        // 订阅主题
        consumer.subscribe(Collections.singletonList("CustomerCountry"));

        // 轮询
        try {
            while (true){
                ConsumerRecords<String, String> records = consumer.poll(100);
                for (ConsumerRecord<String, String> record : records) {
                    LOGGER.info("topic={}, partition={}, offset={}, customer={}, country={}",
                            record.topic(), record.partition(), record.offset(), record.key(), record.value());
                }
            }
        } finally {
            consumer.close();
        }
    }

}
