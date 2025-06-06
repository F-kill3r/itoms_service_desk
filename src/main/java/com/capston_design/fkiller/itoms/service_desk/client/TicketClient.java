package com.capston_design.fkiller.itoms.service_desk.client;

import com.capston_design.fkiller.itoms.service_desk.dto.CreateTicketRequestDTO;
import com.capston_design.fkiller.itoms.service_desk.dto.CreateTicketResponseDTO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface TicketClient {

    @PostExchange("/api/ticket-core/v1/ticket")
    CreateTicketResponseDTO createTicket(@RequestBody CreateTicketRequestDTO request);
}
