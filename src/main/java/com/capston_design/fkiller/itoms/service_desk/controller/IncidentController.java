package com.capston_design.fkiller.itoms.service_desk.controller;

import com.capston_design.fkiller.itoms.service_desk.apiPayload.ApiResponse;
import com.capston_design.fkiller.itoms.service_desk.converter.IncidentConverter;
import com.capston_design.fkiller.itoms.service_desk.dto.IncidentRequest;
import com.capston_design.fkiller.itoms.service_desk.dto.IncidentResponse;
import com.capston_design.fkiller.itoms.service_desk.model.Incident;
import com.capston_design.fkiller.itoms.service_desk.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incident")
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;

    @PostMapping
    public ResponseEntity<ApiResponse<IncidentResponse.IncidentCreateResponseDTO>> createIncident(
            @RequestBody IncidentRequest incidentRequest) {

        Incident incident = incidentService.createIncident(incidentRequest);
        var responseDTO = IncidentConverter.toIncidentResponseDTO(incident);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.onSuccess(responseDTO));
    }
}
