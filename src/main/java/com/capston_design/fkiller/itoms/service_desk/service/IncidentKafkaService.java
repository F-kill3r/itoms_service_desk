package com.capston_design.fkiller.itoms.service_desk.service;

import com.capston_design.fkiller.itoms.service_desk.client.TicketKafkaProducer;
import com.capston_design.fkiller.itoms.service_desk.client.UserKafkaProducer;
import com.capston_design.fkiller.itoms.service_desk.dto.*;
import com.capston_design.fkiller.itoms.service_desk.model.Incident;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Priority;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Status;
import com.capston_design.fkiller.itoms.service_desk.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentKafkaService {

    private static final Logger log = LoggerFactory.getLogger(IncidentKafkaService.class);

    private final IncidentRepository incidentRepository;
    private final TicketKafkaProducer ticketKafkaProducer;
    private final UserKafkaProducer userKafkaProducer;

    /**
     * create incident and publish CreateTicketRequestDTO to kafka.
     */
    @Transactional
    public Incident createIncidentKafka(IncidentRequest incidentRequest) {
        Incident incident = new Incident();

        incident.setTitle(incidentRequest.title());
        incident.setContent(incidentRequest.content());

        incident.setRequestDT(LocalDateTime.now());
        incident.setStatus(Status.Incomplete);
        incident.setPriority(Priority.from(incidentRequest.priority()));

        // Incident 저장 (현재 requester 정보는 비어있음, 후속 UserService 응답으로 채움)
        incidentRepository.save(incident);

        // 1) UserService 에 랜덤 유저 요청 발행
        userKafkaProducer.publishRandomUserRequest(incident.getId());

        // 2) TicketService 에 티켓 생성 요청 발행
        CreateTicketRequestDTO ticketRequest = new CreateTicketRequestDTO(
                incident.getId(),
                null // requester 정보는 Ticket 서비스가 필요 없으면 null 로 전달
        );
        ticketKafkaProducer.publishCreateTicket(ticketRequest);

        log.info("Incident created and ticket event published. IncidentId={}, TicketTopicPayload={}",
                incident.getId(), ticketRequest);

        return incident;
    }

    @Transactional
    public Incident completeIncidentKafka(UUID ticketId, UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with id: " + incidentId));

        incident.setStatus(Status.Completed);
        incident.setEndDT(LocalDateTime.now());

        Incident savedIncident = incidentRepository.save(incident);
        log.info("[Kafka] Incident completed - Incident ID: {}, Title: {}", savedIncident.getId(), savedIncident.getTitle());

        // 완료 이벤트도 필요하다면 이곳에서 kafkaTemplate.send(...)
        return savedIncident;
    }
} 