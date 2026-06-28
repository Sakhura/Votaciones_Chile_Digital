package cl.gob.votacion.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Candidato u opcion de voto (incluye "voto en blanco" como opcion especial).
 * Ruta: votacion-domain/src/main/java/cl/gob/votacion/domain/entity/Candidato.java
 */
@Entity
@Table(name = "candidato")
public class Candidato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "eleccion_id", nullable = false)
    private Integer eleccionId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 150)
    private String partido;

    @Column(name = "numero_papeleta", nullable = false)
    private Integer numeroPapeleta;

    @Column(nullable = false)
    private boolean activo = true;

    public Candidato() { }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getEleccionId() { return eleccionId; }
    public void setEleccionId(Integer e) { this.eleccionId = e; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPartido() { return partido; }
    public void setPartido(String partido) { this.partido = partido; }
    public Integer getNumeroPapeleta() { return numeroPapeleta; }
    public void setNumeroPapeleta(Integer n) { this.numeroPapeleta = n; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
