package cl.gob.votacion.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/**
 * Entidad del padron electoral.
 * Ruta: votacion-domain/src/main/java/cl/gob/votacion/domain/entity/Votante.java
 *
 * - La clave primaria es el rut_hash (HMAC del RUT): el RUT en claro
 *   nunca se persiste.
 * - @Version habilita el bloqueo optimista de JPA. Aunque el doble voto
 *   se ataca con el UPDATE condicional, @Version protege otras
 *   actualizaciones concurrentes de la misma fila.
 */
@Entity
@Table(name = "votante")
public class Votante {

    @Id
    @Column(name = "rut_hash", length = 64, nullable = false)
    private String rutHash;

    @Column(name = "rut_dv", length = 1, nullable = false)
    private String rutDv;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "region_id", nullable = false)
    private Integer regionId;

    @Column(name = "comuna_id", nullable = false)
    private Integer comunaId;

    @Column(name = "mesa_id", nullable = false)
    private Integer mesaId;

    @Column(name = "credencial_hash", length = 255)
    private String credencialHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_voto", nullable = false)
    private EstadoVoto estadoVoto = EstadoVoto.HABILITADO;

    @Column(name = "fecha_habilitacion")
    private LocalDateTime fechaHabilitacion;

    @Column(name = "fecha_marca_voto")
    private LocalDateTime fechaMarcaVoto;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Version
    @Column(nullable = false)
    private int version;

    public Votante() { }

    // ----- getters / setters -----
    public String getRutHash() { return rutHash; }
    public void setRutHash(String rutHash) { this.rutHash = rutHash; }

    public String getRutDv() { return rutDv; }
    public void setRutDv(String rutDv) { this.rutDv = rutDv; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getRegionId() { return regionId; }
    public void setRegionId(Integer regionId) { this.regionId = regionId; }

    public Integer getComunaId() { return comunaId; }
    public void setComunaId(Integer comunaId) { this.comunaId = comunaId; }

    public Integer getMesaId() { return mesaId; }
    public void setMesaId(Integer mesaId) { this.mesaId = mesaId; }

    public String getCredencialHash() { return credencialHash; }
    public void setCredencialHash(String credencialHash) { this.credencialHash = credencialHash; }

    public EstadoVoto getEstadoVoto() { return estadoVoto; }
    public void setEstadoVoto(EstadoVoto estadoVoto) { this.estadoVoto = estadoVoto; }

    public LocalDateTime getFechaHabilitacion() { return fechaHabilitacion; }
    public void setFechaHabilitacion(LocalDateTime f) { this.fechaHabilitacion = f; }

    public LocalDateTime getFechaMarcaVoto() { return fechaMarcaVoto; }
    public void setFechaMarcaVoto(LocalDateTime f) { this.fechaMarcaVoto = f; }

    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int n) { this.intentosFallidos = n; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
