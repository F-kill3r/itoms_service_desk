package com.capston_design.fkiller.itoms.service_desk.dto;

import java.util.UUID;

public record TicketCompletedRequestDTO(
        UUID ticketId,
        UUID incidentId
) {}