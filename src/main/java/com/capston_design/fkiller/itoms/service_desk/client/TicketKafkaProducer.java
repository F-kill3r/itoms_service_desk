package com.capston_design.fkiller.itoms.service_desk.client;

import com.capston_design.fkiller.itoms.service_desk.dto.CreateTicketRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.ticket.request:itoms.ticket.request}")
    private String ticketRequestTopic;

    public void publishCreateTicket(CreateTicketRequestDTO requestDTO) {
        kafkaTemplate.send(ticketRequestTopic, requestDTO.getIncidentId().toString(), requestDTO);
    }
} 