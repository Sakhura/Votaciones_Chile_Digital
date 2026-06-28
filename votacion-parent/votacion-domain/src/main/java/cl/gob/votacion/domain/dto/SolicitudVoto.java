package cl.gob.votacion.domain.dto;

import java.io.Serializable;

/**
 * Solicitud de voto que viaja a la cola JMS.
 * Implementa Serializable porque se envia como ObjectMessage.
 * No contiene RUT: a partir de la autenticacion el flujo es anonimo.
 */
public record SolicitudVoto(
        String tokenCrudo,
        Integer candidatoId,
        Integer eleccionId,
        Integer regionId,
        Integer comunaId,
        Integer mesaId
) implements Serializable { }
