package com.capston_design.fkiller.itoms.service_desk.infra.grpc.config;

import com.capston_design.fkiller.itoms.ticket.TicketServiceGrpc;
import com.capston_design.fkiller.itoms.user.UserServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @Value("${USER_HOST}")
    private String userServiceHost;

    @Value("${USER_PORT}")
    private int userServicePort;

    @Value("${TICKET_CORE_HOST}")
    private String ticketServiceHost;

    @Value("${TICKET_CORE_PORT}")
    private int ticketServicePort;

    @Bean
    public ManagedChannel userServiceChannel() {
        return ManagedChannelBuilder.forAddress(userServiceHost, userServicePort)
                .usePlaintext()
                .build();
    }

    @Bean
    public UserServiceGrpc.UserServiceBlockingStub userClientStub(ManagedChannel userServiceChannel) {
        return UserServiceGrpc.newBlockingStub(userServiceChannel);
    }

    @Bean
    public ManagedChannel ticketServiceChannel() {
        return ManagedChannelBuilder.forAddress(ticketServiceHost, ticketServicePort)
                .usePlaintext()
                .build();
    }

    @Bean
    public TicketServiceGrpc.TicketServiceBlockingStub ticketClientStub(ManagedChannel ticketServiceChannel) {
        return TicketServiceGrpc.newBlockingStub(ticketServiceChannel);
    }

}
