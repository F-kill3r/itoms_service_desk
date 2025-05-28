package com.capston_design.fkiller.itoms.service_desk.service;

import com.capston_design.fkiller.itoms.service_desk.communication.MessageGateway;
import com.capston_design.fkiller.itoms.service_desk.communication.MessageBuilder;
import com.capston_design.fkiller.itoms.service_desk.dto.IncidentRequest;
import com.capston_design.fkiller.itoms.service_desk.model.Incident;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Priority;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Status;
import com.capston_design.fkiller.itoms.service_desk.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;

@Slf4j
@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final MessageGateway messageGateway;
    
    @Value("${messaging.rest.base-url}")
    private String restBaseUrl;
    
    @Value("${messaging.rest.endpoints.incident-create}")
    private String restIncidentCreateEndpoint;
    
    @Value("${messaging.kafka.topics.incident-create}")
    private String kafkaIncidentCreateTopic;

    /**
     * 생성자에서 @Primary로 설정된 MessageGateway Bean을 주입
     */
    public IncidentService(
            IncidentRepository incidentRepository,
            MessageGateway messageGateway) {
        this.incidentRepository = incidentRepository;
        this.messageGateway = messageGateway;
        log.info("IncidentService initialized with {} message gateway", messageGateway.getCommunicationType());
    }

    @Transactional
    public Incident createIncident(IncidentRequest incidentRequest) {
        log.info("Creating incident using {} communication", messageGateway.getCommunicationType());

        Incident incident = new Incident();
        incident.setTitle(incidentRequest.title());
        incident.setContent(incidentRequest.content());
        incident.setRequestDT(LocalDateTime.now());
        incident.setStatus(Status.Incomplete);
        incident.setPriority(Priority.from(incidentRequest.priority()));

        Incident savedIncident = incidentRepository.save(incident);

        restTest(savedIncident);
        //kafkaTest(savedIncident.getId(), incident);
        
        return savedIncident;
    }
    
    /**
     * restTest code
     */
    private void restTest(Incident incident) {
        String correlationId = UUID.randomUUID().toString();
        String testEndpoint = "https://4794bcc9-8d7e-422a-985f-463a502cfa80.mock.pstmn.io/ http://localhost:8080/api/events/incident.created";
        
        // 1. Event-Driven 패턴 테스트 (비동기 이벤트 발행)
        MessageBuilder.create(messageGateway)
            .rest()
            .destination(testEndpoint)
            .message(incident)
            .eventDriven()
            .correlationId(correlationId + "-event")
            .header("event-type", "IncidentCreated")
            .header("pattern", "event-driven")
            .publishEvent()
            .thenAccept(success -> {
                log.info("[Event-Driven] Test result: {}", success ? "Success" : "Failed");
            });

        // 2. Request-Response 패턴 테스트 (동기 응답 대기)
        MessageBuilder.create(messageGateway)
            .rest()
            .destination(testEndpoint)
            .message(incident)
            .requestResponse()
            .correlationId(correlationId + "-reqresp")
            .header("pattern", "request-response")
            .timeout(5000)
            .sendSync(String.class)
            .ifPresentOrElse(
                response -> log.info("[Request-Response] Received response: {}", response),
                () -> log.warn("[Request-Response] No response received")
            );

        // 3. Fire-and-Forget 패턴 테스트 (응답 무시)
        MessageBuilder.create(messageGateway)
            .rest()
            .destination(testEndpoint)
            .message(incident)
            .eventDriven()  // Fire-and-Forget은 eventDriven 패턴 사용
            .correlationId(correlationId + "-fireforget")
            .header("pattern", "fire-and-forget")
            .publishEvent()  // publishEvent로 메시지 발행
            .thenAccept(success -> {
                log.info("[Fire-and-Forget] Message sent: {}", success ? "Success" : "Failed");
            });

        // 4. 비동기 Request-Response 패턴 테스트
        MessageBuilder.create(messageGateway)
            .rest()
            .destination(testEndpoint)
            .message(incident)
            .requestResponse()
            .correlationId(correlationId + "-async")
            .header("pattern", "async-request-response")
            .timeout(5000)
            .sendAsync(String.class)
            .thenAccept(optionalResponse -> {
                if (optionalResponse.isPresent()) {
                    log.info("[Async Request-Response] Received response: {}", optionalResponse.get());
                } else {
                    log.warn("[Async Request-Response] No response received");
                }
            });

        // 5. Callback 패턴 테스트
        String callbackUrl = "http://localhost:8080/api/incident/callback";
        MessageBuilder.create(messageGateway)
            .rest()
            .destination(testEndpoint)
            .message(incident)
            .eventDriven()  // Callback도 eventDriven 패턴 사용
            .correlationId(correlationId + "-callback")
            .header("pattern", "callback")
            .header("callback-url", callbackUrl)  // callback URL을 헤더로 전달
            .publishEvent()
            .thenAccept(success -> {
                log.info("[Callback] Callback registered: {}", success ? "Success" : "Failed");
            });
    }

    /**
     * Kafka test code
     */
    public void kafkaTest(UUID incidentId, Object eventData) {
        MessageBuilder.create(messageGateway)
            .kafka()
            .destination("incident.events")
            .message(eventData)
            .build()  // Kafka는 패턴 생략 가능
            .autoCorrelationId()
            .publishEvent()
            .thenAccept(success -> {
                if (success) {
                    log.info("Event published to Kafka for incident {}", incidentId);
                } else {
                    log.warn("Failed to publish event to Kafka for incident {}", incidentId);
                }
            });
    }

}
