package cl.duocuc.edutrack.ms.clients;

import cl.duocuc.edutrack.ms.infrastructure.discovery.IdentityHeadersFactory;
import cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceIds;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

/**
 * SDK de cliente del <b>Student Service</b>. Declarativo, igual que
 * {@link AttendanceClient}/{@link AuthClient}/{@link NotificationClient}: la URL la
 * deriva {@code DiscoveryConfigSourceFactory} desde el patron de discovery sobre
 * {@link ServiceIds#STUDENT}, y {@link IdentityHeadersFactory} reenvia la identidad del
 * usuario original ({@code X-User-Id}/{@code X-User-Roles}) para que Student aplique su
 * authz sobre quien dispara la consulta.
 *
 * <p>Estilo de client: interfaz tipada que retorna {@code Response} crudo; el consumidor
 * lo lee con su propio DTO tolerant-reader (p. ej. solo {@code id} + {@code email} de un
 * apoderado). Datos sensibles: {@code student.guardians} exige {@code READ}.</p>
 */
@RegisterRestClient(configKey = ServiceIds.STUDENT)
@RegisterClientHeaders(IdentityHeadersFactory.class)
public interface StudentClient {

    /**
     * Lista los apoderados de un alumno ({@code GET /student/students/{studentId}/guardians}).
     * Cada elemento trae, entre otros, {@code id}, {@code email} y {@code relationType}.
     *
     * @param studentId UUID del alumno.
     */
    @GET
    @Path("/" + ServiceIds.STUDENT + "/students/{studentId}/guardians")
    @Produces(MediaType.APPLICATION_JSON)
    Response listGuardians(@PathParam("studentId") UUID studentId);
}
