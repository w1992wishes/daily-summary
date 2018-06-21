package me.w1992wishes.study.kafka.spring.integration.listener;

import me.w1992wishes.study.kafka.spring.integration.constant.KafkaConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FaceKafkaListener implements IntellifKafkaListener<String, String> {

    @KafkaListener(topics = {KafkaConstant.ENGINE_TOPIC})
    @Override
    public  void listen(ConsumerRecord<String, String> record) {
        Optional<String> kafkaMessage = Optional.ofNullable(record.value());
        if (kafkaMessage.isPresent()) {
            Object message = kafkaMessage.get();
            System.out.println("listen1 " + message);
        }
    }

}
