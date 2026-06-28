package cl.gob.votacion.domain.entity;

/**
 * Estados del votante en el proceso electoral.
 *
 *  HABILITADO -> VOTANDO -> VOTADO
 *
 * La transicion HABILITADO -> VOTANDO es el "lock logico" que impide
 * el doble voto: solo una peticion concurrente puede realizarla.
 * BLOQUEADO es un estado terminal para votantes inhabilitados.
 *
 * Ruta: votacion-domain/src/main/java/cl/gob/votacion/domain/entity/EstadoVoto.java
 */
public enum EstadoVoto {
    HABILITADO,
    VOTANDO,
    VOTADO,
    BLOQUEADO
}
