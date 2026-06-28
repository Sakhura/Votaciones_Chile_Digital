package cl.gob.votacion.service.auth;

import cl.gob.votacion.domain.dto.SesionVoto;
import cl.gob.votacion.domain.entity.EstadoVoto;
import cl.gob.votacion.domain.entity.Votante;
import cl.gob.votacion.persistence.repository.VotanteRepository;
import cl.gob.votacion.service.security.HashService;
import cl.gob.votacion.service.security.TokenService;
import cl.gob.votacion.service.audit.AuditoriaService;
import cl.gob.votacion.service.audit.TipoEvento;
import cl.gob.votacion.domain.exception.CredencialInvalidaException;
import cl.gob.votacion.domain.exception.DobleVotoException;
import cl.gob.votacion.domain.exception.VotanteNoHabilitadoException;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import java.util.Arrays;

/**
 * Servicio de autenticacion y validacion del votante.
 * Ruta: votacion-service/src/main/java/cl/gob/votacion/service/auth/AutenticacionService.java
 *
 * EJB Stateless: cada llamada es independiente (encaja con la capa de
 * aplicacion sin estado y escalable). La transaccion la gestiona el
 * contenedor (CMT) -> si algo falla, todo hace rollback automaticamente.
 */
@Stateless
public class AutenticacionService {

    @Inject
    private VotanteRepository votanteRepo;
    @Inject
    private HashService hashService;
    @Inject
    private TokenService tokenService;
    @Inject
    private AuditoriaService auditoria;

    /**
     * Autentica al votante y, si procede, gana el lock logico de voto y
     * emite un token anonimo.
     *
     * @throws VotanteNoHabilitadoException si el RUT no esta en el padron
     *         o esta bloqueado.
     * @throws CredencialInvalidaException  si la credencial no coincide.
     * @throws DobleVotoException           si el votante ya emitio su voto.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public SesionVoto autenticar(String rut, char[] credencial) {
        try {
            String rutNormalizado = normalizarRut(rut);
            String rutHash = hashService.hashRut(rutNormalizado);

            Votante votante = votanteRepo.buscarPorRutHash(rutHash);
            if (votante == null || votante.getEstadoVoto() == EstadoVoto.BLOQUEADO) {
                auditoria.registrar(TipoEvento.AUTENTICACION, rutHash, "RECHAZO_PADRON");
                throw new VotanteNoHabilitadoException("RUT no habilitado para votar");
            }

            if (!hashService.verificarCredencial(credencial, votante.getCredencialHash())) {
                auditoria.registrar(TipoEvento.AUTENTICACION, rutHash, "CREDENCIAL_INVALIDA");
                throw new CredencialInvalidaException("Credencial incorrecta");
            }

            // ===== UPDATE atomico anti-doble-voto =====
            int filas = votanteRepo.marcarVotando(rutHash);
            if (filas == 0) {
                // Otra peticion gano la carrera, o ya habia votado antes.
                auditoria.registrar(TipoEvento.RECHAZO_DOBLE_VOTO, rutHash, null);
                throw new DobleVotoException("El votante ya emitio su voto");
            }

            // Token anonimo. Auditamos rut_hash SIN el token -> preserva anonimato.
            String tokenCrudo = tokenService.generarTokenCrudo();
            auditoria.registrar(TipoEvento.TOKEN_EMITIDO, rutHash, null);

            return new SesionVoto(
                    tokenCrudo,
                    votante.getRegionId(),
                    votante.getComunaId(),
                    votante.getMesaId());

        } finally {
            // Higiene de memoria: borrar la credencial del arreglo.
            if (credencial != null) {
                Arrays.fill(credencial, '\0');
            }
        }
    }

    /** Normaliza el RUT: sin puntos, sin guion, en mayuscula, sin DV. */
    private String normalizarRut(String rut) {
        if (rut == null) {
            throw new VotanteNoHabilitadoException("RUT vacio");
        }
        String limpio = rut.trim().toUpperCase()
                .replace(".", "")
                .replace("-", "");
        // Quita el digito verificador (ultimo caracter) para el hash del cuerpo.
        return limpio.length() > 1 ? limpio.substring(0, limpio.length() - 1) : limpio;
    }
}
