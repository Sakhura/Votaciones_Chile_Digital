package cl.gob.votacion.web;

import cl.gob.votacion.domain.dto.SesionVoto;
import cl.gob.votacion.domain.dto.SolicitudVoto;
import cl.gob.votacion.domain.entity.Candidato;
import cl.gob.votacion.persistence.repository.CandidatoRepository;
import cl.gob.votacion.service.voto.VotoService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * Backing bean del formulario de votacion.
 * Ruta: votacion-web/src/main/java/cl/gob/votacion/web/VotacionBean.java
 *
 * @ViewScoped + Serializable: el estado vive mientras dura la vista.
 */
@Named
@ViewScoped
public class VotacionBean implements Serializable {

    // Piloto: una unica eleccion activa. Produccion: resolver dinamicamente.
    private static final Integer ELECCION_ID = 1;

    private List<Candidato> candidatos;
    private Integer candidatoSeleccionado;
    private boolean votoEmitido;

    @Inject
    private SesionVotanteBean sesionBean;
    @Inject
    private CandidatoRepository candidatoRepository;
    @Inject
    private VotoService votoService;

    @PostConstruct
    void init() {
        candidatos = candidatoRepository.listarActivos(ELECCION_ID);
    }

    /** Guarda de acceso (se invoca con f:viewAction). Sin sesion -> al login. */
    public String verificarAcceso() {
        if (!sesionBean.isAutenticado()) {
            return "login.xhtml?faces-redirect=true";
        }
        return null;
    }

    /** Emite el voto: lo encola (JMS) y CIERRA la sesion de inmediato. */
    public void emitirVoto() {
        if (!sesionBean.isAutenticado()) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Su sesion no es valida. Ingrese nuevamente.");
            return;
        }
        if (candidatoSeleccionado == null) {
            mensaje(FacesMessage.SEVERITY_WARN, "Seleccione una opcion antes de emitir su voto.");
            return;
        }

        SesionVoto s = sesionBean.getSesion();
        // El voto viaja con token + ubicacion de mesa, NUNCA con el RUT.
        votoService.encolarVoto(new SolicitudVoto(
                s.tokenCrudo(), candidatoSeleccionado, ELECCION_ID,
                s.regionId(), s.comunaId(), s.mesaId()));

        votoEmitido = true;
        // Invalida el token en sesion: aunque el usuario use "atras", no
        // puede reenviar. La defensa final es la clave unica uk_token en BD.
        sesionBean.limpiar();
        mensaje(FacesMessage.SEVERITY_INFO, "Su voto fue registrado correctamente.");
    }

    private void mensaje(FacesMessage.Severity sev, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, texto, null));
    }

    public List<Candidato> getCandidatos() { return candidatos; }
    public Integer getCandidatoSeleccionado() { return candidatoSeleccionado; }
    public void setCandidatoSeleccionado(Integer c) { this.candidatoSeleccionado = c; }
    public boolean isVotoEmitido() { return votoEmitido; }
}
