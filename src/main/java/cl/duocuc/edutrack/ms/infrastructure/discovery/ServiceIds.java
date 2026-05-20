package cl.duocuc.edutrack.ms.infrastructure.discovery;

/**
 * Catálogo de identificadores estables de microservicios de EduTrack tal como
 * se registran en Consul y se resuelven vía Stork. Es la <b>única fuente de
 * verdad</b> de los nombres lógicos usados para descubrimiento entre
 * servicios: cualquier REST Client o cliente HTTP de la plataforma debe
 * referenciar uno de estos valores en su URL Stork
 * ({@code stork://<service-id>}) o en la configuración de Stork
 * ({@code quarkus.stork.<service-id>.*}).
 *
 * <h3>Convención de naming</h3>
 * <p>Todos los IDs tienen el prefijo {@code edutrack-} para evitar colisión
 * con otros servicios que puedan registrarse en la misma instancia de Consul,
 * seguido por el sub-dominio funcional en kebab-case singular
 * ({@code edutrack-auth}, no {@code edutrack-authentication-service}).</p>
 *
 * <h3>Registro en Consul</h3>
 * <p>Cada microservicio se registra en Consul con su {@code service-id} como
 * {@code service.name}. El registro lo hace el operador de despliegue (Helm
 * chart / sidecar de Consul / Service Catalog de Kubernetes), no el código
 * del microservicio: este archivo solo provee el contrato simbólico para los
 * clientes.</p>
 */
public interface ServiceIds {

    /** Auth Service — JWT, roles, permisos Unix-style, endpoint {@code /auth/access}. */
    String AUTH = "edutrack-auth";

    /** Course Service — CRUD de cursos y permisos granulares docente-curso. */
    String COURSE = "edutrack-course";

    /** Student Service — alumnos y apoderados, eventos de ciclo de vida. */
    String STUDENT = "edutrack-student";

    /** Content Service — árbol jerárquico de contenido y archivos en S3. */
    String CONTENT = "edutrack-content";

    /** Assessment Service — notas, evaluaciones y promedios ponderados. */
    String ASSESSMENT = "edutrack-assessment";

    /** Attendance Service — registro de asistencia agnóstico al mecanismo de captura. */
    String ATTENDANCE = "edutrack-attendance";

    /** Annotation Service — anotaciones positivas/negativas con notificación async. */
    String ANNOTATION = "edutrack-annotation";

    /** Notification Service — emisor genérico (Strategy); v1 EMAIL_HTML. */
    String NOTIFICATION = "edutrack-notification";

    /** Report Service — definición y generación de reportes JSON/CSV/PDF. */
    String REPORT = "edutrack-report";

}
