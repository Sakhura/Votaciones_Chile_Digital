package cl.gob.votacion.domain.dto;

/**
 * Resultado de una autenticacion exitosa.
 * Ruta: votacion-domain/src/main/java/cl/gob/votacion/domain/dto/SesionVoto.java
 *
 * Transporta el token crudo (que el votante usara para emitir su voto) y
 * la ubicacion de mesa para el escrutinio. NO contiene RUT ni rut_hash:
 * a partir de aqui, el flujo del voto es anonimo.
 */
public record SesionVoto(
        String tokenCrudo,
        Integer regionId,
        Integer comunaId,
        Integer mesaId
) { }
