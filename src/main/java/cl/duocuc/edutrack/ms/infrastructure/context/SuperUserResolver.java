package cl.duocuc.edutrack.ms.infrastructure.context;

import cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds;

/**
 * Contrato CDI que responde si la identidad propagada por el API Gateway en
 * el request actual es <b>superusuaria</b>. La definición operacional de
 * "super" en EduTrack es <i>tener los tres bits ({@code rwx}) sobre el recurso
 * comodín</i> {@link ResourceIds#ALL}: un usuario super puede leer, escribir y
 * ejecutar sobre cualquier recurso del sistema.
 *
 * <h3>Punto de extensión</h3>
 * <p>La librería provee una implementación por defecto
 * ({@link RemoteSuperUserResolver}) anotada con
 * {@link io.quarkus.arc.DefaultBean @DefaultBean} que consulta al Auth Service
 * vía HTTP. Un microservicio puede aportar su propia implementación
 * {@code @ApplicationScoped} sin {@code @DefaultBean} y ArC la usará en lugar
 * de la remota. El caso típico es el propio Auth Service, que evalúa los
 * permisos localmente contra su base de datos sin pasar por HTTP.</p>
 *
 * <h3>Contexto del request</h3>
 * <p>La pregunta es siempre <i>"el usuario del request actual"</i>: el método
 * no recibe identidad porque toma {@code X-User-Id} / {@code X-User-Roles} del
 * {@link RequestContext}. Llamarlo fuera de un request HTTP arroja
 * {@link jakarta.enterprise.context.ContextNotActiveException}.</p>
 *
 * <h3>Caché</h3>
 * <p>El resultado lo memoiza {@link RequestContext} a nivel de request, por lo
 * que las implementaciones no necesitan cachear internamente: cada request
 * paga a lo más una llamada al resolver.</p>
 */
public interface SuperUserResolver {

    /**
     * @return {@code true} si la identidad del request actual posee
     *         {@code rwx} (los tres bits) sobre {@link ResourceIds#ALL};
     *         {@code false} en cualquier otro caso, incluyendo identidad
     *         ausente y fallos de evaluación (fail-closed).
     */
    boolean isSuper();
}
