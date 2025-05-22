package com.capston_design.fkiller.itoms.service_desk.service;

import com.capston_design.fkiller.itoms.service_desk.dto.IncidentRequest;
import com.capston_design.fkiller.itoms.service_desk.model.Incident;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Status;
import com.capston_design.fkiller.itoms.service_desk.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public Incident createIncident(IncidentRequest incidentRequest) {

        Incident incident = new Incident();

        incident.setTitle(incidentRequest.title());
        incident.setContent(incidentRequest.content());

        incident.setRequestDT(LocalDateTime.now()); // 요청 시간
        incident.setStatus(Status.Incomplete);      // 초기 상태

        //String createrById = request.getHeader("X-User-Id");
        //String creater = request.getHeader("X-User-Name");

        return incidentRepository.save(incident);
    }
}
