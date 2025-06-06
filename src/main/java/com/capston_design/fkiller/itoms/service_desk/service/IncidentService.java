package com.capston_design.fkiller.itoms.service_desk.service;

import com.capston_design.fkiller.itoms.service_desk.apiPayload.ApiResponse;
import com.capston_design.fkiller.itoms.service_desk.client.TicketClient;
import com.capston_design.fkiller.itoms.service_desk.client.UserClient;
import com.capston_design.fkiller.itoms.service_desk.dto.*;
import com.capston_design.fkiller.itoms.service_desk.model.Incident;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Priority;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Status;
import com.capston_design.fkiller.itoms.service_desk.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final UserClient userClient;
    private final TicketClient ticketClient;

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

        ApiResponse<UserCreateResponseDTO> userResponse = userClient.getRandomRequesterUser();
        if (userResponse == null || !Boolean.TRUE.equals(userResponse.getIsSuccess())
                || userResponse.getResult() == null) {
            throw new IllegalStateException("UserService로부터 랜덤 유저를 불러오지 못했습니다.");
        }
        UserCreateResponseDTO user = userResponse.getResult();

        incident.setRequester(user.getName());
        incident.setRequesterById(user.getId());

        incidentRepository.save(incident);

        //Ticket 생성 요청
        CreateTicketRequestDTO ticketRequest = new CreateTicketRequestDTO(
                incident.getId(),
                new RequesterDTO(user.getId().toString(), user.getName())
        );
        CreateTicketResponseDTO ticketResponse = ticketClient.createTicket(ticketRequest);

        //ticketId 저장 후 재저장
        incident.setTicketByID(ticketResponse.getTicketId());
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
