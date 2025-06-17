package com.capston_design.fkiller.itoms.service_desk.client;

import com.capston_design.fkiller.itoms.service_desk.dto.CreateTicketResponseDTO;
import com.capston_design.fkiller.itoms.service_desk.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketKafkaConsumer {

    private final IncidentRepository incidentRepository;

    @Value("${kafka.topic.ticket.response:itoms.ticket.response}")
    private String ticketResponseTopic;

    /**
     * Ticket 서비스에서 발행한 응답(Event)을 수신하여 Incident에 ticketId를 업데이트한다.
     */
    @KafkaListener(topics = "#{'${kafka.topic.ticket.response:itoms.ticket.response}'}",
            groupId = "${spring.kafka.consumer.group-id:itoms-service-desk}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleTicketResponse(CreateTicketResponseDTO responseDTO) {
        log.info("[Kafka] 수신된 TicketResponse: incidentId={}, ticketId={}", responseDTO.getIncidentId(), responseDTO.getTicketId());
        incidentRepository.findById(responseDTO.getIncidentId()).ifPresent(incident -> {
            incident.setTicketByID(responseDTO.getTicketId());
            incidentRepository.save(incident);
            log.info("Incident {} updated with TicketId {}", incident.getId(), responseDTO.getTicketId());
        });
    }
} 