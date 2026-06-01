package cl.duocuc.edutrack.ms.infrastructure.discovery;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Puente entre el <b>patrón de discovery</b> de EduTrack y el <b>REST Client
 * declarativo</b> de Quarkus. Materializa, en tiempo de arranque, una propiedad
 * {@code quarkus.rest-client.<id>.url} por cada {@link ServiceIds} expandiendo el
 * placeholder {@code {service}} del patrón {@code edutrack.discovery.pattern}.
 *
 * <h2>Por qué existe</h2>
 * <p>Los clients de la plataforma son declarativos
 * ({@code @RegisterRestClient(configKey = ServiceIds.X)}), lo que nos da
 * integración build-time, native-image, Fault Tolerance y OpenTelemetry "gratis".
 * Pero un client declarativo necesita su URL en
 * {@code quarkus.rest-client.<configKey>.url}. En vez de obligar a cada MS a
 * escribir esa línea por cada uno de los ~9 servicios (y mantenerlas en sync),
 * este factory las <b>deriva</b> de una única fuente de verdad: el patrón. Así
 * conservamos el DRY del esquema anterior (un solo template) y ganamos el
 * paradigma declarativo.</p>
 *
 * <h2>Cómo resuelve el patrón</h2>
 * <p>Es un {@link ConfigSourceFactory} (no un {@link ConfigSource} plano) justamente
 * porque necesita <b>leer</b> config ya cargada: el {@link ConfigSourceContext}
 * le da acceso a {@code edutrack.discovery.scheme} y
 * {@code edutrack.discovery.pattern} —vengan del
 * {@code META-INF/microprofile-config.properties} de la librería o del
 * {@code application.properties} del MS—. La resolución es <b>profile-aware</b>:
 * {@link #resolve} consulta primero las variantes {@code %<profile>.<clave>}
 * (p. ej. {@code %dev.edutrack.discovery.pattern}) y recién después la clave
 * base, de modo que el override local de docker-compose funcione.</p>
 *
 * <h2>Precedencia</h2>
 * <p>El {@link ConfigSource} generado se publica con ordinal {@value #GENERATED_ORDINAL},
 * <b>por debajo</b> del {@code application.properties} de un MS (250). Por tanto un
 * MS puede pisar la URL de un servicio puntual declarando explícitamente
 * {@code quarkus.rest-client.<id>.url=...} (caso raro: un servicio en un host
 * especial); lo derivado solo cubre lo que el MS no fijó a mano.</p>
 *
 * <h2>Registro</h2>
 * <p>Se descubre vía {@code ServiceLoader} por el archivo
 * {@code META-INF/services/io.smallrye.config.ConfigSourceFactory} que viaja en
 * este mismo JAR. Como SmallRye Config (el motor de config de Quarkus) escanea
 * el classpath completo, <b>cualquier MS que dependa de esta librería lo obtiene
 * automáticamente</b>: el MS no declara ni registra nada.</p>
 */
public class DiscoveryConfigSourceFactory implements ConfigSourceFactory {

    /** Clave del esquema HTTP del patrón (http/https). */
    static final String SCHEME_KEY = "edutrack.discovery.scheme";
    /** Clave del template de host con placeholder {@code {service}}. */
    static final String PATTERN_KEY = "edutrack.discovery.pattern";

    static final String DEFAULT_SCHEME = "http";
    static final String DEFAULT_PATTERN = "edutrack-{service}.fly.internal:8080";

    /** Nombre del ConfigSource generado (aparece en logs de config de Quarkus). */
    private static final String SOURCE_NAME = "edutrack-discovery-rest-clients";

    /** Ordinal del source generado: bajo application.properties (250), sobre defaults de librería. */
    private static final int GENERATED_ORDINAL = 150;

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
        String scheme  = resolve(context, SCHEME_KEY, DEFAULT_SCHEME);
        String pattern = resolve(context, PATTERN_KEY, DEFAULT_PATTERN);

        Map<String, String> generated = new HashMap<>();
        for (String serviceId : ServiceIds.ALL) {
            String url = scheme + "://" + pattern.replace("{service}", serviceId);
            generated.put("quarkus.rest-client." + serviceId + ".url", url);
        }
        return List.of(new PropertiesConfigSource(generated, SOURCE_NAME, GENERATED_ORDINAL));
    }

    /**
     * El factory solo depende de ConfigSources estáticos (siempre disponibles en
     * el contexto), así que su prioridad relativa a otros factories es indiferente.
     * Se fija explícita para que el orden sea reproducible.
     */
    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(GENERATED_ORDINAL);
    }

    /**
     * Lee {@code key} respetando el perfil activo: prueba primero cada
     * {@code %<profile>.<key>} en orden de prioridad y cae a la clave base; si
     * nada está definido devuelve {@code def}. Permite que
     * {@code %dev.edutrack.discovery.pattern={service}:8080} (docker-compose)
     * gane sobre el default productivo sin código condicional.
     */
    private static String resolve(ConfigSourceContext context, String key, String def) {
        for (String profile : context.getProfiles()) {
            String found = valueOrNull(context, "%" + profile + "." + key);
            if (found != null) {
                return found;
            }
        }
        String base = valueOrNull(context, key);
        return base != null ? base : def;
    }

    private static String valueOrNull(ConfigSourceContext context, String key) {
        ConfigValue value = context.getValue(key);
        return (value != null && value.getValue() != null) ? value.getValue() : null;
    }
}
