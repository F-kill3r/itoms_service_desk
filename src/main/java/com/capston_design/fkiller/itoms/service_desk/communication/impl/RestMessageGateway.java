package com.capston_design.fkiller.itoms.service_desk.communication.impl;

import com.capston_design.fkiller.itoms.service_desk.communication.MessageGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST 메시지 게이트웨이
 */
@Slf4j
@Component("restMessageGateway")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "communication.type", havingValue = "rest", matchIfMissing = true)
public class RestMessageGateway implements MessageGateway {
    
    private final RestTemplate restTemplate;
    
    @Value("${messaging.rest.base-url}")
    private String restBaseUrl;
    
    // 메트릭 수집용
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    
    @Override
    public <T, R> Optional<R> sendSync(String destination, T message, Class<R> responseType, 
                                      Map<String, String> headers, long timeoutMs) {
        log.info("Sending sync REST request to: {}", destination);
        totalRequests.incrementAndGet();
        
        try {
            HttpHeaders httpHeaders = createHttpHeaders(headers);
            HttpEntity<T> entity = new HttpEntity<>(message, httpHeaders);
            
            R response = restTemplate.exchange(destination, HttpMethod.POST, entity, responseType).getBody();
            successfulRequests.incrementAndGet();
            return Optional.ofNullable(response);
        } catch (Exception e) {
            failedRequests.incrementAndGet();
            log.error("Error sending sync REST request to {}: {}", destination, e.getMessage());
            throw new RuntimeException("REST communication failed", e);
        }
    }
    
    @Override
    @Async
    public <T, R> CompletableFuture<Optional<R>> sendAsync(String destination, T message, 
                                                          Class<R> responseType, Map<String, String> headers) {
        log.info("Sending async REST request to: {}", destination);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendSync(destination, message, responseType, headers, 30000L);
            } catch (Exception e) {
                log.error("Error in async REST request: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public <T> CompletableFuture<Boolean> sendFireAndForget(String destination, T message, Map<String, String> headers) {
        log.info("Sending fire-and-forget REST request to: {}", destination);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                sendSync(destination, message, String.class, headers, 30000L);
                return true;
            } catch (Exception e) {
                log.warn("Fire-and-forget REST request failed to {}: {}", destination, e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public <T> CompletableFuture<Boolean> publishEvent(String eventType, T event, Map<String, String> headers) {
        log.info("Publishing event {} via REST API", eventType);

        // destination이 전체 URL이면 그대로 사용, 그렇지 않으면 base URL + eventType 조합
        String destination;
        if (eventType.startsWith("http://") || eventType.startsWith("https://")) {
            destination = eventType;
        } else {
            // 상대 경로인 경우 base URL과 조합
            destination = restBaseUrl + "/api/events/" + eventType;
        }
        
        return sendFireAndForget(destination, event, headers);
    }
    
    @Override
    public String getCommunicationType() {
        return "REST";
    }
    
    @Override
    public boolean isHealthy() {
        // 간단한 헬스체크 - 실제로는 더 정교한 로직 필요
        try {
            // 헬스체크 엔드포인트 호출 등
            return true;
        } catch (Exception e) {
            log.warn("REST health check failed: {}", e.getMessage());
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
            "communication_type", "REST"
        );
    }
    
    /**
     * HTTP 헤더 생성
     */
    private HttpHeaders createHttpHeaders(Map<String, String> customHeaders) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 커스텀 헤더 추가
        if (customHeaders != null) {
            customHeaders.forEach(headers::add);
        }
        
        return headers;
    }
} 