package com.github.heikyudev.maestrocervecero.service.implementation.distribucion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token del enlace con el que un cliente coordina el retiro de sus barriles: {@value #BYTES_TOKEN} bytes aleatorios de un
 * generador criptográficamente seguro, codificados en Base64URL sin relleno (43 caracteres). En la base de datos solo se
 * guarda su hash SHA-256 en hexadecimal.
 */
final class TokenRetiroBarriles {

    static final int BYTES_TOKEN = 32;

    /** Un token de 32 bytes tiene 43 caracteres: uno mucho más largo no corresponde a ningún enlace. */
    static final int LONGITUD_MAXIMA_TOKEN = 128;

    private static final SecureRandom GENERADOR = new SecureRandom();
    private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();

    private TokenRetiroBarriles() {
    }

    /** Un token nuevo. */
    static String generar() {
        byte[] bytes = new byte[BYTES_TOKEN];
        GENERADOR.nextBytes(bytes);
        return CODIFICADOR.encodeToString(bytes);
    }

    /** El hash SHA-256 del token, en hexadecimal (64 caracteres). */
    static String calcularHash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Toda JVM está obligada a ofrecer SHA-256: no llega a ocurrir
            throw new IllegalStateException("La JVM no ofrece el algoritmo SHA-256", e);
        }
    }
}
