package com.capston_design.fkiller.itoms.service_desk.communication.impl;

import com.capston_design.fkiller.itoms.service_desk.communication.MessageGateway;
import com.capston_design.fkiller.itoms.service_desk.model.Incident;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka를 사용하는 메시지 게이트웨이
 */
@Slf4j
@Component("kafkaMessageGateway")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "communication.type", havingValue = "kafka")
public class KafkaMessageGateway implements MessageGateway {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    // 응답을 기다리는 요청들을 관리하는 맵. Redis나 memcached 대용
    private final ConcurrentHashMap<String, CompletableFuture<Object>> pendingRequests = new ConcurrentHashMap<>();
    
    // 메트릭 수집용
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    
    /**
     * 메시지 키 생성
     * 인시던트의 경우: INCIDENT_{id}_{timestamp}
     * 기타 메시지의 경우: MSG_{timestamp}_{randomUUID}
     */
    private String generateMessageKey(Object message) {
        String timestamp = LocalDateTime.now().format(KEY_FORMATTER);
        
        if (message instanceof Incident) {
            Incident incident = (Incident) message;
            String incidentId = incident.getId() != null ? incident.getId().toString() : "unknown";
            return String.format("INCIDENT_%s_%s", incidentId, timestamp);
        }
        
        return String.format("MSG_%s_%s", timestamp, java.util.UUID.randomUUID().toString());
    }
    
    @Override
    public <T, R> Optional<R> sendSync(String destination, T message, Class<R> responseType, 
                                      Map<String, String> headers, long timeoutMs) {
        log.info("Sending sync Kafka message to topic: {}", destination);
        totalRequests.incrementAndGet();
        
        try {
            // Kafka는 기본적으로 비동기이므로, 동기 방식은 제한된 시간 내에서 응답을 기다림
            CompletableFuture<Optional<R>> future = sendAsync(destination, message, responseType, headers);
            Optional<R> result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            successfulRequests.incrementAndGet();
            return result;
        } catch (Exception e) {
            failedRequests.incrementAndGet();
            log.error("Error sending sync Kafka message to {}: {}", destination, e.getMessage());
            throw new RuntimeException("Kafka sync communication failed", e);
        }
    }
    
    @Override
    public <T, R> CompletableFuture<Optional<R>> sendAsync(String destination, T message, 
                                                          Class<R> responseType, Map<String, String> headers) {
        log.info("Sending async Kafka message to topic: {}", destination);
        
        CompletableFuture<Optional<R>> resultFuture = new CompletableFuture<>();
        
        try {
            String messageKey = generateMessageKey(message);
            String correlationId = headers != null ? headers.get("correlation-id") : java.util.UUID.randomUUID().toString();
            
            // 메시지를 JSON으로 변환하고 헤더 정보 포함
            Map<String, Object> messageWithHeaders = Map.of(
                "payload", message,
                "headers", headers != null ? headers : Map.of(),
                "correlationId", correlationId,
                "timestamp", LocalDateTime.now().toString()
            );
            String jsonMessage = objectMapper.writeValueAsString(messageWithHeaders);
            
            // Kafka로 메시지 전송 (키 포함)
            CompletableFuture<SendResult<String, Object>> kafkaFuture = 
                kafkaTemplate.send(destination, messageKey, jsonMessage);
            
            kafkaFuture.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send Kafka message: {}", ex.getMessage());
                    resultFuture.completeExceptionally(ex);
                } else {
                    log.info("Kafka message sent successfully to topic: {} with key: {} and correlation ID: {}", 
                            destination, messageKey, correlationId);
                    
                    // 실제 구현에서는 응답 토픽에서 correlation ID로 응답을 매칭
                    // 여기서는 간단히 성공 응답을 시뮬레이션
                    if (responseType == String.class) {
                        resultFuture.complete(Optional.of((R) "Message sent successfully"));
                    } else {
                        // 실제로는 응답 토픽에서 메시지를 받아서 처리
                        resultFuture.complete(Optional.empty());
                    }
                }
            });
            
        } catch (Exception e) {
            log.error("Error preparing Kafka message: {}", e.getMessage());
            resultFuture.completeExceptionally(e);
        }
        
        return resultFuture;
    }
    
    @Override
    public <T> CompletableFuture<Boolean> sendFireAndForget(String destination, T message, Map<String, String> headers) {
        log.info("Sending fire-and-forget Kafka message to topic: {}", destination);
        
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        
        try {
            String messageKey = generateMessageKey(message);

            Map<String, Object> messageWithHeaders = Map.of(
                "payload", message,
                "headers", headers != null ? headers : Map.of(),
                "timestamp", LocalDateTime.now().toString()
            );
            String jsonMessage = objectMapper.writeValueAsString(messageWithHeaders);
            
            kafkaTemplate.send(destination, messageKey, jsonMessage)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Fire-and-forget Kafka message failed to {}: {}", destination, ex.getMessage());
                        resultFuture.complete(false);
                    } else {
                        log.debug("Fire-and-forget Kafka message sent successfully to: {} with key: {}", 
                                destination, messageKey);
                        resultFuture.complete(true);
                    }
                });
        } catch (Exception e) {
            log.warn("Error sending fire-and-forget Kafka message: {}", e.getMessage());
            resultFuture.complete(false);
        }
        
        return resultFuture;
    }
    
    @Override
    public <T> CompletableFuture<Boolean> publishEvent(String eventType, T event, Map<String, String> headers) {
        log.info("Publishing event {} to Kafka", eventType);
        
        // 이벤트 타입을 토픽명으로 사용하거나 이벤트 전용 토픽 사용
        String eventTopic = "events." + eventType;
        
        // 이벤트 헤더에 이벤트 타입 정보 추가
        Map<String, String> eventHeaders = headers != null ? 
            new java.util.HashMap<>(headers) : new java.util.HashMap<>();
        eventHeaders.put("event-type", eventType);
        eventHeaders.put("event-timestamp", LocalDateTime.now().toString());
        
        return sendFireAndForget(eventTopic, event, eventHeaders);
    }
    
    @Override
    public String getCommunicationType() {
        return "KAFKA";
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Kafka 클러스터 연결 상태 확인
            // 실제로는 kafkaTemplate.getProducerFactory().createProducer().partitionsFor("test-topic") 등으로 확인
            return true;
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getMetrics() {
        long total = totalRequests.get();
        double successRate = total > 0 ? (double) successfulRequests.get() / total : 1.0;
        
        return Map.of(
            "total_requests", total,
            "successful_requests", successfulRequests.get(),
            "failed_requests", failedRequests.get(),
            "success_rate", successRate,
            "communication_type", "KAFKA",
            "pending_requests", pendingRequests.size()
        );
    }
} 