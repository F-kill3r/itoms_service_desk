package com.capston_design.fkiller.itoms.service_desk.dto;

import com.capston_design.fkiller.itoms.service_desk.model.enums.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class IncidentResponse {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentCreateResponseDTO{
        private Long id;
        private String title;
        private String content;
        private LocalDateTime requestDT;
        private LocalDateTime acceptDT;
        private LocalDateTime endDT;
        private Status status;
        private Long createrById;
        private Long chargerById;
        private String creater;
        private String charger;
    }
}