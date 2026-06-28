package cl.gob.votacion.domain.exception;

/** Excepcion de negocio del proceso de votacion. */
public class CredencialInvalidaException extends RuntimeException {
    public CredencialInvalidaException(String mensaje) { super(mensaje); }
}
