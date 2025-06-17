package com.capston_design.fkiller.itoms.service_desk.client;

import com.capston_design.fkiller.itoms.service_desk.dto.RandomUserResponseDTO;
import com.capston_design.fkiller.itoms.service_desk.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserKafkaConsumer {

    private final IncidentRepository incidentRepository;

    @Value("${kafka.topic.user.random.response:itoms.user.random.response}")
    private String userRandomResponseTopic;

    @KafkaListener(topics = "#{'${kafka.topic.user.random.response:itoms.user.random.response}'}",
            groupId = "${spring.kafka.consumer.group-id:itoms-service-desk}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleRandomUserResponse(RandomUserResponseDTO responseDTO) {
        log.info("[Kafka] 수신된 UserResponse: incidentId={}, userId={}, name={}",
                responseDTO.getIncidentId(), responseDTO.getUserId(), responseDTO.getUserName());

        incidentRepository.findById(responseDTO.getIncidentId()).ifPresent(incident -> {
            incident.setRequesterById(responseDTO.getUserId());
            incident.setRequester(responseDTO.getUserName());
            incidentRepository.save(incident);
            log.info("Incident {} updated with requester {}", incident.getId(), responseDTO.getUserName());
        });
    }
} 