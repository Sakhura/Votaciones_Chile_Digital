package cl.gob.votacion.service;

import cl.gob.votacion.domain.entity.Candidato;
import cl.gob.votacion.domain.entity.Eleccion;
import cl.gob.votacion.domain.entity.EstadoVoto;
import cl.gob.votacion.domain.entity.Votante;
import cl.gob.votacion.service.security.HashService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;

/**
 * Carga datos de prueba al desplegar (solo si la BD esta vacia).
 * Ruta: votacion-service/src/main/java/cl/gob/votacion/service/CargaDatosPrueba.java
 *
 * SOLO PARA EL PILOTO. En produccion el padron se carga desde el Servicio
 * Electoral por un proceso ETL auditado, jamas hardcodeado.
 *
 * Votantes de prueba (RUT / clave):
 *   11111111-1 / 1234
 *   22222222-2 / 1234
 *   33333333-3 / 1234
 */
@Singleton
@Startup
public class CargaDatosPrueba {

    @PersistenceContext(unitName = "votacionPU")
    private EntityManager em;

    @Inject
    private HashService hashService;

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void cargar() {
        Long elecciones = em.createQuery("SELECT COUNT(e) FROM Eleccion e", Long.class)
                .getSingleResult();
        if (elecciones > 0) {
            return; // ya hay datos, no recargar
        }

        // 1) Eleccion
        Eleccion eleccion = new Eleccion();
        eleccion.setNombre("Plebiscito Nacional - Piloto");
        eleccion.setTipo("PLEBISCITO");
        eleccion.setFechaInicio(LocalDateTime.now());
        eleccion.setFechaFin(LocalDateTime.now().plusDays(1));
        eleccion.setEstado("ABIERTA");
        em.persist(eleccion);
        em.flush(); // para obtener el id

        // 2) Candidatos (incluye voto en blanco)
        crearCandidato(eleccion.getId(), 1, "Opcion A", "Lista 1");
        crearCandidato(eleccion.getId(), 2, "Opcion B", "Lista 2");
        crearCandidato(eleccion.getId(), 3, "Opcion C", "Independiente");
        crearCandidato(eleccion.getId(), 99, "Voto en blanco", null);

        // 3) Votantes de prueba (RUT cuerpo sin DV -> hash)
        crearVotante("11111111", "1", "Ana Perez",     13, 1301, 101);
        crearVotante("22222222", "2", "Luis Soto",      13, 1301, 101);
        crearVotante("33333333", "3", "Marta Diaz",     5,  501,  220);
    }

    private void crearCandidato(Integer eleccionId, int numero, String nombre, String partido) {
        Candidato c = new Candidato();
        c.setEleccionId(eleccionId);
        c.setNumeroPapeleta(numero);
        c.setNombre(nombre);
        c.setPartido(partido);
        c.setActivo(true);
        em.persist(c);
    }

    private void crearVotante(String rutCuerpo, String dv, String nombre,
                              int region, int comuna, int mesa) {
        Votante v = new Votante();
        v.setRutHash(hashService.hashRut(rutCuerpo));
        v.setRutDv(dv);
        v.setNombre(nombre);
        v.setRegionId(region);
        v.setComunaId(comuna);
        v.setMesaId(mesa);
        v.setCredencialHash(hashService.generarHashCredencial("1234".toCharArray()));
        v.setEstadoVoto(EstadoVoto.HABILITADO);
        em.persist(v);
    }
}
