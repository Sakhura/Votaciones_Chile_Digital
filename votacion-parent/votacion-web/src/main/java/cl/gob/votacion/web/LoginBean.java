package cl.gob.votacion.web;

import cl.gob.votacion.domain.dto.SesionVoto;
import cl.gob.votacion.domain.exception.CredencialInvalidaException;
import cl.gob.votacion.domain.exception.DobleVotoException;
import cl.gob.votacion.domain.exception.VotanteNoHabilitadoException;
import cl.gob.votacion.service.auth.AutenticacionService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Backing bean del formulario de ingreso.
 * Ruta: votacion-web/src/main/java/cl/gob/votacion/web/LoginBean.java
 */
@Named
@RequestScoped
public class LoginBean {

    private String rut;
    private String credencial;

    @Inject
    private AutenticacionService autenticacionService;
    @Inject
    private SesionVotanteBean sesionBean;

    public String autenticar() {
        try {
            SesionVoto sesion = autenticacionService.autenticar(rut, credencial.toCharArray());
            sesionBean.iniciar(sesion);
            // Redirige al formulario de voto (PRG: evita reenvio por F5)
            return "votacion.xhtml?faces-redirect=true";
        } catch (DobleVotoException e) {
            mensaje("Usted ya emitio su voto. No es posible votar de nuevo.");
        } catch (CredencialInvalidaException | VotanteNoHabilitadoException e) {
            // Mensaje GENERICO a proposito: no revelamos si fallo el RUT o la
            // clave (evita enumeracion de votantes validos).
            mensaje("RUT o clave no validos. Verifique sus datos.");
        } finally {
            credencial = null; // higiene
        }
        return null;
    }

    private void mensaje(String texto) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, texto, null));
    }

    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }
    public String getCredencial() { return credencial; }
    public void setCredencial(String credencial) { this.credencial = credencial; }
}
