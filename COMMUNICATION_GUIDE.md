# ITOMS Service Desk - MSA 통신 시스템 가이드

이 가이드는 ITOMS Service Desk에서 마이크로서비스 간 통신을 위한 통합된 MessageBuilder 시스템 사용법을 설명합니다.

## 🏗️ 아키텍처 개요

### 통합 통신 시스템 구조
```
Controller Layer
      ↓
Service Layer (with MessageBuilder + Pattern Selection)
      ↓
ResilientMessageGateway (Circuit Breaker, Retry, Metrics)
      ↓
MessageGateway Interface
      ↓
┌────────────────────┬─────────────────────┐
│ RestMessageGateway │ KafkaMessageGateway │
└────────────────────┴─────────────────────┘
```

### 주요 특징
- **🔀 패턴 선택**: REST 통신에서 Request-Response, Callback, Event-Driven 패턴 선택 가능
- **🎯 타입 선택**: REST, Kafka, gRPC(향후) 등 통신 타입 선택
- **🛡️ Resilience**: Circuit Breaker, Retry, Timeout 자동 적용
- **📊 관찰성**: 분산 추적, 메트릭 수집, 구조화된 로깅
- **🌐 MSA 지원**: 환경변수 기반 서비스 디스커버리

## 🚀 MessageBuilder 사용법

### 1. 기본 구조

```java
MessageBuilder.create(messageGateway)
    .type("통신타입")           // REST, KAFKA, GRPC
    .destination("서비스URL/endpoint")
    .message(데이터)
    .pattern("통신패턴")        // REQUEST_RESPONSE, CALLBACK, EVENT_DRIVEN
    .correlationId("추적ID")
    .timeout(5000)
    .sendSync(ResponseType.class);
```

### 2. REST 통신 패턴별 사용법

#### **Request-Response 패턴** (직접 응답 받기)
```java
// 동기 방식
Optional<String> response = MessageBuilder.create(messageGateway)
    .rest()
    .serviceDestination("USER_SERVICE_URL", "/api/users/" + userId + "/validate")
    .message(Map.of(
        "incidentId", incidentId,
        "userId", userId,
        "timestamp", LocalDateTime.now().toString()
    ))
    .requestResponse()
    .autoCorrelationId()
    .timeout(5000)
    .sendSync(String.class);

// 비동기 방식
CompletableFuture<Optional<String>> futureResponse = MessageBuilder.create(messageGateway)
    .rest()
    .destination(getDestination())
    .message(Map.of(
        "incidentId", incidentId,
        "action", "process",
        "requestedAt", LocalDateTime.now().toString()
    ))
    .requestResponse()
    .correlationId(correlationId)
    .header("operation", "process-incident")
    .timeout(10000)
    .sendAsync(String.class);
```

#### **Callback 패턴** (콜백 URL로 응답 받기)
```java
MessageBuilder.create(messageGateway)
    .rest()
    .serviceDestination("PAYMENT_SERVICE_URL", "/api/payments/process")
    .message(paymentData)
    .callback("http://incident-service:8080/api/incident/" + incidentId + "/payment-callback")
    .autoCorrelationId()
    .header("payment-type", "incident-payment")
    .sendFireAndForget()
    .thenAccept(success -> {
        if (success) {
            log.info("Payment processing initiated for incident {}", incidentId);
        } else {
            log.warn("Failed to initiate payment processing for incident {}", incidentId);
        }
    });
```

#### **Event-Driven 패턴** (이벤트 발행)
```java
MessageBuilder.create(messageGateway)
    .rest()
    .destination("http://event-bus:8080/api/events/incident-updated")
    .message(Map.of(
        "incidentId", incidentId,
        "updateType", updateType,
        "updateData", updateData,
        "timestamp", LocalDateTime.now().toString()
    ))
    .eventDriven()
    .autoCorrelationId()
    .header("event-source", "incident-service")
    .header("event-version", "v1.0")
    .publishEvent()
    .thenAccept(success -> {
        if (success) {
            log.info("Incident update event broadcasted for incident {}", incidentId);
        } else {
            log.warn("Failed to broadcast incident update for incident {}", incidentId);
        }
    });
```

### 3. Kafka 통신

```java
// 이벤트 발행
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
```

### 4. 환경변수 기반 서비스 호출

```java
// 환경변수 USER_SERVICE_URL 사용
MessageBuilder.create(messageGateway)
    .rest()
    .serviceDestination("USER_SERVICE_URL", "/api/notifications")
    .message(Map.of(
        "incidentId", incidentId,
        "message", message,
        "timestamp", LocalDateTime.now().toString()
    ))
    .requestResponse()
    .autoCorrelationId()
    .header("source-service", "incident-service")
    .sendFireAndForget();
```

## ⚙️ 설정

