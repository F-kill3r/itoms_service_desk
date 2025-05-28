package com.capston_design.fkiller.itoms.service_desk.config;

import com.capston_design.fkiller.itoms.service_desk.communication.MessageGateway;
import com.capston_design.fkiller.itoms.service_desk.communication.ResilientMessageGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableAsync
public class CommunicationConfig {

    @Value("${messaging.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${messaging.kafka.producer.acks}")
    private String acks;
    
    @Value("${messaging.kafka.producer.retries}")
    private int retries;
    
    @Value("${messaging.kafka.producer.batch-size}")
    private int batchSize;
    
    @Value("${messaging.kafka.producer.linger-ms}")
    private int lingerMs;
    
    @Value("${messaging.kafka.producer.buffer-memory}")
    private int bufferMemory;
    
    @Value("${messaging.kafka.producer.key-serializer}")
    private String keySerializer;
    
    @Value("${messaging.kafka.producer.value-serializer}")
    private String valueSerializer;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    @Bean
    @ConditionalOnProperty(name = "communication.type", havingValue = "rest", matchIfMissing = true)
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * JSON 직렬화/역직렬화를 위한 ObjectMapper
     * Java 8 날짜/시간 타입을 ISO-8601 형식으로 직렬화
     * DATE_TIME이 직렬화되지 않는 문제 해결
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // LocalDateTime을 ISO-8601 형식으로 직렬화
        javaTimeModule.addSerializer(LocalDateTime.class, 
            new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        
        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return objectMapper;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        
        // 추가 Kafka Producer 설정
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        
        // ObjectMapper 설정을 JsonSerializer에 전달
        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(objectMapper());
        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }


    /*
    // 메세지별로 통신방식 설정할 경우.
    // builder에 원하는 Gateway를 주입해서 사용.
    /*
    private final MessageGateway restMessageGateway;
    private final MessageGateway kafkaMessageGateway;

    public IncidentService(
            MessageGateway resilientRestMessageGateway,
            MessageGateway resilientKafkaMessageGateway) {
        this.restMessageGateway = resilientRestMessageGateway;
        this.kafkaMessageGateway = resilientKafkaMessageGateway;
    }

    // MessageBuilder.create(restMessageGateway) or MessageBuilder.create(kafkaMessageGateway)
    @Bean
    public MessageGateway resilientRestMessageGateway(@Qualifier("restMessageGateway") MessageGateway restMessageGateway) {
        return new ResilientMessageGateway(restMessageGateway);
    }


    @Bean
    public MessageGateway resilientKafkaMessageGateway(@Qualifier("kafkaMessageGateway") MessageGateway kafkaMessageGateway) {
        return new ResilientMessageGateway(kafkaMessageGateway);
    }
    */

    // 환경변수 설정으로 전체 API 통신 방식 모두 변경.
    /**
     * REST 통신용 ResilientMessageGateway
     * communication.type이 rest이거나 설정되지 않은 경우 생성
     */
    @Bean("resilientMessageGateway")
    @Primary
    @ConditionalOnProperty(name = "communication.type", havingValue = "rest", matchIfMissing = true)
    public MessageGateway resilientRestMessageGateway(@Qualifier("restMessageGateway") MessageGateway restMessageGateway) {
        return new ResilientMessageGateway(restMessageGateway);
    }

    /**
     * Kafka 통신용 ResilientMessageGateway
     * communication.type이 kafka인 경우 생성
     */
    @Bean("resilientMessageGateway")
    @Primary
    @ConditionalOnProperty(name = "communication.type", havingValue = "kafka")
    public MessageGateway resilientKafkaMessageGateway(@Qualifier("kafkaMessageGateway") MessageGateway kafkaMessageGateway) {
        return new ResilientMessageGateway(kafkaMessageGateway);
    }
} 