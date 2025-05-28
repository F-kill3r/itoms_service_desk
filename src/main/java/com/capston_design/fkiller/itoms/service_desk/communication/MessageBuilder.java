package com.capston_design.fkiller.itoms.service_desk.communication;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.UUID;

/**
 * 메시지 빌더
 * 
 * 사용 예시:
 * MessageBuilder.create(messageGateway)
 *     .type("REST")
 *     .destination("http://user-service:8080/api/notify")
 *     .message(incident)
 *     .pattern("REQUEST_RESPONSE")  // REST일 때만 필수
 *     .build()  // 필수 요소 검증 후 ExecutableBuilder 반환
 *     .correlationId("12345")  // 선택적 요소
 *     .timeout(5000)
 *     .sendAsync(IncidentResponse.class);
 */
public class MessageBuilder<T> {
    
    private final MessageGateway gateway;
    private String communicationType;
    private String destination;
    private T message;
    private String communicationPattern;
    
    private MessageBuilder(MessageGateway gateway) {
        this.gateway = gateway;
    }

    public static TypeBuilder create(MessageGateway gateway) {
        return new TypeBuilder(gateway);
    }

    public static class TypeBuilder {
        private final MessageGateway gateway;
        
        private TypeBuilder(MessageGateway gateway) {
            this.gateway = gateway;
        }
        
        public DestinationBuilder type(String communicationType) {
            return new DestinationBuilder(gateway, communicationType);
        }
        
        public DestinationBuilder rest() {
            return type("REST");
        }
        
        public DestinationBuilder kafka() {
            return type("KAFKA");
        }
        
        public DestinationBuilder grpc() {
            return type("GRPC");
        }
    }

    public static class DestinationBuilder {
        private final MessageGateway gateway;
        private final String communicationType;
        
        private DestinationBuilder(MessageGateway gateway, String communicationType) {
            this.gateway = gateway;
            this.communicationType = communicationType;
        }
        
        public MessageSetBuilder destination(String destination) {
            return new MessageSetBuilder(gateway, communicationType, destination);
        }
        
        public MessageSetBuilder serviceDestination(String serviceUrlKey, String endpoint) {
            String serviceUrl = System.getenv(serviceUrlKey);
            if (serviceUrl == null) {
                throw new IllegalArgumentException("Service URL not found for key: " + serviceUrlKey);
            }
            return destination(serviceUrl + endpoint);
        }
    }

    public static class MessageSetBuilder {
        private final MessageGateway gateway;
        private final String communicationType;
        private final String destination;
        
        private MessageSetBuilder(MessageGateway gateway, String communicationType, String destination) {
            this.gateway = gateway;
            this.communicationType = communicationType;
            this.destination = destination;
        }
        
        public <U> PatternBuilder<U> message(U message) {
            return new PatternBuilder<>(gateway, communicationType, destination, message);
        }
    }
    
    // REST일 때만 필수
    public static class PatternBuilder<T> {
        private final MessageGateway gateway;
        private final String communicationType;
        private final String destination;
        private final T message;
        
        private PatternBuilder(MessageGateway gateway, String communicationType, String destination, T message) {
            this.gateway = gateway;
            this.communicationType = communicationType;
            this.destination = destination;
            this.message = message;
        }
        
        // REST 패턴 설정 메서드들
        public ExecutableBuilder<T> requestResponse() {
            return pattern("REQUEST_RESPONSE");
        }
        
        public ExecutableBuilder<T> callback(String callbackUrl) {
            return pattern("CALLBACK").header("callback-url", callbackUrl);
        }
        
        public ExecutableBuilder<T> eventDriven() {
            return pattern("EVENT_DRIVEN");
        }
        
        public ExecutableBuilder<T> pattern(String pattern) {
            return new ExecutableBuilder<>(gateway, communicationType, destination, message, pattern);
        }
        
        // REST가 아닌 경우 패턴 생략 가능(카프카일 때는, EVENT_DRIVEN)
        public ExecutableBuilder<T> build() {
            String defaultPattern = "KAFKA".equals(communicationType) ? "EVENT_DRIVEN" : "REQUEST_RESPONSE";
            return new ExecutableBuilder<>(gateway, communicationType, destination, message, defaultPattern);
        }
    }

    // 추가 옵션 설정
    public static class ExecutableBuilder<T> {
        private final MessageGateway gateway;
        private final String communicationType;
        private final String destination;
        private final T message;
        private final String communicationPattern;
        private final Map<String, String> headers = new HashMap<>();
        private long timeoutMs = 30000L;
        
        private ExecutableBuilder(MessageGateway gateway, String communicationType, String destination, 
                                T message, String communicationPattern) {
            this.gateway = gateway;
            this.communicationType = communicationType;
            this.destination = destination;
            this.message = message;
            this.communicationPattern = communicationPattern;
            
            // 기본 헤더 설정
            header("communication-type", communicationType);
            header("communication-pattern", communicationPattern);
            header("timestamp", java.time.LocalDateTime.now().toString());
        }
        
        // 선택적 요소들
        public ExecutableBuilder<T> correlationId(String correlationId) {
            return header("correlation-id", correlationId);
        }
        
        public ExecutableBuilder<T> autoCorrelationId() {
            return correlationId(UUID.randomUUID().toString());
        }
        
        public ExecutableBuilder<T> priority(String priority) {
            return header("priority", priority);
        }
        
        public ExecutableBuilder<T> header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }
        
        public ExecutableBuilder<T> headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }
        
        public ExecutableBuilder<T> timeout(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }
        
        public ExecutableBuilder<T> source(String source) {
            return header("source", source);
        }
        
        public ExecutableBuilder<T> version(String version) {
            return header("version", version);
        }
        
        // 실행 메서드들
        public <R> Optional<R> sendSync(Class<R> responseType) {
            logCommunicationInfo("Sync");
            return gateway.sendSync(destination, message, responseType, headers, timeoutMs);
        }
        
        public <R> CompletableFuture<Optional<R>> sendAsync(Class<R> responseType) {
            logCommunicationInfo("Async");
            return gateway.sendAsync(destination, message, responseType, headers);
        }
        
        public CompletableFuture<Boolean> sendFireAndForget() {
            logCommunicationInfo("Fire-and-forget");
            return gateway.sendFireAndForget(destination, message, headers);
        }
        
        public CompletableFuture<Boolean> publishEvent() {
            logCommunicationInfo("Event publish");
            return gateway.publishEvent(destination, message, headers);
        }
        
        private void logCommunicationInfo(String operationType) {
            System.out.println(String.format("[MessageBuilder] %s operation - Type: %s, Pattern: %s, Destination: %s", 
                                            operationType, communicationType, communicationPattern, destination));
        }
    }
    
    // 하위 호환성을 위한 간단한 정적 메서드들
    public static CompletableFuture<Boolean> publishSimpleEvent(MessageGateway gateway, String eventType, Object event) {
        return create(gateway)
            .rest()
            .destination(eventType)
            .message(event)
            .eventDriven()
            .autoCorrelationId()
            .publishEvent();
    }
    
    public static CompletableFuture<Boolean> sendSimple(MessageGateway gateway, String destination, Object message) {
        return create(gateway)
            .rest()
            .destination(destination)
            .message(message)
            .requestResponse()
            .autoCorrelationId()
            .sendFireAndForget();
    }
    
    public static <R> Optional<R> callSimple(MessageGateway gateway, String destination, Object message, Class<R> responseType) {
        return create(gateway)
            .rest()
            .destination(destination)
            .message(message)
            .requestResponse()
            .autoCorrelationId()
            .sendSync(responseType);
    }
} 