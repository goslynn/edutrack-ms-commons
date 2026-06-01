package cl.duocuc.edutrack.ms.infrastructure.discovery;

/**
 * Catálogo de identificadores lógicos <b>bare</b> de los microservicios de
 * EduTrack. Es la <b>única fuente de verdad</b> de los nombres usados para
 * descubrimiento entre servicios: {@link ServiceRegistry} sustituye uno de
 * estos valores en el placeholder {@code {service}} del patrón
 * {@code edutrack.discovery.pattern} para resolver el host destino.
 *
 * <h3>Convención de naming</h3>
 * <p>El valor es el <b>nombre lógico sin prefijo</b> ({@code "auth"},
 * {@code "course"}, …), idéntico al <b>primer segmento del path</b> que el API
 * Gateway usa para enrutar y al token {@code {service}} de su
 * {@code UPSTREAM_HOST_PATTERN}. El prefijo {@code edutrack-} y el sufijo de
 * dominio ({@code .fly.internal:8080}) los aporta el patrón de discovery, no
 * la constante — así un mismo valor sirve en local (contenedor {@code auth}) y
 * en Fly.io (app {@code edutrack-auth}).</p>
 *
 * <h3>Resolución (Fly.io DNS, sin registry)</h3>
 * <p>Cada microservicio es una app en Fly.io
 * resolvible por DNS privado como {@code edutrack-<service>.fly.internal}.
 * {@link DiscoveryConfigSourceFactory} aplica el patrón sobre estos valores;
 * el balanceo entre instancias lo hace Fly por DNS. Ver el {@code package-info}
 * de este paquete para el contrato completo.</p>
 */
public interface ServiceIds {

    /** Auth Service — JWT, roles, permisos Unix-style, endpoint {@code /auth/access}. */
    String AUTH = "auth";

    /** Course Service — CRUD de cursos y permisos granulares docente-curso. */
    String COURSE = "course";

    /** Student Service — alumnos y apoderados, eventos de ciclo de vida. */
    String STUDENT = "student";

    /** Content Service — árbol jerárquico de contenido y archivos. */
    String CONTENT = "content";

    /** Assessment Service — notas, evaluaciones y promedios ponderados. */
    String ASSESSMENT = "assessment";

    /** Attendance Service — registro de asistencia agnóstico al mecanismo de captura. */
    String ATTENDANCE = "attendance";

    /** Annotation Service — anotaciones positivas/negativas con notificación async. */
    String ANNOTATION = "annotation";

    /** Notification Service — emisor genérico (Strategy); v1 EMAIL_HTML. */
    String NOTIFICATION = "notification";

    /** Report Service — definición y generación de reportes JSON/CSV/PDF. */
    String REPORT = "report";

    /**
     * Conjunto de <b>todos</b> los ids bare del catálogo. Es lo que recorre
     * {@link DiscoveryConfigSourceFactory} para materializar, por cada servicio,
     * la propiedad {@code quarkus.rest-client.<id>.url} a partir del patrón de
     * discovery — de modo que el patrón siga siendo la única fuente de verdad y
     * los clients puedan ser declarativos ({@code @RegisterRestClient}).
     *
     * <p>Al agregar un microservicio nuevo: suma su constante arriba <b>y</b>
     * su referencia aquí. Si falta aquí, su URL de rest-client no se genera y
     * el client declarativo fallará en runtime con "no URL configured".</p>
     */
    java.util.Set<String> ALL = java.util.Set.of(
        AUTH, COURSE, STUDENT, CONTENT, ASSESSMENT,
        ATTENDANCE, ANNOTATION, NOTIFICATION, REPORT);

}
