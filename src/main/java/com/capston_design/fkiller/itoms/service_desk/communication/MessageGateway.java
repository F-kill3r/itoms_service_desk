package com.capston_design.fkiller.itoms.service_desk.communication;

import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.Optional;

/**
 * 통신 방식을 추상화하는 메시지 게이트웨이 인터페이스
 */
public interface MessageGateway {
    
    /**
     * @param destination 목적지 (REST: URL, Kafka: Topic)
     * @param message 전송할 메시지
     * @param responseType 응답 타입
     * @param headers 추가 헤더 정보
     * @param timeoutMs 타임아웃 (밀리초)
     * @return 응답 객체 (Optional로 래핑)
     */
    <T, R> Optional<R> sendSync(String destination, T message, Class<R> responseType, 
                               Map<String, String> headers, long timeoutMs);
    
    /**
     * 동기 메시지 전송 (기본 타임아웃 사용)
     */
    default <T, R> Optional<R> sendSync(String destination, T message, Class<R> responseType) {
        return sendSync(destination, message, responseType, Map.of(), 30000L);
    }
    
    /**
     * 비동기 메시지 전송 (개선된 버전)
     * @param destination 목적지
     * @param message 전송할 메시지
     * @param responseType 응답 타입
     * @param headers 추가 헤더 정보
     * @return CompletableFuture로 래핑된 응답
     */
    <T, R> CompletableFuture<Optional<R>> sendAsync(String destination, T message, 
                                                   Class<R> responseType, Map<String, String> headers);
    
    /**
     * 비동기 메시지 전송 (기본 헤더 사용)
     */
    default <T, R> CompletableFuture<Optional<R>> sendAsync(String destination, T message, Class<R> responseType) {
        return sendAsync(destination, message, responseType, Map.of());
    }
    
    /**
     * Fire-and-forget 방식 메시지 전송 (개선된 버전)
     * @param destination 목적지
     * @param message 전송할 메시지
     * @param headers 추가 헤더 정보
     * @return 전송 성공 여부를 나타내는 CompletableFuture
     */
    <T> CompletableFuture<Boolean> sendFireAndForget(String destination, T message, Map<String, String> headers);
    
    /**
     * Fire-and-forget 방식 메시지 전송 (기본 헤더 사용)
     */
    default <T> CompletableFuture<Boolean> sendFireAndForget(String destination, T message) {
        return sendFireAndForget(destination, message, Map.of());
    }
    
    /**
     * 이벤트 발행 (Event-Driven Architecture 지원)
     * @param eventType 이벤트 타입
     * @param event 이벤트 객체
     * @param headers 추가 헤더 정보
     * @return 발행 성공 여부
     */
    <T> CompletableFuture<Boolean> publishEvent(String eventType, T event, Map<String, String> headers);
    
    /**
     * 이벤트 발행 (기본 헤더 사용)
     */
    default <T> CompletableFuture<Boolean> publishEvent(String eventType, T event) {
        return publishEvent(eventType, event, Map.of());
    }
    
    /**
     * 현재 사용 중인 통신 방식 반환
     * @return 통신 방식 (REST, KAFKA 등)
     */
    String getCommunicationType();
    
    /**
     * 통신 상태 확인
     * @return 통신 가능 여부
     */
    boolean isHealthy();
    
    /**
     * 통신 메트릭 정보 반환
     * @return 메트릭 정보 맵
     */
    Map<String, Object> getMetrics();
} 