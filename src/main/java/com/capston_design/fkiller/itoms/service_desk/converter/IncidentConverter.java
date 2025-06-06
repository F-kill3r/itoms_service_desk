package com.capston_design.fkiller.itoms.service_desk.converter;

import com.capston_design.fkiller.itoms.service_desk.dto.IncidentResponse;
import com.capston_design.fkiller.itoms.service_desk.model.Incident;

public class IncidentConverter {

    public static IncidentResponse.IncidentCreateResponseDTO toIncidentResponseDTO(Incident incident) {
        return IncidentResponse.IncidentCreateResponseDTO.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .content(incident.getContent())
                .requestDT(incident.getRequestDT())
                .acceptDT(incident.getAcceptDT())
                .endDT(incident.getEndDT())
                .status(incident.getStatus())
                .priority(incident.getPriority())
                .ticketByID(incident.getTicketByID())
                .requesterById(incident.getRequesterById())
                .creatorById(incident.getCreatorById())
                .chargerById(incident.getChargerById())
                .requester(incident.getRequester())
                .creator(incident.getCreator())
                .charger(incident.getCharger())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }
}
