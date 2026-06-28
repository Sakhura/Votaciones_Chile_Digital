package cl.gob.votacion.service.audit;

import cl.gob.votacion.domain.entity.AuditoriaEvento;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;

/**
 * Servicio de auditoria del proceso.
 * Ruta: votacion-service/src/main/java/cl/gob/votacion/service/audit/AuditoriaService.java
 *
 * REQUIRES_NEW: cada registro de auditoria se guarda en su PROPIA
 * transaccion. Asi, si la transaccion principal hace rollback (ej. un
 * rechazo de doble voto), la evidencia del intento NO se borra.
 */
@Stateless
public class AuditoriaService {

    @PersistenceContext(unitName = "votacionPU")
    private EntityManager em;

    /** Evento de PERSONA: lleva rut_hash, nunca el token. */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void registrar(TipoEvento tipo, String rutHash, String detalle) {
        AuditoriaEvento ev = new AuditoriaEvento();
        ev.setTipoEvento(tipo.name());
        ev.setRutHash(rutHash);
        ev.setDetalle(detalle);
        ev.setFechaHora(LocalDateTime.now());
        em.persist(ev);
    }

    /** Evento de VOTO: lleva token_ref, nunca el rut_hash. */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void registrarVoto(TipoEvento tipo, String tokenRef) {
        AuditoriaEvento ev = new AuditoriaEvento();
        ev.setTipoEvento(tipo.name());
        ev.setTokenRef(tokenRef);
        ev.setFechaHora(LocalDateTime.now());
        em.persist(ev);
    }
}
