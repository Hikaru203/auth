package com.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class HashUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private HashUtils() {}

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String generateSecureToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String generateApiKey() {
        // Format: ak_<prefix8chars>_<random48chars>
        String random = generateSecureToken(36);
        return "ak_" + random;
    }

    public static String extractPrefix(String key, int length) {
        if (key == null || key.length() < length) return key;
        return key.substring(0, length);
    }

    public static String extractPrefix(String key) {
        return extractPrefix(key, 10); // Default for ak_prefix_ format
    }
}
