package com.capston_design.fkiller.itoms.service_desk.client.grpc;

import com.capston_design.fkiller.itoms.ticket.TicketProto;
import com.capston_design.fkiller.itoms.ticket.TicketServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GrpcTicketClient {

    private final TicketServiceGrpc.TicketServiceBlockingStub ticketClientStub;

    public TicketProto.CreateTicketResponse createTicket(TicketProto.CreateTicketRequest createTicketRequest) {
        return ticketClientStub.createTicket(createTicketRequest);
    }
}
