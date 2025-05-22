package com.capston_design.fkiller.itoms.service_desk.dto;

import com.capston_design.fkiller.itoms.service_desk.model.enums.Status;

import java.time.LocalDateTime;

public record IncidentResponse(
        Long id,
        String title,
        String content,
        LocalDateTime requestDT,
        LocalDateTime acceptDT,
        LocalDateTime endDT,
        Status status,
        String createrById,
        String chargerById,
        String creater,
        String charger,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}