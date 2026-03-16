package com.auth.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
@Slf4j
public class JwtProperties {

    private String privateKeyPath = "classpath:keys/private.pem";
    private String publicKeyPath = "classpath:keys/public.pem";
    private long accessTokenExpiryMs = 900_000L;     // 15 min
    private long refreshTokenExpiryMs = 604_800_000L; // 7 days
    private String issuer = "auth-service";

    private final ResourceLoader resourceLoader;

    public JwtProperties(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public PrivateKey loadPrivateKey() {
        try {
            Resource resource = resourceLoader.getResource(privateKeyPath);
            String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                     .replace("-----END PRIVATE KEY-----", "")
                     .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT private key from: " + privateKeyPath, e);
        }
    }

    public PublicKey loadPublicKey() {
        try {
            Resource resource = resourceLoader.getResource(publicKeyPath);
            String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            pem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                     .replace("-----END PUBLIC KEY-----", "")
                     .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT public key from: " + publicKeyPath, e);
        }
    }
}
