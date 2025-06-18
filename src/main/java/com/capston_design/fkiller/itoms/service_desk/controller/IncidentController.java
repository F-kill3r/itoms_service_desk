package com.capston_design.fkiller.itoms.service_desk.controller;

import com.capston_design.fkiller.itoms.service_desk.apiPayload.ApiResponse;
import com.capston_design.fkiller.itoms.service_desk.converter.IncidentConverter;
import com.capston_design.fkiller.itoms.service_desk.dto.IncidentRequest;
import com.capston_design.fkiller.itoms.service_desk.dto.IncidentResponse;
import com.capston_design.fkiller.itoms.service_desk.dto.TicketCompletedRequestDTO;
import com.capston_design.fkiller.itoms.service_desk.model.Incident;
import com.capston_design.fkiller.itoms.service_desk.service.IncidentService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incident")
public class IncidentController {
    private final IncidentService incidentService;

    public IncidentController(@Qualifier("grpc") IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IncidentResponse.IncidentCreateResponseDTO>> createIncident(
            @RequestBody IncidentRequest incidentRequest) {

        Incident incident = incidentService.createIncident(incidentRequest);
        var responseDTO = IncidentConverter.toIncidentResponseDTO(incident);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.onSuccess(responseDTO));
    }

    @PostMapping("/v1/ticket/complete")
    public ResponseEntity<ApiResponse<IncidentResponse.IncidentCreateResponseDTO>> completeIncident(
            @RequestBody TicketCompletedRequestDTO request) {
        Incident incident = incidentService.completeTicket(request.ticketId(), request.incidentId());
        var responseDTO = IncidentConverter.toIncidentResponseDTO(incident);
        return ResponseEntity.ok(ApiResponse.onSuccess(responseDTO));
    }
}
