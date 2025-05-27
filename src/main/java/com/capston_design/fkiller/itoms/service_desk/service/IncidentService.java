package com.capston_design.fkiller.itoms.service_desk.service;

import com.capston_design.fkiller.itoms.service_desk.apiPayload.ApiResponse;
import com.capston_design.fkiller.itoms.service_desk.client.UserClient;
import com.capston_design.fkiller.itoms.service_desk.dto.IncidentRequest;
import com.capston_design.fkiller.itoms.service_desk.dto.UserCreateResponseDTO;
import com.capston_design.fkiller.itoms.service_desk.model.Incident;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Priority;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Status;
import com.capston_design.fkiller.itoms.service_desk.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserClient userClient;

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

        ApiResponse<UserCreateResponseDTO> userResponse = userClient.getRandomOutsourcedUser();
        if (userResponse == null || !Boolean.TRUE.equals(userResponse.getIsSuccess())
                || userResponse.getResult() == null) {
            throw new IllegalStateException("UserService로부터 랜덤 유저를 불러오지 못했습니다.");
        }
        UserCreateResponseDTO user = userResponse.getResult();

        incident.setCharger(user.getName());
        incident.setChargerById(user.getId());

        return incidentRepository.save(incident);
    }
}
