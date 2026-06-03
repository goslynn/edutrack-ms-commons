/**
 * <h2>edutrack-ms-commons — código transversal de los microservicios</h2>
 *
 * <p>Artefacto Maven {@code cl.duocuc.edutrack:edutrack-ms-commons}: librería
 * Quarkus consumida como dependencia por cada microservicio de EduTrack
 * (Auth, Course, Student, Content, Assessment, Attendance, Annotation,
 * Notification, Report). Provee los componentes técnicos que no pertenecen a
 * ningún dominio en particular y que, sin esta dependencia, cada servicio
 * tendría que reimplementar de forma idéntica.</p>
 *
 * <h3>Qué encontrarás aquí</h3>
 * <ul>
 *   <li>{@link cl.duocuc.edutrack.ms.infrastructure.security} — autorización
 *       Unix-style: anotación {@link cl.duocuc.edutrack.ms.infrastructure.security.RequirePermission @RequirePermission},
 *       enum {@link cl.duocuc.edutrack.ms.infrastructure.security.Permission}
 *       con los bits {@code r=4, w=2, x=1}, recurso comodín
 *       {@link cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds#ALL},
 *       contrato CDI {@link cl.duocuc.edutrack.ms.infrastructure.security.PermissionEvaluator}
 *       que cada MS implementa y filtro JAX-RS
 *       {@link cl.duocuc.edutrack.ms.infrastructure.security.RequirePermissionFilter}
 *       que aplica la anotación.</li>
 *   <li>{@link cl.duocuc.edutrack.ms.infrastructure.context} — único intérprete
 *       de las cabeceras internas {@code X-User-Id} / {@code X-User-Roles} que
 *       el API Gateway propaga tras validar el JWT: enum
 *       {@link cl.duocuc.edutrack.ms.infrastructure.context.InternalHeader}
 *       como única fuente de los nombres en el wire, record inmutable
 *       {@link cl.duocuc.edutrack.ms.infrastructure.context.RequestHeaders} con
 *       los valores tipados, y bean
 *       {@link cl.duocuc.edutrack.ms.infrastructure.context.RequestContext}
 *       que parsea una sola vez por request.</li>
 *   <li>{@link cl.duocuc.edutrack.ms.infrastructure.exception} — handler global
 *       {@link cl.duocuc.edutrack.ms.infrastructure.exception.GlobalExceptionMappers}
 *       que convierte cualquier excepción que escape de un recurso en un
 *       envelope JSON único
 *       {@link cl.duocuc.edutrack.ms.infrastructure.exception.ErrorResponse};
 *       jerarquía de excepciones de dominio
 *       {@link cl.duocuc.edutrack.ms.infrastructure.exception.DomainException}
 *       con sugar
 *       {@link cl.duocuc.edutrack.ms.infrastructure.exception.ConflictException 409},
 *       {@link cl.duocuc.edutrack.ms.infrastructure.exception.NotFoundException 404}
 *       y {@link cl.duocuc.edutrack.ms.infrastructure.exception.ForbiddenException 403}.</li>
 *   <li>{@link cl.duocuc.edutrack.ms.infrastructure.jackson} — jerarquía
 *       estándar de vistas {@code @JsonView}
 *       ({@link cl.duocuc.edutrack.ms.infrastructure.jackson.Views Views.Base/Extra/...})
 *       y {@link cl.duocuc.edutrack.ms.infrastructure.jackson.JacksonCustomConfig}
 *       que fija {@code Views.Base} como vista por defecto del
 *       {@code ObjectMapper}.</li>
 *   <li>{@link cl.duocuc.edutrack.ms.infrastructure.validation} — grupos de
 *       Bean Validation transversales:
 *       {@link cl.duocuc.edutrack.ms.infrastructure.validation.Validations#OnCreate}
 *       y la secuencia
 *       {@link cl.duocuc.edutrack.ms.infrastructure.validation.Validations#Create}.</li>
 *   <li>{@link cl.duocuc.edutrack.ms.infrastructure.persistence} — superclases
 *       {@code @MappedSuperclass} con la auditoría común a las entidades:
 *       {@link cl.duocuc.edutrack.ms.infrastructure.persistence.CreatableEntity}
 *       ({@code id} UUID + {@code createdAt}) y
 *       {@link cl.duocuc.edutrack.ms.infrastructure.persistence.UpdatableEntity}
 *       (agrega {@code updatedAt}).</li>
 * </ul>
 *
 * <h3>Contrato del paquete</h3>
 * <p>Estas reglas son las que mantienen al artefacto agnóstico del dominio de
 * cualquier MS consumidor:</p>
 * <ul>
 *   <li><b>No importar paquetes {@code cl.duocuc.edutrack.ms.<servicio>.*}.</b>
 *       Ninguna clase aquí depende del código de un microservicio concreto.
 *       Cuando se necesita un punto de extensión específico (la evaluación de
 *       permisos, por ejemplo), se modela como contrato CDI y cada MS aporta su
 *       implementación.</li>
 *   <li><b>No referenciar nombres específicos.</b> Identificadores propios de
 *       un MS (claves de recurso, vistas de dominio, grupos de validación
 *       específicos) viven en el paquete del MS consumidor. Aquí solo lo común:
 *       el wildcard {@link cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds#ALL},
 *       la jerarquía
 *       {@link cl.duocuc.edutrack.ms.infrastructure.jackson.Views Views.Base/Extra/Detailed/...},
 *       el grupo
 *       {@link cl.duocuc.edutrack.ms.infrastructure.validation.Validations#OnCreate}
 *       y su secuencia
 *       {@link cl.duocuc.edutrack.ms.infrastructure.validation.Validations#Create}.</li>
 *   <li><b>Subpaquetes por responsabilidad técnica</b> ({@code security},
 *       {@code context}, {@code exception}, {@code jackson}, {@code validation},
 *       {@code persistence}), <b>no por dominio</b>.</li>
 * </ul>
 *
 * <h3>Propiedades de configuración expuestas</h3>
 * <p>La librería lee únicamente dos propiedades (ambas opcionales, con default
 * seguro para producción). Ver {@code application.properties.example} en la
 * raíz del artefacto para la descripción completa.</p>
 * <ul>
 *   <li>{@code edutrack.headers.validation.mode} — {@code EAGER} (default) o
 *       {@code WARN}. Política frente a una cabecera interna malformada.</li>
 *   <li>{@code edutrack.errors.expose-stacktrace} — {@code false} (default) o
 *       {@code true}. Incluye o no el stack trace en el envelope de error.</li>
 * </ul>
 */
package cl.duocuc.edutrack.ms.infrastructure;
