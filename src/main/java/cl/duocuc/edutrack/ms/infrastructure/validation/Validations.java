package cl.duocuc.edutrack.ms.infrastructure.validation;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

/**
 * Grupos de Bean Validation transversales para modelar validaciones
 * condicionales por endpoint sobre un único record {@code XxxRequest}, en línea
 * con la convención "un Request, un Response" por entidad (la granularidad de
 * campos se modela con {@link cl.duocuc.edutrack.ms.infrastructure.jackson.Views @JsonView}
 * y la granularidad de validaciones se modela con estos grupos).
 *
 * <h3>Convención de uso en los DTOs</h3>
 * <ul>
 *   <li>Las restricciones de <b>formato</b> ({@code @Email}, {@code @Size},
 *       {@code @Min}, {@code @Max}, {@code @Pattern}) se declaran <i>sin</i>
 *       grupos (es decir, en el grupo {@link Default}): siempre se evalúan y
 *       son null-safe — pasan cuando el campo no viaja en esa vista.</li>
 *   <li>Las restricciones de <b>presencia</b> ({@code @NotBlank},
 *       {@code @NotNull}) se anotan con
 *       {@code groups = Validations.OnXxx.class} y solo se evalúan en los
 *       endpoints que disparen ese grupo.</li>
 * </ul>
 *
 * <h3>Cómo lo activa el endpoint</h3>
 * <p>El parámetro de body del recurso JAX-RS lleva
 * {@code @Valid @ConvertGroup(from = Default.class, to = Validations.Xxx.class)}.
 * Cada {@code Xxx} es una {@link GroupSequence} que ejecuta primero
 * {@link Default} (formato) y, si pasa, el marcador de presencia del endpoint.
 * Esto garantiza que un campo malformado no enmascare un campo faltante (las
 * sequences cortocircuitan al primer fallo).</p>
 *
 * <p>Endpoints sin presencia obligatoria adicional (típicamente {@code PATCH})
 * usan solo {@code @Valid}, lo que ejecuta el grupo {@link Default}.</p>
 *
 * <h3>Qué hay aquí y qué hay en cada MS</h3>
 * <p>Esta interfaz expone los grupos transversales: cualquier MS define al
 * menos un {@code POST} de creación. Los grupos específicos del dominio
 * (p. ej. {@code AuthValidations.OnLogin}, {@code AuthValidations.Login})
 * viven en una interfaz hermana dentro del propio microservicio, siguiendo el
 * mismo patrón marcador + secuencia.</p>
 */
public interface Validations {

    /**
     * Marcador de presencia obligatoria para creación. Se asocia a campos
     * cuya nulidad es válida en otros contextos pero no en un {@code POST}.
     * No se ejecuta por sí solo: el endpoint debe disparar {@link Create}.
     */
    interface OnCreate {}

    /**
     * Secuencia que el endpoint de creación dispara: primero {@link Default}
     * (validaciones de formato), y solo si pasan, {@link OnCreate}
     * (validaciones de presencia obligatoria). El orden corto-circuita: un
     * fallo de formato impide reportar un fallo de presencia, lo que mantiene
     * los errores acotados a un único nivel a la vez.
     */
    @GroupSequence({ Default.class, OnCreate.class })
    interface Create {}
}
