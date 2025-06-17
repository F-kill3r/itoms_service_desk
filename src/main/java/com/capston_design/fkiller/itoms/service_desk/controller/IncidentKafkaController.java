package com.capston_design.fkiller.itoms.service_desk.controller;

import com.capston_design.fkiller.itoms.service_desk.apiPayload.ApiResponse;
import com.capston_design.fkiller.itoms.service_desk.converter.IncidentConverter;
import com.capston_design.fkiller.itoms.service_desk.dto.IncidentRequest;
import com.capston_design.fkiller.itoms.service_desk.dto.IncidentResponse;
import com.capston_design.fkiller.itoms.service_desk.dto.TicketCompletedRequestDTO;
import com.capston_design.fkiller.itoms.service_desk.model.Incident;
import com.capston_design.fkiller.itoms.service_desk.service.IncidentKafkaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incident-kafka")
@RequiredArgsConstructor
public class IncidentKafkaController {

    private final IncidentKafkaService incidentKafkaService;

    /**
     * Incident 생성 요청 -> 내부적으로 Kafka publish
     * 클라이언트에게는 202 ACCEPTED 반환 (비동기)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<IncidentResponse.IncidentCreateResponseDTO>> createIncidentKafka(
            @RequestBody IncidentRequest incidentRequest) {

        Incident incident = incidentKafkaService.createIncidentKafka(incidentRequest);
        var responseDTO = IncidentConverter.toIncidentResponseDTO(incident);
        // 비동기지만 클라이언트에선 완료된 Incident 정보를 받도록 함
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.onSuccess(responseDTO));
    }

    @PostMapping("/v1/ticket/complete")
    public ResponseEntity<ApiResponse<IncidentResponse.IncidentCreateResponseDTO>> completeIncidentKafka(
            @RequestBody TicketCompletedRequestDTO request) {
        Incident incident = incidentKafkaService.completeIncidentKafka(request.ticketId(), request.incidentId());
        var responseDTO = IncidentConverter.toIncidentResponseDTO(incident);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.onSuccess(responseDTO));
    }
} 