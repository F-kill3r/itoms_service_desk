package com.capston_design.fkiller.itoms.service_desk.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CreateTicketResponseDTO {
    private UUID ticketId;
}
