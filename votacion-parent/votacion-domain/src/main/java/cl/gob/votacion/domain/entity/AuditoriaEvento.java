package cl.gob.votacion.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Evento de auditoria del proceso (no del voto).
 * Ruta: votacion-domain/src/main/java/cl/gob/votacion/domain/entity/AuditoriaEvento.java
 *
 * Regla de anonimato: una fila lleva rut_hash (eventos de persona) O
 * token_ref (eventos de voto), nunca ambos, y nunca el candidato.
 */
@Entity
@Table(name = "auditoria_evento")
public class AuditoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_evento", nullable = false, length = 30)
    private String tipoEvento;

    @Column(name = "rut_hash", length = 64)
    private String rutHash;

    @Column(name = "token_ref", length = 64)
    private String tokenRef;

    @Column(length = 255)
    private String detalle;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    public AuditoriaEvento() { }

    public Long getId() { return id; }
    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String t) { this.tipoEvento = t; }
    public String getRutHash() { return rutHash; }
    public void setRutHash(String r) { this.rutHash = r; }
    public String getTokenRef() { return tokenRef; }
    public void setTokenRef(String t) { this.tokenRef = t; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String d) { this.detalle = d; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime f) { this.fechaHora = f; }
}
