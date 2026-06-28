package cl.gob.votacion.service.voto;

import cl.gob.votacion.domain.dto.SolicitudVoto;

/** Contrato del servicio de votos. Lo implementa VotoServiceImpl (productor JMS). */
public interface VotoService {
    void encolarVoto(SolicitudVoto solicitud);
}
