package com.capston_design.fkiller.itoms.service_desk.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.user.random.request:itoms.user.random.request}")
    private String userRandomRequestTopic;

    public void publishRandomUserRequest(UUID incidentId) {
        kafkaTemplate.send(userRandomRequestTopic, incidentId.toString(), incidentId);
    }
} 