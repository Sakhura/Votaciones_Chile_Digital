package cl.gob.votacion.web;

import cl.gob.votacion.domain.dto.SesionVoto;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;

/**
 * Portador de la sesion de voto entre vistas.
 * Ruta: votacion-web/src/main/java/cl/gob/votacion/web/SesionVotanteBean.java
 *
 * Guarda el resultado de la autenticacion (token + ubicacion de mesa).
 * Se limpia en cuanto el voto se encola: el token deja de ser reutilizable.
 */
@Named
@SessionScoped
public class SesionVotanteBean implements Serializable {

    private SesionVoto sesion;

    public boolean isAutenticado() {
        return sesion != null;
    }

    public void iniciar(SesionVoto sesion) {
        this.sesion = sesion;
    }

    public void limpiar() {
        this.sesion = null;
    }

    public SesionVoto getSesion() {
        return sesion;
    }
}