### application.yml
```yaml
# 기본 통신 방식 (REST 기본값)
communication:
  type: ${COMMUNICATION_TYPE:rest}

# MSA 서비스 URL 설정
services:
  user-service:
    url: ${USER_SERVICE_URL:http://localhost:8081}
  notification-service:
    url: ${NOTIFICATION_SERVICE_URL:http://localhost:8082}
  payment-service:
    url: ${PAYMENT_SERVICE_URL:http://localhost:8083}

# REST 설정
messaging:
  rest:
    base-url: ${REST_BASE_URL:http://localhost:8080}
    timeout: ${REST_TIMEOUT:30000}
    endpoints:
      incident-create: /api/external/incident
      incident-update: /api/external/incident/update

# Kafka 설정
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:100.69.13.48:3001}
    topics:
      incident-create: ${KAFKA_TOPIC_INCIDENT_CREATE:itoms.incident.create}
    producer:
      acks: all
      retries: 3
```

## 🎯 실제 사용 사례

### 사례 1: 복합 처리 (여러 패턴 조합)
```java
public CompletableFuture<String> complexIncidentProcessing(Long incidentId) {
    return CompletableFuture.supplyAsync(() -> {
        // 1. Request-Response: 사용자 검증
        String userValidation = validateUserSync(incidentId, 123L);
        
        // 2. Callback: 결제 처리 (비동기)
        Map<String, Object> paymentData = Map.of("amount", 100.0, "currency", "USD");
        processPaymentWithCallback(incidentId, paymentData);
        
        // 3. Event-Driven: 상태 변경 알림
        broadcastIncidentUpdate(incidentId, "PROCESSING_STARTED", Map.of("status", "IN_PROGRESS"));
        
        return "Complex processing initiated for incident " + incidentId;
    });
}
```

### 사례 2: 간소화된 이벤트 발행
```java
// 정적 메서드 사용 - 가장 간단한 방식
MessageBuilder.publishSimpleEvent(messageGateway, "incident.created", incident)
    .thenAccept(success -> {
        if (success) {
            log.info("Incident created event published successfully for incident {}", incident.getId());
        } else {
            log.error("Failed to publish incident created event for incident {}", incident.getId());
        }
    });
```

### 사례 3: 간소화된 서비스 호출
```java
// 정적 메서드 사용 - 동기 호출
Optional<String> response = MessageBuilder.callSimple(messageGateway, serviceUrl, requestData, String.class);
return response.orElse("No response received");
```

## 📊 모니터링 및 관찰성

### 분산 추적
- **Correlation ID**: 모든 요청에 자동 추가 (`autoCorrelationId()`)
- **통신 패턴**: 로그에 패턴 정보 포함
- **타이밍**: 요청/응답 시간 추적

### 로그 예시
```
[MessageBuilder] Sync operation - Type: REST, Pattern: REQUEST_RESPONSE, Destination: http://user-service:8080/api/validate
[MessageBuilder] Fire-and-forget operation - Type: REST, Pattern: CALLBACK, Destination: http://payment-service:8080/api/process
[MessageBuilder] Event publish operation - Type: KAFKA, Pattern: EVENT_DRIVEN, Destination: incident.events
```

### 메트릭 수집
```java
// 통신 상태 및 메트릭 조회
Map<String, Object> status = Map.of(
    "type", messageGateway.getCommunicationType(),
    "healthy", messageGateway.isHealthy(),
    "metrics", messageGateway.getMetrics()
);
```

## 🛡️ Resilience 패턴

### 자동 적용되는 패턴들
- **Circuit Breaker**: 연속 실패 시 요청 차단
- **Retry**: 실패 시 자동 재시도
- **Timeout**: 요청별 타임아웃 설정 (기본값: 30초)
- **Bulkhead**: 리소스 격리

### 설정 커스터마이징
```yaml
messaging:
  resilience:
    circuit-breaker:
      failure-rate-threshold: 50
      wait-duration-in-open-state: 30000
    retry:
      max-attempts: 3
      wait-duration: 1000
```

## 🚨 모범 사례

### 1. 패턴 선택 가이드라인
- **Request-Response**: 즉시 응답이 필요한 검증, 조회
- **Callback**: 긴 처리 시간이 필요한 결제, 분석
- **Event-Driven**: 여러 서비스에 알려야 하는 상태 변경

### 2. 에러 처리
```java
try {
    var response = MessageBuilder.create(messageGateway)
        .rest()
        .destination(getDestination())
        .message(data)
        .requestResponse()
        .correlationId(correlationId)
        .timeout(5000)
        .sendSync(String.class);
    
    if (response.isPresent()) {
        log.info("Processing completed: {}", response.get());
        return response.get();
    } else {
        log.warn("No response received");
        return "No response received";
    }
} catch (Exception e) {
    log.error("Processing failed: {}", e.getMessage());
    throw new RuntimeException("Processing failed", e);
}
```

### 3. 보안 고려사항
```java
MessageBuilder.create(messageGateway)
    .rest()
    .destination("http://secure-service:8080/api")
    .message(data)
    .header("Authorization", "Bearer " + token)
    .header("Content-Type", "application/json")
    .sendFireAndForget();
```

## 🔮 향후 계획

### 단기 계획
- ✅ REST 패턴 선택 기능
- ✅ 환경변수 기반 서비스 디스커버리
- ✅ ApiResponse 표준화
- 🔄 MessageConsumer 인터페이스

### 장기 계획
- gRPC 지원 추가
- Service Mesh 연동
- AI 기반 라우팅
- 자동 스케일링 연동

이 통합 통신 시스템을 통해 마이크로서비스 간 효율적이고 안정적인 통신을 구현할 수 있습니다. 