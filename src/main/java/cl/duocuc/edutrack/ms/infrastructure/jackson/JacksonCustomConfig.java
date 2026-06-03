package cl.duocuc.edutrack.ms.infrastructure.jackson;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

/**
 * Personalización del {@code ObjectMapper} de Quarkus para integrar la
 * jerarquía {@link Views} de la librería. Se registra como
 * {@link ObjectMapperCustomizer} (extensión {@code quarkus-jackson}): Quarkus
 * la detecta automáticamente porque es un bean {@code @Singleton}.
 *
 * <h3>Qué hace</h3>
 * <ul>
 *   <li>Fija {@link Views.Base} como vista por defecto, tanto en
 *       {@code SerializationConfig} como en {@code DeserializationConfig}: si
 *       un endpoint no declara {@code @JsonView(...)}, Jackson aplica
 *       {@code Views.Base}.</li>
 *   <li>Mantiene {@link MapperFeature#DEFAULT_VIEW_INCLUSION} en su valor por
 *       defecto (<b>habilitado</b>): una propiedad <b>sin</b> {@code @JsonView}
 *       se incluye en la vista activa en vez de quedar excluida — se comporta
 *       como si perteneciera a la vista por defecto ({@code Base}). Esto permite
 *       que los DTO <i>tolerant-reader</i> de los clients inter-servicio (campos
 *       sin anotar) deserialicen sin tener que anotar cada componente.</li>
 * </ul>
 *
 * <p><b>Matiz:</b> "habilitado" incluye la propiedad sin anotar en <i>cualquier</i>
 * vista activa, no solo en {@code Base}. La diferencia solo aplica a las vistas
 * que no extienden {@code Base} ({@code Extra}, {@code Internal}); como los DTO
 * propios anotan todos sus campos, en la práctica solo afecta a lectores
 * tolerantes, donde la inclusión permisiva es justamente lo deseado.</p>
 *
 * <h3>Override por endpoint</h3>
 * <p>La vista configurada aquí es <i>default</i>: cualquier
 * {@code @JsonView(...)} declarado en un método de recurso o en un parámetro
 * de body gana sobre ella. Quarkus REST aplica
 * {@code writer/reader.withView(...)} por request, lo que sobreescribe el
 * default del mapper sin mutarlo.</p>
 */
@Singleton
public class JacksonCustomConfig implements ObjectMapperCustomizer {

    /**
     * Aplica las dos personalizaciones descritas en el Javadoc de clase. Se
     * invoca una sola vez durante el arranque del microservicio.
     */
    @Override
    public void customize(ObjectMapper mapper) {
        mapper.setConfig(mapper.getSerializationConfig().withView(Views.Base.class));
        mapper.setConfig(mapper.getDeserializationConfig().withView(Views.Base.class));
    }
}
