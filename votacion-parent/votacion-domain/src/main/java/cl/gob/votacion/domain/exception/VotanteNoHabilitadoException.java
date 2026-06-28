package cl.gob.votacion.domain.exception;

/** Excepcion de negocio del proceso de votacion. */
public class VotanteNoHabilitadoException extends RuntimeException {
    public VotanteNoHabilitadoException(String mensaje) { super(mensaje); }
}
