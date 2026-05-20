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
 *   <li>Desactiva
 *       {@link MapperFeature#DEFAULT_VIEW_INCLUSION}: una propiedad sin
 *       {@code @JsonView} <b>no</b> se serializa ni deserializa cuando hay una
 *       vista activa. Como en este proyecto todas las propiedades de DTO
 *       declaran su vista, el efecto es estricto y predecible — nada "se cuela"
 *       por defecto.</li>
 *   <li>Fija {@link Views.Base} como vista por defecto, tanto en
 *       {@code SerializationConfig} como en {@code DeserializationConfig}: si
 *       un endpoint no declara {@code @JsonView(...)}, Jackson aplica
 *       {@code Views.Base}.</li>
 * </ul>
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
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setConfig(mapper.getSerializationConfig().withView(Views.Base.class));
        mapper.setConfig(mapper.getDeserializationConfig().withView(Views.Base.class));
    }
}
