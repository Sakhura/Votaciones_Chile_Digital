package cl.gob.votacion.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Voto ANONIMO.
 * Ruta: votacion-domain/src/main/java/cl/gob/votacion/domain/entity/Voto.java
 *
 * No tiene ninguna referencia al votante. Se identifica por el hash del
 * token. hashAnterior/hashActual forman una cadena a prueba de manipulacion.
 */
@Entity
@Table(name = "voto")
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_anonimo", length = 64, nullable = false, unique = true)
    private String tokenAnonimo;

    @Column(name = "candidato_id", nullable = false)
    private Integer candidatoId;

    @Column(name = "eleccion_id", nullable = false)
    private Integer eleccionId;

    @Column(name = "region_id", nullable = false)
    private Integer regionId;

    @Column(name = "comuna_id", nullable = false)
    private Integer comunaId;

    @Column(name = "mesa_id", nullable = false)
    private Integer mesaId;

    // Instante de PERSISTENCIA (no de emision): rompe correlacion temporal.
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private Long secuencia;

    @Column(name = "hash_anterior", length = 64, nullable = false)
    private String hashAnterior;

    @Column(name = "hash_actual", length = 64, nullable = false, unique = true)
    private String hashActual;

    public Voto() { }

    public Long getId() { return id; }
    public String getTokenAnonimo() { return tokenAnonimo; }
    public void setTokenAnonimo(String t) { this.tokenAnonimo = t; }
    public Integer getCandidatoId() { return candidatoId; }
    public void setCandidatoId(Integer c) { this.candidatoId = c; }
    public Integer getEleccionId() { return eleccionId; }
    public void setEleccionId(Integer e) { this.eleccionId = e; }
    public Integer getRegionId() { return regionId; }
    public void setRegionId(Integer r) { this.regionId = r; }
    public Integer getComunaId() { return comunaId; }
    public void setComunaId(Integer c) { this.comunaId = c; }
    public Integer getMesaId() { return mesaId; }
    public void setMesaId(Integer m) { this.mesaId = m; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime f) { this.fechaHora = f; }
    public Long getSecuencia() { return secuencia; }
    public void setSecuencia(Long s) { this.secuencia = s; }
    public String getHashAnterior() { return hashAnterior; }
    public void setHashAnterior(String h) { this.hashAnterior = h; }
    public String getHashActual() { return hashActual; }
    public void setHashActual(String h) { this.hashActual = h; }
}
