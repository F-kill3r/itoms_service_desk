package com.capston_design.fkiller.itoms.service_desk.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CreateTicketRequestDTO {
    private UUID incidentId;
    private RequesterDTO requester;
}

