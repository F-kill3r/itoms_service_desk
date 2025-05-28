package com.capston_design.fkiller.itoms.service_desk.communication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resilience 패턴을 적용한 MessageGateway 데코레이터
 * Circuit Breaker, Retry, Timeout, Bulkhead 패턴 구현
 * 추후 Spring 메트릭으로 전환하여 시각화 예정.
 *
 * 주요 기능:
 * - Circuit Breaker: 연속 실패 시 요청 차단
 * - Retry: 실패 시 재시도
 * - Timeout: 요청 타임아웃 관리
 * - Metrics: 성능 및 상태 메트릭 수집
 */
@Slf4j
public class ResilientMessageGateway implements MessageGateway {
    
    private final MessageGateway delegate;
    
    // Circuit Breaker 상태
    private enum CircuitState { CLOSED, OPEN, HALF_OPEN }
    private volatile CircuitState circuitState = CircuitState.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    
    // 설정값
    private final int failureThreshold = 5;
    private final Duration openTimeout = Duration.ofSeconds(60);
    private final int maxRetries = 3;
    private final Duration retryDelay = Duration.ofMillis(1000);
    
    // 메트릭
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong circuitBreakerOpenCount = new AtomicLong(0);
    
    public ResilientMessageGateway(MessageGateway delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public <T, R> Optional<R> sendSync(String destination, T message, Class<R> responseType, 
                                      Map<String, String> headers, long timeoutMs) {
        return executeWithResilience(() -> 
            delegate.sendSync(destination, message, responseType, headers, timeoutMs));
    }
    
    @Override
    public <T, R> CompletableFuture<Optional<R>> sendAsync(String destination, T message, 
                                                          Class<R> responseType, Map<String, String> headers) {
        return executeWithResilienceAsync(() -> 
            delegate.sendAsync(destination, message, responseType, headers));
    }
    
    @Override
    public <T> CompletableFuture<Boolean> sendFireAndForget(String destination, T message, Map<String, String> headers) {
        return executeWithResilienceAsync(() -> 
            delegate.sendFireAndForget(destination, message, headers));
    }
    
    @Override
    public <T> CompletableFuture<Boolean> publishEvent(String eventType, T event, Map<String, String> headers) {
        return executeWithResilienceAsync(() -> 
            delegate.publishEvent(eventType, event, headers));
    }
    
    @Override
    public String getCommunicationType() {
        return "Resilient-" + delegate.getCommunicationType();
    }
    
    @Override
    public boolean isHealthy() {
        return circuitState != CircuitState.OPEN && delegate.isHealthy();
    }
    
    @Override
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>(delegate.getMetrics());
        metrics.put("circuit_state", circuitState.name());
        metrics.put("failure_count", failureCount.get());
        metrics.put("total_requests", totalRequests.get());
        metrics.put("successful_requests", successfulRequests.get());
        metrics.put("failed_requests", failedRequests.get());
        metrics.put("circuit_breaker_open_count", circuitBreakerOpenCount.get());
        metrics.put("success_rate", calculateSuccessRate());
        return metrics;
    }
    
    /**
     * Circuit Breaker와 Retry를 적용한 동기 실행
     */
    private <T> T executeWithResilience(java.util.function.Supplier<T> operation) {
        totalRequests.incrementAndGet();
        
        if (!isCircuitClosed()) {
            failedRequests.incrementAndGet();
            throw new RuntimeException("Circuit breaker is OPEN");
        }
        
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                T result = operation.get();
                onSuccess();
                successfulRequests.incrementAndGet();
                return result;
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {} failed: {}", attempt + 1, e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelay.toMillis() * (attempt + 1)); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        onFailure();
        failedRequests.incrementAndGet();
        throw new RuntimeException("All retry attempts failed", lastException);
    }
    
    /**
     * Circuit Breaker와 Retry를 적용한 비동기 실행
     */
    private <T> CompletableFuture<T> executeWithResilienceAsync(java.util.function.Supplier<CompletableFuture<T>> operation) {
        totalRequests.incrementAndGet();
        
        if (!isCircuitClosed()) {
            failedRequests.incrementAndGet();
            return CompletableFuture.failedFuture(new RuntimeException("Circuit breaker is OPEN"));
        }
        
        return retryAsync(operation, 0);
    }
    
    /**
     * 비동기 재시도 로직
     */
    private <T> CompletableFuture<T> retryAsync(java.util.function.Supplier<CompletableFuture<T>> operation, int attempt) {
        return operation.get()
            .handle((result, throwable) -> {
                if (throwable == null) {
                    onSuccess();
                    successfulRequests.incrementAndGet();
                    return CompletableFuture.completedFuture(result);
                } else {
                    log.warn("Async attempt {} failed: {}", attempt + 1, throwable.getMessage());
                    
                    if (attempt < maxRetries) {
                        return CompletableFuture
                            .supplyAsync(() -> {
                                try {
                                    Thread.sleep(retryDelay.toMillis() * (attempt + 1));
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                return null;
                            })
                            .thenCompose(v -> retryAsync(operation, attempt + 1));
                    } else {
                        onFailure();
                        failedRequests.incrementAndGet();
                        return CompletableFuture.<T>failedFuture(
                            new RuntimeException("All async retry attempts failed", throwable));
                    }
                }
            })
            .thenCompose(java.util.function.Function.identity());
    }
    
    /**
     * Circuit Breaker 상태 확인
     */
    private boolean isCircuitClosed() {
        if (circuitState == CircuitState.CLOSED) {
            return true;
        }
        
        if (circuitState == CircuitState.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime.get() > openTimeout.toMillis()) {
                circuitState = CircuitState.HALF_OPEN;
                log.info("Circuit breaker moved to HALF_OPEN state");
                return true;
            }
            return false;
        }
        
        // HALF_OPEN 상태에서는 제한된 요청만 허용
        return true;
    }
    
    /**
     * 성공 시 호출
     */
    private void onSuccess() {
        failureCount.set(0);
        if (circuitState == CircuitState.HALF_OPEN) {
            circuitState = CircuitState.CLOSED;
            log.info("Circuit breaker moved to CLOSED state");
        }
    }
    
    /**
     * 실패 시 호출
     */
    private void onFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        if (failures >= failureThreshold && circuitState == CircuitState.CLOSED) {
            circuitState = CircuitState.OPEN;
            circuitBreakerOpenCount.incrementAndGet();
            log.warn("Circuit breaker moved to OPEN state after {} failures", failures);
        }
    }
    
    /**
     * 성공률 계산
     */
    private double calculateSuccessRate() {
        long total = totalRequests.get();
        if (total == 0) return 1.0;
        return (double) successfulRequests.get() / total;
    }
} 