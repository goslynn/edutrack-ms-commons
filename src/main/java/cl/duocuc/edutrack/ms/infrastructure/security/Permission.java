package cl.duocuc.edutrack.ms.infrastructure.security;

/**
 * Bits de permiso Unix-style usados por la anotación
 * {@link RequirePermission} para declarar el mínimo requerido por un endpoint.
 *
 * <p>Los valores son los tres bits clásicos del modelo de permisos de Unix
 * sobre un solo byte: {@code r=4, w=2, x=1}. La combinación de bits efectivos
 * que el llamante posee sobre un recurso se almacena como un {@code SMALLINT}
 * en la base de datos del Auth Service y se evalúa con un AND bit a bit contra
 * el valor solicitado:</p>
 *
 * <pre>{@code (effectiveFlags & requested.bit) == requested.bit}</pre>
 *
 * <p>Cada constante expone su {@link #bit} como {@code short} para que el
 * filtro y los evaluadores puedan operar directamente sobre el valor numérico
 * sin volver a calcularlo.</p>
 */
public enum Permission {

    /** Lectura ({@code 4}). Endpoints {@code GET}/listados. */
    READ((short) 4),

    /**
     * Escritura ({@code 2}). Endpoints que crean, modifican o eliminan estado:
     * {@code POST}, {@code PUT}, {@code PATCH}, {@code DELETE}.
     */
    WRITE((short) 2),

    /**
     * Ejecución ({@code 1}). Acciones que no son CRUD: disparar un proceso,
     * emitir tokens, marcar asistencia, etc.
     */
    EXECUTE((short) 1);

    /** Bit del permiso ({@code 4}, {@code 2} o {@code 1}). */
    public final short bit;

    Permission(short bit) {
        this.bit = bit;
    }
}
