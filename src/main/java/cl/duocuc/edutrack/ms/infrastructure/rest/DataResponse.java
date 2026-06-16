package cl.duocuc.edutrack.ms.infrastructure.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envelope JSON único de <b>respuesta exitosa</b> compartido por todos los
 * microservicios. Es la contraparte de {@code exception.ErrorResponse}: donde
 * aquél estandariza la forma de un error, éste estandariza la forma de un
 * payload con metadatos asociados.
 *
 * <p>Forma del cable:</p>
 * <pre>{@code
 * {
 *   "data": <T>,
 *   "meta": { ... }
 * }
 * }</pre>
 *
 * <p>{@code data} es la carga útil (cualquier tipo serializable — un objeto, una
 * {@code List<String>}, etc.) y <b>siempre</b> viaja, incluso cuando es una
 * colección vacía. {@code meta} es un mapa abierto de clave/valor para contexto
 * estructurado (totales, nombre del servicio, paginación…) y se <b>omite</b>
 * cuando está vacío ({@link JsonInclude.Include#NON_EMPTY} a nivel de componente),
 * de modo que un envelope sin metadatos queda como {@code {"data": ...}}.</p>
 *
 * <p>La construcción pasa por los factory {@link #of(Object)} /
 * {@link #of(Object, Map)} y el {@code meta} se enriquece de forma fluida con
 * {@link #with(String, Object)} (mismo estilo encadenable que
 * {@code DomainException.with(...)}):</p>
 *
 * <pre>{@code
 * return DataResponse.of(resourceKeys)
 *     .with("service", ServiceIds.COURSE)
 *     .with("count", resourceKeys.size());
 * }</pre>
 *
 * @param <T>  tipo de la carga útil
 * @param data carga útil; nunca se omite (incluso vacía o {@code null})
 * @param meta contexto estructurado; se omite del JSON cuando está vacío
 */
public record DataResponse<T>(
    T data,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, Object> meta
) {

    /** Garantiza que {@code meta} nunca sea {@code null} (siempre encadenable con {@link #with}). */
    public DataResponse {
        if (meta == null) {
            meta = new LinkedHashMap<>();
        }
    }

    /** Envelope sin metadatos. El {@code meta} queda vacío y listo para {@link #with}. */
    public static <T> DataResponse<T> of(T data) {
        return new DataResponse<>(data, new LinkedHashMap<>());
    }

    /** Envelope con metadatos iniciales (copiados a un mapa mutable propio). */
    public static <T> DataResponse<T> of(T data, Map<String, Object> meta) {
        return new DataResponse<>(data, meta == null ? new LinkedHashMap<>() : new LinkedHashMap<>(meta));
    }

    /** Agrega una entrada a {@code meta} y devuelve {@code this} para encadenar. */
    public DataResponse<T> with(String key, Object value) {
        meta.put(key, value);
        return this;
    }
}
