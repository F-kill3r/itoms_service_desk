package com.capston_design.fkiller.itoms.service_desk.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RandomUserResponseDTO {
    private UUID incidentId;
    private UUID userId;
    private String userName;
} 