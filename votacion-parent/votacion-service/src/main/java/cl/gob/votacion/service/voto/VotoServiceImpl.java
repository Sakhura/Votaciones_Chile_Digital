package cl.gob.votacion.service.voto;

import cl.gob.votacion.domain.dto.SolicitudVoto;
import cl.gob.votacion.service.audit.AuditoriaService;
import cl.gob.votacion.service.audit.TipoEvento;
import cl.gob.votacion.service.security.TokenService;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;

/**
 * Productor de votos (implementa VotoService).
 * Ruta: votacion-service/src/main/java/cl/gob/votacion/service/voto/VotoServiceImpl.java
 *
 * Solo ENCOLA: no toca la BD de votos. La operacion es de milisegundos.
 * Al ser transaccional (REQUIRED), el mensaje se confirma en la cola al
 * retornar el metodo: cuando la UI dice "registrado", el voto ya esta
 * durablemente en la cola.
 */
@Stateless
public class VotoServiceImpl implements VotoService {

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = "java:/jms/cola/votos")
    private Queue colaVotos;

    @Inject
    private TokenService tokenService;
    @Inject
    private AuditoriaService auditoria;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void encolarVoto(SolicitudVoto solicitud) {
        // Envia el DTO como ObjectMessage (SolicitudVoto es Serializable).
        jmsContext.createProducer().send(colaVotos, solicitud);

        // Auditoria del encolado: registra el HASH del token (token_ref),
        // nunca el RUT. Aqui ya no se conoce la identidad del votante.
        String tokenHash = tokenService.hashToken(solicitud.tokenCrudo());
        auditoria.registrarVoto(TipoEvento.VOTO_ENCOLADO, tokenHash);
    }
}
