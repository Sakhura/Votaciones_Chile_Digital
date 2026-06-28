package cl.gob.votacion.service.jms;

import cl.gob.votacion.domain.dto.SolicitudVoto;
import cl.gob.votacion.domain.entity.Voto;
import cl.gob.votacion.persistence.repository.VotoRepository;
import cl.gob.votacion.service.audit.AuditoriaService;
import cl.gob.votacion.service.audit.TipoEvento;
import cl.gob.votacion.service.security.TokenService;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Consumidor de votos (Message-Driven Bean).
 * Ruta: votacion-service/src/main/java/cl/gob/votacion/service/jms/ConsumidorVotoMDB.java
 *
 * maxSession = 1: drena la cola EN SERIE. Esto garantiza una cadena de
 * hash global consistente (un voto enlaza con el anterior). El throughput
 * lo limita la BD, pero la COLA absorbe el pico, asi que el sistema no se
 * cae: los votos esperan en la cola, no en hilos bloqueados.
 *
 * Transaccion: por defecto el MDB usa una transaccion gestionada por el
 * contenedor. Si onMessage lanza excepcion, hay rollback y el mensaje se
 * reentrega (hasta agotar reintentos -> va a la DLQ).
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType",
                              propertyValue = "jakarta.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destinationLookup",
                              propertyValue = "java:/jms/cola/votos"),
    @ActivationConfigProperty(propertyName = "maxSession",
                              propertyValue = "1")
})
public class ConsumidorVotoMDB implements MessageListener {

    @Inject
    private VotoRepository votoRepository;
    @Inject
    private TokenService tokenService;
    @Inject
    private AuditoriaService auditoria;

    @Override
    public void onMessage(Message message) {
        try {
            SolicitudVoto sol = message.getBody(SolicitudVoto.class);
            String tokenHash = tokenService.hashToken(sol.tokenCrudo());

            // Idempotencia: si el token ya fue procesado, descartar sin error.
            if (votoRepository.existeToken(tokenHash)) {
                return;
            }

            String hashAnterior = votoRepository.ultimoHash();
            long secuencia = votoRepository.siguienteSecuencia();

            Voto voto = new Voto();
            voto.setTokenAnonimo(tokenHash);
            voto.setCandidatoId(sol.candidatoId());
            voto.setEleccionId(sol.eleccionId());
            voto.setRegionId(sol.regionId());
            voto.setComunaId(sol.comunaId());
            voto.setMesaId(sol.mesaId());
            voto.setFechaHora(LocalDateTime.now());   // instante de PERSISTENCIA
            voto.setSecuencia(secuencia);
            voto.setHashAnterior(hashAnterior);
            voto.setHashActual(calcularHash(hashAnterior, secuencia, sol.candidatoId(), tokenHash));

            votoRepository.guardar(voto);
            auditoria.registrarVoto(TipoEvento.VOTO_PERSISTIDO, tokenHash);

        } catch (JMSException e) {
            // Fuerza rollback -> reentrega del mensaje.
            throw new IllegalStateException("Error al procesar el voto", e);
        }
    }

    /** Eslabon de la cadena: SHA-256(hashAnterior | secuencia | candidato | token). */
    private String calcularHash(String hashAnterior, long secuencia,
                                Integer candidatoId, String tokenHash) {
        try {
            String base = hashAnterior + "|" + secuencia + "|" + candidatoId + "|" + tokenHash;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(base.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Error al calcular hash de cadena", e);
        }
    }
}
