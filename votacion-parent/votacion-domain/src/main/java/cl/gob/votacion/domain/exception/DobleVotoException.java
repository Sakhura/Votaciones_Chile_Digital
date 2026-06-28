package cl.gob.votacion.domain.exception;

/** Excepcion de negocio del proceso de votacion. */
public class DobleVotoException extends RuntimeException {
    public DobleVotoException(String mensaje) { super(mensaje); }
}
