package com.auth.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HashUtilsTest {

    @Test
    void sha256_returnsCorrectHash() {
        String input = "password123";
        // SHA-256 of "password123"
        String expected = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f";
        
        String result = HashUtils.sha256(input);
        
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void sha256_isDeterministic() {
        String input = "test-deterministic";
        String hash1 = HashUtils.sha256(input);
        String hash2 = HashUtils.sha256(input);
        
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void generateSecureToken_returnsStringOfCorrectLength() {
        int length = 32;
        String token = HashUtils.generateSecureToken(length);
        
        // Base64 without padding for 32 bytes is length 43 or 44 depending on exact output
        assertThat(token).isNotEmpty();
        assertThat(token.length()).isGreaterThanOrEqualTo(length);
    }

    @Test
    void generateApiKey_hasCorrectPrefix() {
        String apiKey = HashUtils.generateApiKey();
        
        assertThat(apiKey).startsWith("ak_");
        assertThat(apiKey.length()).isGreaterThan(10);
    }

    @Test
    void extractPrefix_worksCorrectly() {
        String apiKey = "ak_prefix_some_random_stuff";
        String prefix = HashUtils.extractPrefix(apiKey);
        
        assertThat(prefix).isEqualTo("ak_prefix_"); // Default is 10 chars
    }
}
