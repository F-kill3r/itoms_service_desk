package com.capston_design.fkiller.itoms.service_desk.client.grpc;

import com.capston_design.fkiller.itoms.user.UserProto;
import com.capston_design.fkiller.itoms.user.UserServiceGrpc;
import com.google.protobuf.Empty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GrpcUserClient {

    private final UserServiceGrpc.UserServiceBlockingStub userClientStub;

    public UserProto.UserCreateResponse getRandomRequesterUser() {
        return userClientStub.getRandomRequesterUser(Empty.newBuilder().build());
    }
}
