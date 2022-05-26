package org.pipservices3.rpc.services;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

import java.time.ZonedDateTime;
import java.util.function.Function;

public class HeartbeatOperations extends RestOperations {

    public Function<ContainerRequestContext, Response> getHeartbeatOperation() {
        return this::heartbeat;
    }

    public Response heartbeat(ContainerRequestContext req) {
        return this.sendResult(ZonedDateTime.now());
    }
}
