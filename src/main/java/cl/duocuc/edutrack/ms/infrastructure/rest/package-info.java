/**
 * Envelopes REST transversales de <b>respuesta exitosa</b>, compartidos por todos
 * los microservicios. Complementa a {@code infrastructure.exception}, que ya
 * estandariza la forma de los errores ({@code ErrorResponse}): aquí vive la forma
 * estándar de un payload con metadatos.
 *
 * <h2>Contrato {@code DataResponse<T>} — {@code {data, meta}}</h2>
 * <p>{@link cl.duocuc.edutrack.ms.infrastructure.rest.DataResponse} es el envelope
 * genérico {@code {"data": <T>, "meta": {...}}}. {@code data} es la carga útil
 * (siempre presente) y {@code meta} un mapa abierto de contexto estructurado
 * (omitido cuando vacío).</p>
 *
 * <h2>Convención de metadatos de servicio — {@code GET /<servicio>/meta/...}</h2>
 * <p>El prefijo de path <b>{@code /<servicio>/meta/}</b> es un namespace público
 * reservado para que cada MS <b>se auto-describa</b> (no expone datos de dominio,
 * solo metadatos del servicio). Es público tras el Gateway (sin JWT) — ver la
 * location {@code ^/[^/]+/meta/} en {@code infra/gateway/nginx.conf.template}.</p>
 *
 * <p>El primer endpoint del contrato es <b>{@code GET /<servicio>/meta/resources}</b>:
 * devuelve, en un {@code DataResponse<List<String>>}, el catálogo de
 * <em>resource keys</em> que ese servicio protege con permisos Unix-style — la
 * fuente de verdad descentralizada (cada MS la deriva en código de su propio
 * {@code XxxResourceId}). Un agregador (frontend/BFF) barre {@code ServiceIds.ALL}
 * para poblar la UI de administración de permisos por rol con el universo de
 * recursos asignables. {@code meta} lleva al menos {@code service} (id lógico bare)
 * y {@code count}.</p>
 *
 * <p>Implementación de referencia: cada MS aporta un recurso JAX-RS
 * {@code ResourceCatalogResource} en su paquete {@code <servicio>.resource} con
 * {@code @Path("/meta/resources")}, sin {@code @RequirePermission}.</p>
 */
package cl.duocuc.edutrack.ms.infrastructure.rest;
