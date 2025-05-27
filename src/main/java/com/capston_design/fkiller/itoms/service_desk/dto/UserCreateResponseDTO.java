package com.capston_design.fkiller.itoms.service_desk.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class UserCreateResponseDTO {
    private UUID id;
    private String name;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}