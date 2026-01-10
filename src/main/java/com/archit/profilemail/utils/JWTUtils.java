package com.archit.profilemail.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Component
public class JWTUtils {
    // Use a consistent secret key - in production, this should come from environment/config
    @Value("${jwt.secret:MySecretKeyJWT@13090501DefaultSecretKeyThatIsLongEnough}")
    private String secretKey;
    
    // Lazily initialize the key to ensure @Value is injected first
    private SecretKey key;
    
    private SecretKey getSigningKey() {
        if (key == null) {
            // Create a consistent key from the secret string using SHA-256
            // This ensures the same key is used across restarts
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
                key = new SecretKeySpec(hash, "HmacSHA256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to create signing key", e);
            }
        }
        return key;
    }

    public String generateToken(UserDetails userDetails){
        long jwtExpirationMs = 60 * 60 * 1000;
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .setIssuedAt(new Date())
                .compact();
    }
    
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    
    public boolean isTokenValid(String token, UserDetails userDetails){
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername());
    }
}
