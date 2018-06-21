package me.w1992wishes.study.kafka.spring.integration.producer;

import me.w1992wishes.study.kafka.spring.integration.constant.KafkaConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FaceKafkaProducer implements IntellifKafkaProducer{

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedRate = 3000)
    public void send(){
        kafkaTemplate.send(KafkaConstant.ENGINE_TOPIC, "currentTime", String.valueOf(System.currentTimeMillis()));
    }
}
