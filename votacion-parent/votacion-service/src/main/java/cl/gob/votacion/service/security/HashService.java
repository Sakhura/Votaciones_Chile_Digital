package cl.gob.votacion.service.security;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Servicio de hashing del sistema.
 * Ruta: votacion-service/src/main/java/cl/gob/votacion/service/security/HashService.java
 *
 * Dos responsabilidades, dos algoritmos:
 *  - hashRut(): HMAC-SHA256 DETERMINISTA (mismo RUT -> mismo hash) para
 *    poder buscar al votante por su hash sin guardar el RUT en claro.
 *    Usa un "pepper" secreto: sin el, un atacante con la BD no puede
 *    construir una tabla rainbow de RUTs (que son predecibles).
 *  - verificarCredencial(): PBKDF2 LENTO Y SALADO, disenado para
 *    resistir fuerza bruta. Se compara en tiempo constante.
 *
 * PRODUCCION: el pepper debe provenir del HSM (PKCS#11), nunca del codigo
 * ni de una variable de entorno. Aqui se simula para el piloto local.
 */
@ApplicationScoped
public class HashService {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String PBKDF2_ALGO = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERACIONES = 120_000;
    private static final int PBKDF2_LONGITUD_BITS = 256;

    private byte[] pepper;

    @PostConstruct
    void init() {
        // Piloto local: system property o variable de entorno.
        // Produccion: reemplazar por lectura desde HSM.
        String p = System.getProperty("votacion.rut.pepper");
        if (p == null) {
            p = System.getenv().getOrDefault("VOTACION_RUT_PEPPER", "PEPPER_DEV_CAMBIAR");
        }
        this.pepper = p.getBytes(StandardCharsets.UTF_8);
    }

    /** HMAC-SHA256 del RUT normalizado. Devuelve 64 caracteres hex. */
    public String hashRut(String rutNormalizado) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(pepper, HMAC_ALGO));
            byte[] h = mac.doFinal(rutNormalizado.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error al calcular HMAC del RUT", e);
        }
    }

    /** Genera el hash de una credencial nueva (para carga de datos). */
    public String generarHashCredencial(char[] credencial) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(credencial, salt, PBKDF2_ITERACIONES);
        return PBKDF2_ITERACIONES + ":"
                + HexFormat.of().formatHex(salt) + ":"
                + HexFormat.of().formatHex(hash);
    }

    /** Verifica una credencial contra el hash almacenado (tiempo constante). */
    public boolean verificarCredencial(char[] credencial, String almacenado) {
        if (almacenado == null) {
            return false;
        }
        try {
            String[] partes = almacenado.split(":");
            int iter = Integer.parseInt(partes[0]);
            byte[] salt = HexFormat.of().parseHex(partes[1]);
            byte[] esperado = HexFormat.of().parseHex(partes[2]);
            byte[] actual = pbkdf2(credencial, salt, iter);
            // MessageDigest.isEqual compara en tiempo constante:
            // evita filtrar informacion por diferencias de tiempo.
            return MessageDigest.isEqual(esperado, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private byte[] pbkdf2(char[] credencial, byte[] salt, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(credencial, salt, iteraciones, PBKDF2_LONGITUD_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGO);
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error al calcular PBKDF2", e);
        }
    }
}
