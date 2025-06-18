package com.capston_design.fkiller.itoms.service_desk.service;

import com.capston_design.fkiller.itoms.service_desk.dto.IncidentRequest;
import com.capston_design.fkiller.itoms.service_desk.model.Incident;

import java.util.UUID;

public interface IncidentService {
    Incident createIncident(IncidentRequest incidentRequest);
    Incident completeTicket(UUID ticketId, UUID incidentId);
}
