package com.capston_design.fkiller.itoms.service_desk.service;


import com.capston_design.fkiller.itoms.service_desk.client.grpc.GrpcTicketClient;
import com.capston_design.fkiller.itoms.service_desk.client.grpc.GrpcUserClient;
import com.capston_design.fkiller.itoms.service_desk.dto.*;
import com.capston_design.fkiller.itoms.service_desk.model.Incident;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Priority;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Status;
import com.capston_design.fkiller.itoms.service_desk.repository.IncidentRepository;
import com.capston_design.fkiller.itoms.ticket.TicketProto;
import com.capston_design.fkiller.itoms.user.UserProto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Qualifier("grpc")
public class IncidentGrpcServiceImpl implements IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentGrpcServiceImpl.class);

    private final IncidentRepository incidentRepository;
    private final GrpcUserClient grpcUserClient;
    private final GrpcTicketClient grpcTicketClient;

    @Transactional
    public Incident createIncident(IncidentRequest incidentRequest) {

        Incident incident = new Incident();

        incident.setTitle(incidentRequest.title());
        incident.setContent(incidentRequest.content());

        incident.setRequestDT(LocalDateTime.now()); // 요청 시간
        incident.setStatus(Status.Incomplete);      // 초기 상태
        incident.setPriority(Priority.from(incidentRequest.priority()));
        //String createrById = request.getHeader("X-User-Id");
        //String creater = request.getHeader("X-User-Name");

        UserProto.UserCreateResponse user = grpcUserClient.getRandomRequesterUser();

        incident.setRequester(user.getName());
        incident.setRequesterById(UUID.fromString(user.getId()));

        incidentRepository.save(incident);

        //Ticket 생성 요청
        TicketProto.CreateTicketRequest ticketRequest =
                TicketProto.CreateTicketRequest.newBuilder()
                        .setIncidentId(incident.getId().toString())
                        .setRequesterId(user.getId())
                        .setRequesterName(user.getName())
                        .build();

        TicketProto.CreateTicketResponse ticketResponse = grpcTicketClient.createTicket(ticketRequest);

        //ticketId 저장 후 재저장
        incident.setTicketByID(UUID.fromString(ticketResponse.getTicketId()));
        return incidentRepository.save(incident);
    }


    @Transactional
    public Incident completeTicket(UUID ticketId, UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with id: " + incidentId));

        incident.setStatus(Status.Completed);
        incident.setEndDT(LocalDateTime.now());

        Incident savedIncident = incidentRepository.save(incident);
        log.info("Incident completed successfully - Incident ID: {}, Title: {}, Completed by: {}, Completed at: {}",
                savedIncident.getId(),
                savedIncident.getTitle(),
                savedIncident.getCharger(),
                savedIncident.getEndDT());

        return savedIncident;
    }
}
