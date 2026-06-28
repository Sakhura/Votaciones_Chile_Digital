package cl.gob.votacion.service.security;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Servicio de tokens anonimos de un solo uso.
 * Ruta: votacion-service/src/main/java/cl/gob/votacion/service/security/TokenService.java
 *
 * Flujo del token:
 *  1) Al autenticarse, se genera un token CRUDO (aleatorio, 256 bits) que
 *     se entrega al votante (vive en su sesion HTTP).
 *  2) Al emitir el voto, el votante envia el token crudo.
 *  3) En la tabla `voto` se guarda solo el HASH del token (token_anonimo),
 *     no el token crudo. La clave unica uk_token impide reusarlo.
 *
 * El token no se asocia al RUT en ninguna tabla -> rompe la trazabilidad
 * persona<->voto. (El blindaje criptografico completo via firmas ciegas
 * queda como requisito de produccion.)
 */
@ApplicationScoped
public class TokenService {

    private final SecureRandom rng = new SecureRandom();

    /** Token aleatorio de 256 bits, seguro para URL. Se entrega al votante. */
    public String generarTokenCrudo() {
        byte[] bytes = new byte[32];
        rng.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Hash SHA-256 del token: es lo unico que se persiste en `voto`. */
    public String hashToken(String tokenCrudo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(tokenCrudo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
