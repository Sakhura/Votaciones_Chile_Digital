package cl.gob.votacion.persistence.repository;

import cl.gob.votacion.domain.entity.Voto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * Repositorio de votos.
 * Ruta: votacion-persistence/src/main/java/cl/gob/votacion/persistence/repository/VotoRepository.java
 *
 * ultimoHash() y siguienteSecuencia() son consistentes porque el MDB
 * consume en serie (maxSession=1): no hay dos hilos calculando la cadena
 * a la vez. Para mayor throughput se usarian cadenas por region.
 */
@ApplicationScoped
public class VotoRepository {

    @PersistenceContext(unitName = "votacionPU")
    private EntityManager em;

    /** Idempotencia: evita procesar dos veces el mismo token. */
    public boolean existeToken(String tokenHash) {
        Long n = em.createQuery(
                "SELECT COUNT(v) FROM Voto v WHERE v.tokenAnonimo = :t", Long.class)
            .setParameter("t", tokenHash)
            .getSingleResult();
        return n > 0;
    }

    /** Hash del ultimo voto de la cadena, o "GENESIS" si es el primero. */
    public String ultimoHash() {
        List<String> r = em.createQuery(
                "SELECT v.hashActual FROM Voto v ORDER BY v.secuencia DESC", String.class)
            .setMaxResults(1)
            .getResultList();
        return r.isEmpty() ? "GENESIS" : r.get(0);
    }

    public long siguienteSecuencia() {
        Long max = em.createQuery(
                "SELECT COALESCE(MAX(v.secuencia), 0) FROM Voto v", Long.class)
            .getSingleResult();
        return max + 1;
    }

    public void guardar(Voto voto) {
        em.persist(voto);
    }
}
