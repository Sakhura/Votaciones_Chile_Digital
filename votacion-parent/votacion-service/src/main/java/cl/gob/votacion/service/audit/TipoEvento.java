package cl.gob.votacion.service.audit;

/** Tipos de evento auditados del proceso electoral. */
public enum TipoEvento {
    AUTENTICACION,
    TOKEN_EMITIDO,
    VOTO_ENCOLADO,
    VOTO_PERSISTIDO,
    RECHAZO_DOBLE_VOTO,
    ERROR
}
