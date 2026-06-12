package cl.duocuc.edutrack.ms.infrastructure.discovery;

import cl.duocuc.edutrack.ms.infrastructure.exception.DomainException;
import cl.duocuc.edutrack.ms.infrastructure.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Filtro de respuesta <b>opt-in</b> que automatiza la propagación de errores
 * inter-servicio: en cualquier respuesta no-2xx <b>lanza</b> el
 * {@link DomainException} reconstruido desde el envelope {@link ErrorResponse}
 * del upstream, evitando tener que llamar a
 * {@link HTTPClientUtils#readOrThrow} en cada call site.
 *
 * <h2>Por qué un {@code ClientResponseFilter} y no un {@code ResponseExceptionMapper}</h2>
 * <p>El estilo de client de la plataforma es <i>interfaz tipada que retorna
 * {@link Response}</i>, y un método que retorna {@code Response} <b>no dispara</b>
 * los {@code ResponseExceptionMapper} de MP REST Client (el runtime asume que
 * quien pide el {@code Response} crudo maneja el status). Un
 * {@link ClientResponseFilter}, en cambio, corre <b>siempre</b>, antes de que el
 * {@code Response} llegue al call site, sin importar el tipo de retorno. Así el
 * call site queda en un {@code response.readEntity(MiDto.class)} sin chequear
 * status: cualquier no-2xx ya habría lanzado.</p>
 *
 * <h2>Opt-in deliberado</h2>
 * <p>No se registra global (vía {@code META-INF/services}) a propósito: lanzar en
 * todo no-2xx <b>rompe</b> los consumidores que necesitan inspeccionar el status
 * o los headers crudos (p. ej. tratar un {@code 404} como {@code Optional.empty()}).
 * Cada client que quiera propagación transparente lo engancha explícitamente con
 * {@code @RegisterProvider(ErrorPropagationClientFilter.class)}; los demás siguen
 * usando la {@link Response} directa y, si acaso, {@code readOrThrow}.</p>
 *
 * <p>La traducción {@code ErrorResponse → DomainException} se delega en
 * {@link HTTPClientUtils#fromErrorResponse}, el mismo punto de ensamble que usa
 * {@code readOrThrow} — la lógica no se duplica.</p>
 */
@Provider
@ApplicationScoped
public class ErrorPropagationClientFilter implements ClientResponseFilter {

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        if (responseContext.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            return;
        }
        throw toDomainException(responseContext);
    }

    private DomainException toDomainException(ClientResponseContext responseContext) {
        int status = responseContext.getStatus();
        if (responseContext.hasEntity()) {
            try {
                byte[] body = readAll(responseContext.getEntityStream());
                // Re-arma el stream para no consumir el cuerpo de cara a cualquier
                // lectura posterior (defensa: en no-2xx normalmente nadie más lo lee).
                responseContext.setEntityStream(new ByteArrayInputStream(body));
                ErrorResponse e = objectMapper.readValue(body, ErrorResponse.class);
                DomainException mapped = HTTPClientUtils.fromErrorResponse(status, e);
                if (mapped != null) {
                    return mapped;
                }
            } catch (IOException | RuntimeException ignored) {
                // El upstream no devolvió el envelope estándar — cae al genérico.
            }
        }
        return HTTPClientUtils.genericUpstreamError(status);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try (in) {
            return in.readAllBytes();
        }
    }
}
