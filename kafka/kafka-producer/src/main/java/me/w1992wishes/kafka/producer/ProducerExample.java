package me.w1992wishes.kafka.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * @Description ProducerExample
 * @Author w1992wishes
 * @Date 2018/6/20 14:00
 * @Version 1.0
 */
public class ProducerExample {

    public static void main(String[] args) {
        // 创建一个 properties 对象
        Properties kafkaProps = new Properties();
        // 为生产者对象设置三个必要属性，其他默认
        kafkaProps.put("bootstrap.servers", "192.168.11.179:9092");
        kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // 创建生成者
        Producer<String, String> producer = new KafkaProducer<>(kafkaProps);

        // 消息
        ProducerRecord<String, String> record = new ProducerRecord<>("CustomerCountry", "Precision Products", "France");

        // 发送消息
        try {
            producer.send(record).get();// 同步发送，send()返回一个Future对象，然后调用get()等待kafka响应
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        producer.send(record, (recordMetadata, exception) ->{});// 异步发送，使用回调org.apache.kafka.clients.producer.Callback
    }

}
