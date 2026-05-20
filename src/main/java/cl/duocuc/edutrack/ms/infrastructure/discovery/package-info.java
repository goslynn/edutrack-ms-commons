/**
 * Estándar de <b>comunicación HTTP inter-servicio</b> de EduTrack: descubrimiento
 * de servicios vía Consul resuelto en runtime por
 * <a href="https://smallrye.io/smallrye-stork/">SmallRye Stork</a>. Encaja con
 * la decisión arquitectónica del proyecto (informe §2.2: Service Discovery con
 * Consul como registry complementario al Service Discovery nativo de
 * Kubernetes).
 *
 * <h2>Contrato</h2>
 * <ol>
 *   <li>Todo cliente REST que llame a otro microservicio de EduTrack se
 *       construye con MicroProfile REST Client y declara su URL base
 *       directamente en la anotación con
 *       {@code @RegisterRestClient(baseUri = "stork://" + ServiceIds.X,
 *       configKey = "...")}. Como las constantes de
 *       {@link cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceIds} son
 *       {@code public static final String}, la concatenación es una constante
 *       de compilación válida en anotaciones. <b>Prohibido</b> hardcodear
 *       hosts, IPs o nombres DNS de otros MS en el código o en
 *       {@code application.properties}.</li>
 *   <li>El nombre simbólico se resuelve en runtime contra Consul vía Stork:
 *       Stork lee instancias registradas, aplica el load balancer configurado
 *       (round-robin por defecto) y devuelve un host:puerto concreto para
 *       cada llamada.</li>
 *   <li>El registro en Consul es responsabilidad del operador de despliegue
 *       (Helm chart, sidecar de Consul, Service Catalog de Kubernetes), no del
 *       código de aplicación. Esta librería solo provee el contrato simbólico
 *       y las dependencias para los <i>consumidores</i>.</li>
 * </ol>
 *
 * <h2>Configuración del consumidor</h2>
 * <p>El JAR de la librería trae bundleado en
 * {@code META-INF/microprofile-config.properties} el catálogo completo de
 * service discovery (un bloque {@code quarkus.stork.<service-id>.*} por cada
 * entrada de {@link cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceIds}),
 * apuntando a dos llaves globales:</p>
 * <pre>{@code
 * edutrack.consul.host=consul.internal
 * edutrack.consul.port=8500
 * }</pre>
 * <p>Un MS consumidor no necesita declarar nada por defecto. Sobreescribe
 * {@code edutrack.consul.host}/{@code edutrack.consul.port} en su propio
 * {@code application.properties} solo si su ambiente apunta a otro Consul, y
 * el catálogo entero se mueve con esas dos líneas. No hay duplicación por
 * servicio destino.</p>
 *
 * <h2>Ejemplo dentro de esta misma librería</h2>
 * <p>{@link cl.duocuc.edutrack.ms.infrastructure.context.AuthAccessClient}
 * sigue el contrato: declara
 * {@code @RegisterRestClient(baseUri = "stork://" + ServiceIds.AUTH, ...)} y
 * Stork resuelve la instancia de Auth en cada llamada.</p>
 *
 * <h2>Resiliencia</h2>
 * <p>El estándar de descubrimiento es ortogonal a los patrones de resiliencia:
 * Stork resuelve <i>dónde</i> está el servicio; el caller decide <i>cómo</i>
 * tolera fallos (timeouts, retries, circuit breakers via SmallRye Fault
 * Tolerance). Cada consumidor anota su REST Client con los decoradores que
 * correspondan a su criticidad — no se impone una política global desde acá.</p>
 */
package cl.duocuc.edutrack.ms.infrastructure.discovery;
