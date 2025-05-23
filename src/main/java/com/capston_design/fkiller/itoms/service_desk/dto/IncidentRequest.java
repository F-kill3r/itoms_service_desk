package com.capston_design.fkiller.itoms.service_desk.dto;

import java.time.LocalDateTime;

public record IncidentRequest(
        String title,
        String content,
        String priority
) {}