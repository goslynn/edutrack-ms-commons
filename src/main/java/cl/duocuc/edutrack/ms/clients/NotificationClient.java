package cl.duocuc.edutrack.ms.clients;

import cl.duocuc.edutrack.ms.infrastructure.discovery.IdentityHeadersFactory;
import cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceIds;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * SDK de cliente del <b>Notification Service</b>. Es el <b>unico</b> mecanismo por
 * el que otro microservicio pide una notificacion: encapsula el contrato HTTP de
 * ingesta ({@code POST /notification} ⇒ {@code 202 Accepted} fire-and-forget) para
 * que los callers no armen URLs ni conozcan la forma interna del servicio.
 *
 * <p>Declarativo, igual que {@link AttendanceClient}/{@link AuthClient}: la URL la
 * deriva {@code DiscoveryConfigSourceFactory} desde el patron de discovery sobre
 * {@link ServiceIds#NOTIFICATION} (no se escribe a mano), y
 * {@link IdentityHeadersFactory} reenvia la identidad del usuario original
 * ({@code X-User-Id}/{@code X-User-Roles}) desde el {@code RequestContext} — de modo
 * que Notification aplique su authz ({@code EXECUTE} sobre
 * {@code notification.notifications}) sobre quien disparo la accion.</p>
 *
 * <p><b>Tolerant writer:</b> el cuerpo viaja como {@code Object} (un mapa/record que
 * calce con {@code NotificationRequest}: {@code notificationType}, {@code recipientId},
 * {@code templateId}, {@code payload}) para no acoplar al caller a los DTOs de
 * Notification.</p>
 *
 * <p><b>Resiliencia (spec {@code async-events}):</b> este client es sincrono; el
 * caller es responsable de invocarlo de forma resiliente (no dejar que un fallo de
 * transporte tumbe su operacion de negocio). Cuando entre RabbitMQ, un publisher de
 * broker reemplaza al que usa este client sin tocar la capa de servicio.</p>
 */
@RegisterRestClient(configKey = ServiceIds.NOTIFICATION)
@RegisterClientHeaders(IdentityHeadersFactory.class)
public interface NotificationClient {

    /**
     * Encola una notificacion. Exito ⇒ {@code 202 Accepted} con el job en estado
     * {@code PENDING}; tipo desconocido ⇒ {@code 422}.
     *
     * @param request cuerpo compatible con {@code NotificationRequest}
     *                ({@code notificationType}, {@code recipientId}, {@code templateId},
     *                {@code payload}).
     */
    @POST
    @Path("/" + ServiceIds.NOTIFICATION)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response enqueue(Object request);
}
