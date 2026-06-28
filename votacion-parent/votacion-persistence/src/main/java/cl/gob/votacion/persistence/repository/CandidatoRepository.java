package cl.gob.votacion.persistence.repository;

import cl.gob.votacion.domain.entity.Candidato;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * Repositorio de candidatos.
 * Ruta: votacion-persistence/src/main/java/cl/gob/votacion/persistence/repository/CandidatoRepository.java
 */
@ApplicationScoped
public class CandidatoRepository {

    @PersistenceContext(unitName = "votacionPU")
    private EntityManager em;

    public List<Candidato> listarActivos(Integer eleccionId) {
        return em.createQuery(
                "SELECT c FROM Candidato c WHERE c.eleccionId = :e AND c.activo = true "
              + "ORDER BY c.numeroPapeleta", Candidato.class)
            .setParameter("e", eleccionId)
            .getResultList();
    }
}
