package cl.duocuc.edutrack.ms.clients;

import cl.duocuc.edutrack.ms.infrastructure.discovery.IdentityHeadersFactory;
import cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceIds;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@RegisterRestClient(configKey = ServiceIds.ATTENDANCE)
@RegisterClientHeaders(IdentityHeadersFactory.class)
public interface AttendanceClient {

    @POST
    @Path("/"+ServiceIds.ATTENDANCE+"/sessions")
    @Produces(MediaType.APPLICATION_JSON)
    Response createSession(Object request);

    @PATCH
    @Path("/"+ServiceIds.ATTENDANCE+"/sessions/{id}/close")
    @Produces(MediaType.APPLICATION_JSON)
    Response closeSession(@PathParam("id") UUID sessionId);

    @POST
    @Path("/"+ServiceIds.ATTENDANCE+"/sessions/{id}/records")
    @Produces(MediaType.APPLICATION_JSON)
    Response registerRecord(@PathParam("id") UUID sessionId, Object request);


}


