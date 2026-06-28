package cl.gob.votacion.persistence.repository;

import cl.gob.votacion.domain.entity.EstadoVoto;
import cl.gob.votacion.domain.entity.Votante;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;

/**
 * Repositorio del padron.
 * Ruta: votacion-persistence/src/main/java/cl/gob/votacion/persistence/repository/VotanteRepository.java
 */
@ApplicationScoped
public class VotanteRepository {

    @PersistenceContext(unitName = "votacionPU")
    private EntityManager em;

    public Votante buscarPorRutHash(String rutHash) {
        return em.find(Votante.class, rutHash);
    }

    /**
     * UPDATE CONDICIONAL ATOMICO: el corazon de la prevencion del doble voto.
     *
     * La condicion "estado = HABILITADO" va DENTRO del WHERE. InnoDB
     * serializa el acceso a la fila, asi que de N peticiones concurrentes
     * del mismo RUT, solo UNA obtiene filasAfectadas = 1 (gana el derecho
     * a votar); el resto obtiene 0 (rechazadas).
     *
     * Esto evita la condicion de carrera del patron SELECT-luego-UPDATE.
     *
     * @return 1 si gano el lock logico, 0 si ya estaba VOTANDO/VOTADO.
     */
    public int marcarVotando(String rutHash) {
        return em.createQuery(
                "UPDATE Votante v SET v.estadoVoto = :nuevo, "
              + "v.fechaHabilitacion = :ahora, v.version = v.version + 1 "
              + "WHERE v.rutHash = :h AND v.estadoVoto = :esperado")
            .setParameter("nuevo", EstadoVoto.VOTANDO)
            .setParameter("ahora", LocalDateTime.now())
            .setParameter("h", rutHash)
            .setParameter("esperado", EstadoVoto.HABILITADO)
            .executeUpdate();
    }

    /**
     * Marca VOTANDO -> VOTADO una vez confirmado el registro del voto.
     * @return 1 en exito, 0 si el estado no era el esperado.
     */
    public int marcarVotado(String rutHash) {
        return em.createQuery(
                "UPDATE Votante v SET v.estadoVoto = :nuevo, "
              + "v.fechaMarcaVoto = :ahora, v.version = v.version + 1 "
              + "WHERE v.rutHash = :h AND v.estadoVoto = :esperado")
            .setParameter("nuevo", EstadoVoto.VOTADO)
            .setParameter("ahora", LocalDateTime.now())
            .setParameter("h", rutHash)
            .setParameter("esperado", EstadoVoto.VOTANDO)
            .executeUpdate();
    }
}
