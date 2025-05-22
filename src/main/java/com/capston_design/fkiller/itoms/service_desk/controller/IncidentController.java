package com.capston_design.fkiller.itoms.service_desk.controller;

import com.capston_design.fkiller.itoms.service_desk.dto.IncidentRequest;
import com.capston_design.fkiller.itoms.service_desk.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incident")
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String createIncident(@RequestBody IncidentRequest incidentRequest) {
        incidentService.createIncident(incidentRequest);
        return "Incident created";
    }
}
