package com.auth.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorService {

    private static final String ISSUER = "AuthService";
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    public String generateSecret() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    public String generateQrCodeUrl(String secret, String username, String tenantName) {
        String issuer = ISSUER + " - " + tenantName;
        String label = issuer + ":" + username;
        String otpauthUri = String.format("otpauth://totp/%s?secret=%s&issuer=%s",
                URLEncoder.encode(label, StandardCharsets.UTF_8).replace("+", "%20"),
                secret,
                URLEncoder.encode(issuer, StandardCharsets.UTF_8).replace("+", "%20"));
        
        return "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + 
                URLEncoder.encode(otpauthUri, StandardCharsets.UTF_8);
    }

    public boolean verifyCode(String secret, String code) {
        try {
            int codeInt = Integer.parseInt(code.trim());
            return googleAuthenticator.authorize(secret, codeInt);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
