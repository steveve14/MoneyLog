package com.moneylog.util;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class CryptoUtils {

    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final String SEPARATOR = ":";

    private CryptoUtils() {}

    /**
     * PIN 문자열 → PBKDF2WithHmacSHA256 해시 (랜덤 salt 포함).
     * 반환 형식: "base64(salt):base64(hash)"
     */
    public static String hashPin(String pin) {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);
            byte[] hash = deriveKey(pin, salt);
            return Base64.encodeToString(salt, Base64.NO_WRAP)
                    + SEPARATOR
                    + Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException("PIN hashing failed", e);
        }
    }

    /**
     * 입력 PIN과 저장된 해시(salt:hash)를 timing-safe 하게 비교합니다.
     */
    public static boolean verifyPin(String pin, String storedHash) {
        if (pin == null || storedHash == null) return false;
        try {
            String[] parts = storedHash.split(SEPARATOR, 2);
            if (parts.length != 2) return false;

            byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] expectedHash = Base64.decode(parts[1], Base64.NO_WRAP);
            byte[] actualHash = deriveKey(pin, salt);

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] deriveKey(String pin, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }
}
